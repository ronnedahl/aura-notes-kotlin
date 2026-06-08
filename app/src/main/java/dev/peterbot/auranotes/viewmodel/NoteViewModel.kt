package dev.peterbot.auranotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.peterbot.auranotes.data.local.Category
import dev.peterbot.auranotes.data.local.NoteDatabase
import dev.peterbot.auranotes.data.local.NoteEntity
import dev.peterbot.auranotes.data.repository.NoteRepository
import dev.peterbot.auranotes.speech.SpeechManager
import dev.peterbot.auranotes.speech.SpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Exposes the (optionally filtered) notes list as [StateFlow], owns all write
 * operations, and drives the voice-recording session via [SpeechManager].
 *
 * AndroidViewModel because it needs an [Application] context to open the Room
 * database and create the speech recognizer. The UI only ever calls these
 * methods and collects the exposed flows.
 */
class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository =
        NoteRepository(NoteDatabase.getInstance(application).noteDao())

    private val speechManager = SpeechManager(application)

    /** The category chip the user is filtering by; null means "All". */
    private val _selectedFilter = MutableStateFlow<Category?>(null)
    val selectedFilter: StateFlow<Category?> = _selectedFilter.asStateFlow()

    /** Notes after applying [selectedFilter]. */
    val notes: StateFlow<List<NoteEntity>> =
        combine(repository.notes, _selectedFilter) { notes, filter ->
            if (filter == null) notes else notes.filter { it.category == filter }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Category chosen for the note currently being recorded. */
    private val _recordingCategory = MutableStateFlow(Category.NONE)
    val recordingCategory: StateFlow<Category> = _recordingCategory.asStateFlow()

    /** Drives the recording overlay; [SpeechState.Idle] means no overlay. */
    val recordingState: StateFlow<SpeechState> = speechManager.state

    init {
        // A finished transcription is saved with the chosen category, then the
        // session resets so the overlay closes. Blank results are ignored.
        viewModelScope.launch {
            speechManager.state.collect { state ->
                if (state is SpeechState.Result) {
                    addNote(state.text, _recordingCategory.value)
                    speechManager.reset()
                }
            }
        }
    }

    fun setFilter(category: Category?) {
        _selectedFilter.value = category
    }

    fun addNote(text: String, category: Category) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addNote(trimmed, category) }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    // --- Voice recording -----------------------------------------------------

    /** True if this device has a usable speech recognition service. */
    fun isSpeechAvailable(): Boolean = speechManager.isAvailable()

    /** Start listening. Caller must have already granted RECORD_AUDIO. */
    fun startRecording() {
        _recordingCategory.value = Category.NONE
        speechManager.startListening()
    }

    /** Change the category for the in-progress recording. */
    fun setRecordingCategory(category: Category) {
        _recordingCategory.value = category
    }

    /** Stop listening and transcribe what was heard (auto-saved on result). */
    fun stopRecording() = speechManager.stopListening()

    /** Abort the recording without saving. */
    fun cancelRecording() = speechManager.cancel()

    override fun onCleared() {
        speechManager.destroy()
    }
}
