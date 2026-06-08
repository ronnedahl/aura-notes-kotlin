package dev.peterbot.auranotes.viewmodel

import dev.peterbot.auranotes.data.local.Category
import dev.peterbot.auranotes.data.local.NoteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the plain-text body of a .txt export. Pure (no Android dependencies) so
 * it is easy to test; the ViewModel handles reading the notes and writing the file.
 *
 * Each note becomes a timestamp line (with category and a star for favorites)
 * followed by its text, notes separated by a blank line.
 */
fun formatNotesForExport(notes: List<NoteEntity>): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    return notes.joinToString(separator = "\n\n") { note ->
        buildString {
            append(dateFormat.format(Date(note.createdAt)))
            if (note.category != Category.NONE) {
                append("  [").append(note.category.displayName()).append("]")
            }
            if (note.isFavorite) append("  ★")
            append("\n")
            append(note.text)
        }
    }
}

private fun Category.displayName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
