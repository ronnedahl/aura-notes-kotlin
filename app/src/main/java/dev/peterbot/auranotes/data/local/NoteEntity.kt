package dev.peterbot.auranotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single note as stored in the local Room database.
 *
 * Task 1 keeps the schema intentionally small (text + timestamp). Later tasks
 * (categories, favorites) will add columns via a Room migration.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val createdAt: Long,
)
