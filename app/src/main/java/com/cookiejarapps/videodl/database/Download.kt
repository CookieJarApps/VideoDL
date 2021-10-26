package com.cookiejarapps.videodl.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads_table")
data class Download(
    @ColumnInfo(name = "name")
    val name: String,

    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0
) {
    @ColumnInfo(name = "timestamp")
    var timestamp: Long = 0

    @ColumnInfo(name = "download_progress")
    var downloadPercent: Double = 0.0

    @ColumnInfo(name = "download_path")
    var downloadPath: String? = null

    @ColumnInfo(name = "file_type")
    lateinit var fileType: String

    @ColumnInfo(name = "video_id")
    lateinit var videoId: String
}