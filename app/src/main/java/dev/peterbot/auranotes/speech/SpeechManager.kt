package dev.peterbot.auranotes.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * One observable step of a voice-note recording session.
 *
 * The UI renders an overlay for every state except [Idle]; the ViewModel reacts
 * to [Result] by saving the note. Errors carry the raw [SpeechRecognizer] code so
 * the UI decides how (or whether) to surface them.
 */
sealed interface SpeechState {
    /** Nothing is happening; no recording overlay shown. */
    data object Idle : SpeechState

    /** Recognizer started, waiting for the user to start speaking. */
    data object Ready : SpeechState

    /** Live transcription so far (finalized segments + current partial). */
    data class Listening(val partialText: String) : SpeechState

    /** Final transcription. Empty text means nothing was understood. */
    data class Result(val text: String) : SpeechState

    /** Recognition failed; [code] is a [SpeechRecognizer] ERROR_* constant. */
    data class Error(val code: Int) : SpeechState
}

/**
 * Thin wrapper around Android's on-device [SpeechRecognizer].
 *
 * MVVM note: this is a plain class owned by the ViewModel, NOT a ViewModel or a
 * Composable. It exposes [state] as a [StateFlow] and knows nothing about the UI.
 *
 * **Continuous listening.** A single [SpeechRecognizer] recognition ends after a
 * short silence, which is too eager for dictating a note — pausing to think would
 * cut you off. So a session keeps the recognizer running: when a silence ends one
 * segment, we append its text and immediately listen again. The session only ends
 * when the user calls [stopListening] or [cancel]. Result text is the segments
 * joined together.
 *
 * Threading: [SpeechRecognizer] must be created and called on the main thread,
 * and it delivers every [RecognitionListener] callback on the main thread too.
 * All callers (the ViewModel methods, invoked from Compose) already run there, so
 * the [MutableStateFlow] and the session fields are only touched from one thread.
 */
class SpeechManager(private val context: Context) : RecognitionListener {

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null

    /** Finalized segments of the current session, joined with spaces. */
    private val transcript = StringBuilder()

    /**
     * Latest partial text of the in-flight recognition. Kept so that pressing
     * Stop never loses words that were only ever delivered as partial results
     * (some devices don't emit a final [onResults] when stopped mid-utterance).
     */
    private var lastPartial = ""

    /** A recording session is open (between startListening and finish/cancel). */
    private var sessionActive = false

    /** True once the user has asked to stop; the next result finalizes the note. */
    private var stopping = false

    /** A recognition is in flight (listening), vs. waiting in a restart gap. */
    private var listening = false

    /** Pending auto-restart, so it can be cancelled if the user stops mid-gap. */
    private var pendingRestart: Runnable? = null

    /** Guards against a tight loop if the recognizer keeps reporting "busy". */
    private var busyRetries = 0

    /** Whether a recognition service is installed and reachable on this device. */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Start a recording session. Caller MUST have RECORD_AUDIO granted and should
     * check [isAvailable] first. Listening continues across pauses until
     * [stopListening] or [cancel].
     */
    fun startListening() {
        if (!isAvailable()) {
            _state.value = SpeechState.Error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
            return
        }
        transcript.setLength(0)
        stopping = false
        sessionActive = true
        busyRetries = 0
        _state.value = SpeechState.Ready
        beginRecognition()
    }

    /** Stop the session and finalize the note from everything heard so far. */
    fun stopListening() {
        if (!sessionActive) return
        stopping = true
        cancelPendingRestart()
        // If a recognition is in flight, ask for its final result and let the
        // callback finalize. If we're in a restart gap, no callback is coming —
        // finalize right now.
        if (listening) recognizer?.stopListening() else finish()
    }

    /** Abort the session without producing a result and hide the overlay. */
    fun cancel() {
        clearSession()
        recognizer?.cancel()
        _state.value = SpeechState.Idle
    }

    /** Return to [SpeechState.Idle] (used after a result has been consumed). */
    fun reset() {
        _state.value = SpeechState.Idle
    }

    /** Release the recognizer. Call from ViewModel.onCleared(). */
    fun destroy() {
        clearSession()
        recognizer?.destroy()
        recognizer = null
        _state.value = SpeechState.Idle
    }

    // --- Internals -----------------------------------------------------------

