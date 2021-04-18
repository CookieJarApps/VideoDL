package com.cookiejarapps.smartcookieweb_ytdl.youtubedl

import android.content.Context
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.net.URL


internal class CustomUpdater {

    companion object {
        private const val releasesUrl =
            "https://api.github.com/repos/cookiejarapps/youtubedl-lazy/releases/latest"
        private const val youtubeDLVersionKey = "youtubeDLVersion"
        protected val objectMapper = ObjectMapper()
        protected const val youtubeDLFile = "youtube_dl.zip"
        protected const val youtubeDLDirName = "youtube-dl";
        protected const val baseName = "youtubedl-android"

        @Throws(IOException::class, YoutubeDLException::class)
        fun update(appContext: Context): YoutubeDL.UpdateStatus {
            val json: JsonNode = checkForUpdate(appContext)
                ?: return YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE
            val downloadUrl = getDownloadUrl(json)
            val file = download(appContext, downloadUrl)
            var youtubeDLDir: File? = null
            try {
                youtubeDLDir = getYoutubeDLDir(appContext)
                //purge older version
                FileUtils.deleteDirectory(youtubeDLDir)
                //install newer version
                youtubeDLDir.mkdirs()
                ZipUtils.unzip(file, youtubeDLDir)
            } catch (e: Exception) {
                //if something went wrong restore default version
                FileUtils.deleteQuietly(youtubeDLDir)
                YoutubeDL.getInstance().init(appContext)
                throw YoutubeDLException(e)
            } finally {
                file.delete()
            }
            updateSharedPrefs(appContext, getTag(json))
            return YoutubeDL.UpdateStatus.DONE
        }

        private fun updateSharedPrefs(appContext: Context, tag: String) {
            val pref = appContext.getSharedPreferences("youtubedl-android", Context.MODE_PRIVATE)
            val editor = pref.edit()
            editor.putString(youtubeDLVersionKey, tag)
            editor.apply()
        }

        @Throws(IOException::class)
        private fun checkForUpdate(appContext: Context): JsonNode? {
            val url = URL(releasesUrl)
            val json: JsonNode = objectMapper.readTree(url)
            val newVersion = getTag(json)
            val pref = appContext.getSharedPreferences("youtubedl-android", Context.MODE_PRIVATE)
            val oldVersion: String? = pref.getString(youtubeDLVersionKey, null)
            return if (newVersion == oldVersion) {
                null
            } else json
        }

        private fun getTag(json: JsonNode): String {
            return json.get("tag_name").asText()
        }

        @Throws(IOException::class, YoutubeDLException::class)
        private fun getDownloadUrl(json: JsonNode): String {
            val assets = json.get("assets") as ArrayNode
            var downloadUrl = ""
            for (asset in assets) {
                if (youtubeDLFile == asset["name"].asText()) {
                    downloadUrl = asset["browser_download_url"].asText()
                    break
                }
            }
            if (downloadUrl.isEmpty()) throw YoutubeDLException("unable to get download url")
            return downloadUrl
        }

        @Throws(IOException::class)
        private fun download(appContext: Context, url: String): File {
            val downloadUrl = URL(url)
            val file = File.createTempFile("youtube_dl", "zip", appContext.cacheDir)
            FileUtils.copyURLToFile(downloadUrl, file, 5000, 10000)
            return file
        }

        private fun getYoutubeDLDir(appContext: Context): File {
            val baseDir = File(appContext.noBackupFilesDir, baseName)
            return File(baseDir, youtubeDLDirName)
        }

        fun version(appContext: Context?): String? {
            val pref = appContext!!.getSharedPreferences("youtubedl-android", Context.MODE_PRIVATE)
            return pref.getString(youtubeDLVersionKey, null)
        }
    }
}