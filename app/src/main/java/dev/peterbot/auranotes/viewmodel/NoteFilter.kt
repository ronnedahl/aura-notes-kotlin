package dev.peterbot.auranotes.viewmodel

import dev.peterbot.auranotes.data.local.Category

/**
 * Which notes the list is showing. Exactly one filter is active at a time, so
 * [All] always brings every note back — picked from a single chip row in the UI.
 * Search is applied on top of whichever filter is selected.
 */
sealed interface NoteFilter {
    /** Every note. */
    data object All : NoteFilter

    /** Only notes the user has starred. */
    data object Favorites : NoteFilter

    /** Only notes in [category]. */
    data class ByCategory(val category: Category) : NoteFilter
}
