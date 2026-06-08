package dev.peterbot.auranotes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The app's Room database. Single instance for the whole process (see [getInstance]).
 *
 * Schema history:
 *  - v1: id, text, createdAt
 *  - v2: + category (see [MIGRATION_1_2])
 *  - v3: + isFavorite (see [MIGRATION_2_3])
 *
 * Real migrations preserve the user's notes across upgrades. The destructive
 * fallback is kept only as a last-resort backstop if no migration path exists.
 */
@Database(
    entities = [NoteEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        private const val DATABASE_NAME = "aura_notes.db"

        /** Adds the category column; existing notes become uncategorized (NONE). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN category TEXT NOT NULL DEFAULT 'NONE'",
                )
            }
        }

        /** Adds the favorite flag; existing notes start as not favorite (0). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): NoteDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NoteDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
