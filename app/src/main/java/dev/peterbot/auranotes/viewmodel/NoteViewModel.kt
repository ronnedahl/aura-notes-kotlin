package dev.peterbot.auranotes.viewmodel

import android.app.Application
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** Which notes to show; [NoteFilter.All] shows everything. */
    private val _selectedFilter = MutableStateFlow<NoteFilter>(NoteFilter.All)
    val selectedFilter: StateFlow<NoteFilter> = _selectedFilter.asStateFlow()

    /** Current search text; blank means no text filter. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Notes after applying the selected filter and the search query. */
    val notes: StateFlow<List<NoteEntity>> =
        combine(repository.notes, _selectedFilter, _searchQuery) { notes, filter, query ->
            val trimmedQuery = query.trim()
            notes.filter { note ->
                note.matches(filter) &&
                    (trimmedQuery.isEmpty() || note.text.contains(trimmedQuery, ignoreCase = true))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private fun NoteEntity.matches(filter: NoteFilter): Boolean = when (filter) {
        NoteFilter.All -> true
        NoteFilter.Favorites -> isFavorite
        is NoteFilter.ByCategory -> category == filter.category
    }

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

    fun setFilter(filter: NoteFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNote(text: String, category: Category) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addNote(trimmed, category) }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch { repository.setFavorite(note, !note.isFavorite) }
    }

    /**
     * Write every note as plain text to [uri] (chosen via the system file picker).
     * The write runs off the main thread; [onResult] is invoked on the main thread
     * with whether it succeeded.
     */
    fun exportAllNotes(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val text = formatNotesForExport(repository.allNotes())
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri)
                        ?.use { output -> output.write(text.toByteArray()) }
                        ?: error("Could not open output stream for $uri")
                }.isSuccess
            }
            onResult(success)
        }
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
