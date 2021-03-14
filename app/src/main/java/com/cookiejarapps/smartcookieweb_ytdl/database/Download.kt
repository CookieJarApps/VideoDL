package com.cookiejarapps.smartcookieweb_ytdl.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads_table")
data class Download(
    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "time")
    val timestamp: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "download_progress")
    var downloadPercent: Double = 0.0

    @ColumnInfo(name = "download_path")
    lateinit var downloadPath: String

    @ColumnInfo(name = "file_type")
    lateinit var fileType: String

    @ColumnInfo(name = "video_id")
    lateinit var videoId: String
}