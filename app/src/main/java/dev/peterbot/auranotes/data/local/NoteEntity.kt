package dev.peterbot.auranotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single note as stored in the local Room database.
 *
 * [category] was added in schema v2 (see MIGRATION_1_2) and [isFavorite] in v3
 * (see MIGRATION_2_3); existing rows default to [Category.NONE] / not favorite.
 * Category is stored via [Converters].
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val createdAt: Long,
    val category: Category = Category.NONE,
    val isFavorite: Boolean = false,
)
