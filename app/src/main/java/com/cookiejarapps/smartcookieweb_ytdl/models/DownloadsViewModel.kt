package com.cookiejarapps.smartcookieweb_ytdl.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cookiejarapps.smartcookieweb_ytdl.database.DownloadDatabase
import com.cookiejarapps.smartcookieweb_ytdl.database.Download
import com.cookiejarapps.smartcookieweb_ytdl.database.DownloadsRepository

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadsRepository
    val downloadList: LiveData<List<Download>>

    init {
        val downloadsDao = DownloadDatabase.getDatabase(application).downloadsDao()
        repository = DownloadsRepository(downloadsDao)
        downloadList = repository.allDownloads
    }
}
