package com.cookiejarapps.smartcookieweb_ytdl.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DownloadsDao {
    @Insert
    suspend fun insertDownloads(item: Download)

    @Update
    suspend fun updateDownloads(item: Download)

    @Delete
    suspend fun deleteDownloads(item: Download)

    @Query("SELECT * from downloads_table WHERE time = :timestamp ORDER BY time DESC")
    suspend fun getDownloadByTimeStamp(timestamp: Long): Download

    @Query("SELECT * from downloads_table ORDER BY time DESC")
    fun getAll(): LiveData<List<Download>>

}