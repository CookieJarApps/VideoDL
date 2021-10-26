package com.cookiejarapps.smartcookieweb_ytdl.dl

import android.util.Log
import com.cookiejarapps.smartcookieweb_ytdl.BuildConfig
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

internal class StreamGobbler(private val buffer: StringBuffer, private val stream: InputStream) :
    Thread() {
    override fun run() {
        try {
            val `in`: Reader = InputStreamReader(stream, "UTF-8")
            var nextChar: Int
            while (`in`.read().also { nextChar = it } != -1) {
                buffer.append(nextChar.toChar())
            }
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) Log.e(TAG, "failed to read stream", e)
        }
    }

    companion object {
        private const val TAG = "StreamGobbler"
    }

    init {
        start()
    }
}