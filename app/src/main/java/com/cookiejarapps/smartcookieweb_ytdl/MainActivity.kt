package com.cookiejarapps.smartcookieweb_ytdl

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pm: PackageManager = getPackageManager()
        if(isPackageInstalled("com.cookiejarapps.smartcookiebeta", pm) || isPackageInstalled("com.cookiegames.smartcookie", pm)){
            findViewById<TextView>(R.id.homepage).text = resources.getString(R.string.app_successful)
        }
        else{
            findViewById<TextView>(R.id.homepage).text = resources.getString(R.string.app_required)
        }
        initialize()
    }

    private fun initialize(){
        try {
            YoutubeDL.getInstance().init(application)
            FFmpeg.getInstance().init(application)
        } catch (e: YoutubeDLException) {
            Log.e(TAG, "failed to initialize youtubedl-android", e)
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}