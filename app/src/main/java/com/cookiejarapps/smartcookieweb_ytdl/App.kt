@file:Suppress("unused")

package com.cookiejarapps.smartcookieweb_ytdl

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cookiejarapps.smartcookieweb_ytdl.fragments.SettingsFragment
import com.cookiejarapps.smartcookieweb_ytdl.youtubedl.CustomUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        GlobalScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().init(this@App)
                    FFmpeg.getInstance().init(this@App)
                    if(sharedPrefs.getBoolean(UPDATE_ON_START, true)){
                       updateYoutubeDL()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, R.string.startup_error, Toast.LENGTH_SHORT).show()
                Log.e("App", e.toString())
            }
        }
    }

    suspend fun updateYoutubeDL(){
        val result = CustomUpdater.update(applicationContext)
        if (result != YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE) {
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, R.string.update_found, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    companion object{
        private const val UPDATE_ON_START = "update_on_start"
    }
}