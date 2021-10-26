package com.cookiejarapps.videodl.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cookiejarapps.videodl.database.DownloadDatabase
import com.cookiejarapps.videodl.database.Download
import com.cookiejarapps.videodl.database.DownloadsRepository

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadsRepository
    val downloadList: LiveData<List<Download>>

    init {
        val downloadsDao = DownloadDatabase.getDatabase(application).downloadsDao()
        repository = DownloadsRepository(downloadsDao)
        downloadList = repository.allDownloads
    }
}
