package dev.peterbot.auranotes.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for [NoteEntity].
 *
 * Reads return a [Flow] so the UI updates automatically when the table changes.
 * Writes are suspend functions and must be called from a coroutine.
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    /** One-shot snapshot of every note, for .txt export. */
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAllNotesList(): List<NoteEntity>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}
