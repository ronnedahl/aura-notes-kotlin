package dev.peterbot.auranotes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's Room database. Single instance for the whole process (see [getInstance]).
 *
 * NOTE: while the schema is still evolving across the early feature PRs we use
 * destructive migration, so installing a build with a changed schema wipes
 * existing notes. Proper migrations will be added before any release build.
 */
@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        private const val DATABASE_NAME = "aura_notes.db"

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
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
