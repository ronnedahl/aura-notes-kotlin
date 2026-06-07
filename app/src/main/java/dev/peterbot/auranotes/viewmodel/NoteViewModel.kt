package dev.peterbot.auranotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.peterbot.auranotes.data.local.NoteDatabase
import dev.peterbot.auranotes.data.local.NoteEntity
import dev.peterbot.auranotes.data.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Exposes the notes list as [StateFlow] and owns all write operations.
 *
 * AndroidViewModel because it needs an [Application] context to open the Room
 * database. The UI only ever calls these methods and collects [notes].
 */
class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository =
        NoteRepository(NoteDatabase.getInstance(application).noteDao())

    val notes: StateFlow<List<NoteEntity>> = repository.notes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun addNote(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addNote(trimmed) }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { repository.deleteNote(note) }
    }
}
