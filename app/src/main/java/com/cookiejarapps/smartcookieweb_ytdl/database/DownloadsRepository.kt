package com.cookiejarapps.smartcookieweb_ytdl.database

import androidx.lifecycle.LiveData

class DownloadsRepository(private val downloadsDao: DownloadsDao) {

    val allDownloads: LiveData<List<Download>> = downloadsDao.getAll()

    suspend fun getDownloadById(uid: Int): Download{
        return downloadsDao.getDownloadById(uid)
    }

    suspend fun getDownloadByTimestamp(timestamp: Long): Download{
        return downloadsDao.getDownloadByTimestamp(timestamp)
    }

    suspend fun insertDownloads(download: Download) {
        downloadsDao.insertDownloads(download)
    }

    suspend fun updateDownloads(download: Download) {
        downloadsDao.updateDownloads(download)
    }

    suspend fun deleteDownloads(download: Download) {
        downloadsDao.deleteDownloads(download)
    }
}

