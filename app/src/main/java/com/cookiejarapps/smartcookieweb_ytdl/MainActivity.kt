package com.cookiejarapps.smartcookieweb_ytdl

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


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

    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}