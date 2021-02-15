package com.cookiejarapps.smartcookieweb_ytdl

import android.app.Application
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().init(this@App)
                    FFmpeg.getInstance().init(this@App)
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, R.string.init_fail, Toast.LENGTH_SHORT).show()
                Log.e("App", e.toString())
            }
        }
    }
}