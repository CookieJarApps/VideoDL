@file:Suppress("unused")

package com.cookiejarapps.videodl

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.yausername.ffmpeg.FFmpeg
import com.cookiejarapps.videodl.dl.Ytdl
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class App : Application() {
    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)

        GlobalScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Ytdl.instance.init(this@App)
                    FFmpeg.getInstance().init(this@App)
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, R.string.startup_error, Toast.LENGTH_SHORT).show()
                Log.e("App", e.toString())
            }
        }
    }

    companion object{
        private const val UPDATE_ON_START = "update_on_start"
    }
}