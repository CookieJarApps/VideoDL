package com.cookiejarapps.smartcookieweb_ytdl.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Download::class], version = 1, exportSchema = true)
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
                ).build()
                dbInstance = instance
                return instance
            }
        }
    }
}
