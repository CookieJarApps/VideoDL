package com.cookiejarapps.videodl.database

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

    @Query("SELECT * from downloads_table WHERE uid = :uid ORDER BY timestamp DESC")
    suspend fun getDownloadById(uid: Int): Download

    @Query("SELECT * from downloads_table WHERE timestamp = :timestamp ORDER BY timestamp DESC")
    suspend fun getDownloadByTimestamp(timestamp: Long): Download

    @Query("SELECT * from downloads_table ORDER BY timestamp DESC")
    fun getAll(): LiveData<List<Download>>
}