package dev.peterbot.auranotes.data.local

import androidx.room.TypeConverter

/**
 * Room type converters. Stores [Category] as its enum name; unknown or corrupt
 * values fall back to [Category.NONE] so a bad row can never crash a query.
 */
class Converters {

    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(value: String): Category =
        runCatching { Category.valueOf(value) }.getOrDefault(Category.NONE)
}
