package com.cookiejarapps.videodl.listener

import android.view.View

interface DownloadListListener {
    fun onClick(view: View?, position: Int)
    fun onLongClick(view: View?, position: Int)
}