    /**
     * (Re)start a single recognition on the shared recognizer instance.
     *
     * The recognizer is created lazily and reused for the whole app lifetime.
     * Creating a fresh one per recognition leaves the previous service binding
     * tearing down, so the next start fails with ERROR_CLIENT / RECOGNIZER_BUSY.
     */
    private fun beginRecognition() {
        val recognizer = this.recognizer
            ?: SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(this)
                this.recognizer = it
            }
        listening = true
        recognizer.startListening(recognizerIntent())
    }

    private fun recognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            // Transcribe in the device language (Swedish on Peter's A33).
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Some OEMs (incl. Samsung) require the calling package to be set.
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Hint the recognizer to tolerate longer pauses before ending a
            // segment. Not all services honour these — the continuous restart
            // above is what actually lets the user pause and keep talking.
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                SILENCE_MILLIS,
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                SILENCE_MILLIS,
            )
        }

    /** Schedule another recognition shortly after the previous one ended. */
    private fun scheduleRestart() {
        if (!sessionActive || stopping) return
        cancelPendingRestart()
        val runnable = Runnable {
            pendingRestart = null
            if (sessionActive && !stopping) beginRecognition()
        }
        pendingRestart = runnable
        mainHandler.postDelayed(runnable, RESTART_DELAY_MILLIS)
    }

    private fun cancelPendingRestart() {
        pendingRestart?.let { mainHandler.removeCallbacks(it) }
        pendingRestart = null
    }

    /** Append a recognized segment and refresh the live text. */
    private fun appendSegment(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (transcript.isNotEmpty()) transcript.append(' ')
        transcript.append(trimmed)
    }

    private fun finish() {
        // Fold in any in-flight partial that was never finalized (e.g. Stop
        // pressed mid-utterance), so the spoken words aren't lost.
        appendSegment(lastPartial)
        val text = transcript.toString().trim()
        clearSession()
        _state.value = SpeechState.Result(text)
    }

    private fun clearSession() {
        cancelPendingRestart()
        sessionActive = false
        stopping = false
        listening = false
        busyRetries = 0
        transcript.setLength(0)
        lastPartial = ""
    }

    /** Live text = finalized segments + an optional in-progress partial. */
    private fun emitListening(partial: String = "") {
        val combined = buildString {
            append(transcript)
            val p = partial.trim()
            if (p.isNotEmpty()) {
                if (isNotEmpty()) append(' ')
                append(p)
            }
        }
        _state.value = if (combined.isEmpty()) SpeechState.Ready else SpeechState.Listening(combined)
    }

    // --- RecognitionListener -------------------------------------------------

    override fun onReadyForSpeech(params: Bundle?) {
        emitListening()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partial = partialResults.firstResult()
        if (partial.isNotEmpty()) lastPartial = partial
        emitListening(lastPartial)
    }

    override fun onResults(results: Bundle?) {
        listening = false
        busyRetries = 0
        // Prefer the final text; fall back to the last partial if the device
        // returned an empty final result.
        val final = results.firstResult()
        appendSegment(if (final.isNotEmpty()) final else lastPartial)
        lastPartial = ""
        if (stopping) finish() else { emitListening(); scheduleRestart() }
    }

    override fun onError(error: Int) {
        listening = false
        when (error) {
            // Silence: the user may just be pausing to think. Keep the session
            // alive and listen again — unless they've asked to stop.
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                if (stopping) finish() else scheduleRestart()
            }
            // Transient "busy/client" hiccups between segments: retry a few times
            // before giving up, so one stutter doesn't end the note.
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_CLIENT -> when {
                stopping -> finish()
                busyRetries++ < MAX_BUSY_RETRIES -> scheduleRestart()
                transcript.isNotEmpty() -> finish()
                else -> fail(error)
            }
            // Real failures (audio, network, permissions…). Keep whatever we
            // already captured rather than losing it; otherwise surface it.
            else -> if (transcript.isNotEmpty()) finish() else fail(error)
        }
    }

    private fun fail(error: Int) {
        clearSession()
        _state.value = SpeechState.Error(error)
    }

    // Unused callbacks required by the interface.
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun Bundle?.firstResult(): String =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()

    private companion object {
        /** Pause tolerated within a segment before the recognizer may cut it. */
        const val SILENCE_MILLIS = 4000

        /** Gap before re-listening after a segment ends, to avoid "busy". */
        const val RESTART_DELAY_MILLIS = 200L

        /** Max consecutive busy/client retries before giving up. */
        const val MAX_BUSY_RETRIES = 5
    }
}
