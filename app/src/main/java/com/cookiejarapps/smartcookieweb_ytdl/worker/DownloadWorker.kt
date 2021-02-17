package com.cookiejarapps.smartcookieweb_ytdl.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cookiejarapps.smartcookieweb_ytdl.R
import com.cookiejarapps.smartcookieweb_ytdl.database.DownloadDatabase
import com.cookiejarapps.smartcookieweb_ytdl.database.Download
import com.cookiejarapps.smartcookieweb_ytdl.database.DownloadsRepository
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.apache.commons.io.IOUtils
import java.io.File
import java.util.*


class DownloadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager?


    override suspend fun doWork(): Result {

        val url = inputData.getString(urlKey)!!
        val name = inputData.getString(nameKey)!!
        val formatId = inputData.getString(formatIdKey)!!
        val audioCodec = inputData.getString(audioCodecKey)
        val videoCodec = inputData.getString(videoCodecKey)
        val downloadDir = inputData.getString(downloadDirKey)!!

        createNotificationChannel()
        val notificationId = id.hashCode()
        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(name)
            .setContentText(applicationContext.getString(R.string.download_start))
            .build()

        val foregroundInfo = ForegroundInfo(notificationId, notification)
        setForeground(foregroundInfo)

        val request = YoutubeDLRequest(url)
        val tmpFile = File.createTempFile("ytdl", null, applicationContext.externalCacheDir)
        tmpFile.delete()
        tmpFile.mkdir()
        tmpFile.deleteOnExit()
        request.addOption("-o", "${tmpFile.absolutePath}/%(title)s.%(ext)s")
        val videoOnly = videoCodec != "none" && audioCodec == "none"
        if (videoOnly) {
            request.addOption("-f", "${formatId}+bestaudio")
        } else {
            request.addOption("-f", formatId)
        }

        var destUri: Uri? = null
        try {
            YoutubeDL.getInstance()
                .execute(request) { progress, _ ->
                    showProgress(id.hashCode(), name, progress.toInt())
                }
            val treeUri = Uri.parse(downloadDir)
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val destDir = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            tmpFile.listFiles().forEach {
                val mimeType =
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.extension) ?: "*/*"
                destUri = DocumentsContract.createDocument(
                    applicationContext.contentResolver,
                    destDir,
                    mimeType,
                    it.name
                )
                val ins = it.inputStream()
                val ops = applicationContext.contentResolver.openOutputStream(destUri!!)
                IOUtils.copy(ins, ops)
                IOUtils.closeQuietly(ops)
                IOUtils.closeQuietly(ins)
            }
        } finally {
            tmpFile.deleteRecursively()
        }

        val downloadsDao = DownloadDatabase.getDatabase(
            applicationContext
        ).downloadsDao()
        val repository =
            DownloadsRepository(downloadsDao)
        val download =
            Download(name, Date().time)
        download.downloadPath = destUri.toString()
        download.downloadPercent = 100.00
        download.fileType = if (videoCodec == "none" && audioCodec != "none"){ "audio" } else{ "video" }

        repository.insertDownloads(download)

        return Result.success()
    }

    private fun showProgress(id: Int, name: String, progress: Int) {
        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(name)
            .setProgress(100, progress, false)
            .build()
        notificationManager?.notify(id, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationChannel =
                notificationManager?.getNotificationChannel(channelId)
            if (notificationChannel == null) {
                val channelName = channelId
                notificationChannel = NotificationChannel(
                    channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW
                )
                notificationChannel.description =
                    channelName
                notificationManager?.createNotificationChannel(notificationChannel)
            }
        }
    }

    companion object {
        private const val channelId = "scw_download"
        const val urlKey = "url"
        const val nameKey = "name"
        const val formatIdKey = "formatId"
        const val downloadDirKey = "downloadDir"
        const val sizeKey = "size"
        const val audioCodecKey = "acodec"
        const val videoCodecKey = "vcodec"
    }
}
