package com.cookiejarapps.videodl.dl

import android.util.Log
import com.cookiejarapps.videodl.BuildConfig
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.lang.StringBuilder
import java.util.regex.Pattern

internal class StreamProcessExtractor(
    private val buffer: StringBuffer,
    private val stream: InputStream,
    private val callback: DownloadProgressCallback?
) : Thread() {
    private val p = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d)% .* ETA (\\d+):(\\d+)")
    override fun run() {
        try {
            val `in`: Reader = InputStreamReader(stream, "UTF-8")
            val currentLine = StringBuilder()
            var nextChar: Int
            while (`in`.read().also { nextChar = it } != -1) {
                buffer.append(nextChar.toChar())
                if (nextChar == '\r'.toInt() && callback != null) {
                    processOutputLine(currentLine.toString())
                    currentLine.setLength(0)
                    continue
                }
                currentLine.append(nextChar.toChar())
            }
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) Log.e(TAG, "failed to read stream", e)
        }
    }

    private fun processOutputLine(line: String) {
        val m = p.matcher(line)
        if (m.matches()) {
            val progress = m.group(GROUP_PERCENT).toFloat()
            val eta = convertToSeconds(m.group(GROUP_MINUTES), m.group(GROUP_SECONDS)).toLong()
            callback!!.onProgressUpdate(progress, eta)
        }
    }

    private fun convertToSeconds(minutes: String, seconds: String): Int {
        return minutes.toInt() * 60 + seconds.toInt()
    }

    companion object {
        private const val GROUP_PERCENT = 1
        private const val GROUP_MINUTES = 2
        private const val GROUP_SECONDS = 3
        private const val TAG = "StreamProcessExtractor"
    }

    init {
        start()
    }
}