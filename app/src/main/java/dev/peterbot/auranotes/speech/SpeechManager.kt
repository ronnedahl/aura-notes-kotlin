package dev.peterbot.auranotes.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    /** Live partial transcription while the user is speaking. */
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
 * Threading: [SpeechRecognizer] must be created and called on the main thread,
 * and it delivers every [RecognitionListener] callback on the main thread too.
 * All callers (the ViewModel methods, invoked from Compose) already run there, so
 * the [MutableStateFlow] is only ever touched from the main thread.
 */
class SpeechManager(private val context: Context) : RecognitionListener {

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    /** Whether a recognition service is installed and reachable on this device. */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Start a new listening session. Caller MUST have RECORD_AUDIO granted and
     * should check [isAvailable] first. Safe to call again; it tears down any
     * previous session.
     */
    fun startListening() {
        if (!isAvailable()) {
            _state.value = SpeechState.Error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@SpeechManager)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            // Transcribe in the device language (Swedish on Peter's A33).
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Some OEMs (incl. Samsung) require the calling package to be set.
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        _state.value = SpeechState.Ready
        recognizer?.startListening(intent)
    }

    /** Stop listening and ask for the final result of whatever was heard. */
    fun stopListening() {
        recognizer?.stopListening()
    }

    /** Abort the session without producing a result and hide the overlay. */
    fun cancel() {
        recognizer?.cancel()
        _state.value = SpeechState.Idle
    }

    /** Return to [SpeechState.Idle] (used after a result has been consumed). */
    fun reset() {
        _state.value = SpeechState.Idle
    }

    /** Release the recognizer. Call from ViewModel.onCleared(). */
    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        _state.value = SpeechState.Idle
    }

    // --- RecognitionListener -------------------------------------------------

    override fun onReadyForSpeech(params: Bundle?) {
        _state.value = SpeechState.Ready
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults.firstResult()
        if (text.isNotEmpty()) _state.value = SpeechState.Listening(text)
    }

    override fun onResults(results: Bundle?) {
        _state.value = SpeechState.Result(results.firstResult())
    }

    override fun onError(error: Int) {
        // "No match" / "speech timeout" just mean the user said nothing usable —
        // close the overlay quietly instead of showing a scary error.
        _state.value = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechState.Idle
            else -> SpeechState.Error(error)
        }
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
}
