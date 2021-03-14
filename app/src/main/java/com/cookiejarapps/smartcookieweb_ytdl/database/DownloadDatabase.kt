package com.cookiejarapps.smartcookieweb_ytdl.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(entities = [Download::class], version = 2, exportSchema = true)
abstract class DownloadDatabase : RoomDatabase() {

    abstract fun downloadsDao(): DownloadsDao

    companion object {
        @Volatile
        private var dbInstance: DownloadDatabase? = null

        private const val name = "ytdl_db"

        fun getDatabase(context: Context): DownloadDatabase {
            val tempInstance = dbInstance
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DownloadDatabase::class.java,
                    name
                ).addMigrations(MIGRATION_1_2)
                    .build()
                dbInstance = instance
                return instance
            }
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE downloads_table ADD COLUMN video_id TEXT")
            }
        }
    }
}
