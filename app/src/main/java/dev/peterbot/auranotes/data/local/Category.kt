package dev.peterbot.auranotes.data.local

/**
 * The category a note can belong to. [NONE] means uncategorized.
 *
 * Pure data — the display label (localized) and the brand colour live in the UI
 * layer so this enum stays free of Android/Compose dependencies. Stored in Room
 * as its [name] via [Converters].
 */
enum class Category {
    PERSONAL,
    WORK,
    IDEAS,
    SHOPPING,
    NONE,
}
