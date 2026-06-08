package dev.peterbot.auranotes.data.repository

import dev.peterbot.auranotes.data.local.Category
import dev.peterbot.auranotes.data.local.NoteDao
import dev.peterbot.auranotes.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for notes. Wraps [NoteDao] so the ViewModel never
 * touches Room directly. Keeps the data-access layer swappable and testable.
 */
class NoteRepository(private val noteDao: NoteDao) {

    val notes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    /** Snapshot of all notes for export (newest first). */
    suspend fun allNotes(): List<NoteEntity> = noteDao.getAllNotesList()

    suspend fun addNote(text: String, category: Category): Long =
        noteDao.insert(
            NoteEntity(
                text = text,
                createdAt = System.currentTimeMillis(),
                category = category,
            ),
        )

    suspend fun updateNote(note: NoteEntity) = noteDao.update(note)

    suspend fun setFavorite(note: NoteEntity, isFavorite: Boolean) =
        noteDao.update(note.copy(isFavorite = isFavorite))

    suspend fun deleteNote(note: NoteEntity) = noteDao.delete(note)
}
