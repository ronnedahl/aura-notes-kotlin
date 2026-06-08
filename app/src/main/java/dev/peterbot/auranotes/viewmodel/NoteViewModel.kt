package dev.peterbot.auranotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.peterbot.auranotes.data.local.NoteDatabase
import dev.peterbot.auranotes.data.local.NoteEntity
import dev.peterbot.auranotes.data.repository.NoteRepository
import dev.peterbot.auranotes.speech.SpeechManager
import dev.peterbot.auranotes.speech.SpeechState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Exposes the notes list as [StateFlow] and owns all write operations, plus the
 * voice-recording session via [SpeechManager].
 *
 * AndroidViewModel because it needs an [Application] context to open the Room
 * database and to create the speech recognizer. The UI only ever calls these
 * methods and collects [notes] / [recordingState].
 */
class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository =
        NoteRepository(NoteDatabase.getInstance(application).noteDao())

    private val speechManager = SpeechManager(application)

    val notes: StateFlow<List<NoteEntity>> = repository.notes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Drives the recording overlay; [SpeechState.Idle] means no overlay. */
    val recordingState: StateFlow<SpeechState> = speechManager.state

    init {
        // A finished transcription is saved automatically, then the session
        // resets so the overlay closes. Blank results are ignored by addNote.
        viewModelScope.launch {
            speechManager.state.collect { state ->
                if (state is SpeechState.Result) {
                    addNote(state.text)
                    speechManager.reset()
                }
            }
        }
    }

    fun addNote(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addNote(trimmed) }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    // --- Voice recording -----------------------------------------------------

    /** True if this device has a usable speech recognition service. */
    fun isSpeechAvailable(): Boolean = speechManager.isAvailable()

    /** Start listening. Caller must have already granted RECORD_AUDIO. */
    fun startRecording() = speechManager.startListening()

    /** Stop listening and transcribe what was heard (auto-saved on result). */
    fun stopRecording() = speechManager.stopListening()

    /** Abort the recording without saving. */
    fun cancelRecording() = speechManager.cancel()

    override fun onCleared() {
        speechManager.destroy()
    }
}
