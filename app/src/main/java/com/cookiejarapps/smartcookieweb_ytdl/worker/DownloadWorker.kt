package com.cookiejarapps.smartcookieweb_ytdl.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cookiejarapps.smartcookieweb_ytdl.R
import com.cookiejarapps.smartcookieweb_ytdl.database.DownloadDatabase
import com.cookiejarapps.smartcookieweb_ytdl.database.Download
import com.cookiejarapps.smartcookieweb_ytdl.database.DownloadsRepository
import com.cookiejarapps.smartcookieweb_ytdl.dl.Ytdl
import com.cookiejarapps.smartcookieweb_ytdl.dl.YtdlRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

        val timestamp = Date().time
        val downloadsDao = DownloadDatabase.getDatabase(
            applicationContext
        ).downloadsDao()
        val repository =
            DownloadsRepository(downloadsDao)
        val download =
            Download(name)

        download.timestamp = timestamp
        download.downloadPercent = 0.00
        download.fileType = if (videoCodec == "none" && audioCodec != "none"){ "audio" } else{ "video" }
        download.videoId = inputData.getString(videoId)!!

        repository.insertDownloads(download)

        val uid = repository.getDownloadByTimestamp(timestamp).uid

        val foregroundInfo = ForegroundInfo(notificationId, notification)
        setForeground(foregroundInfo)

        val request = YtdlRequest(url)
        val tmpFile = File.createTempFile("ytdl", uid.toString(), applicationContext.externalCacheDir)
        tmpFile.delete()
        tmpFile.mkdir()
        tmpFile.deleteOnExit()
        request.addOption("-o", "${tmpFile.absolutePath}/[${uid}-%(id)s] %(title)s.%(ext)s")
        val videoOnly = videoCodec != "none" && audioCodec == "none"
        if (videoOnly) {
            request.addOption("-f", "${formatId}+bestaudio")
        } else {
            request.addOption("-f", formatId)
        }

        var destUri: Uri? = null
        try {
            Ytdl.instance
                .execute(request) { progress, etaInSeconds ->
                    if(isStopped){
                        tmpFile.deleteRecursively()
                    }
                    else{
                        showProgress(id.hashCode(), name, progress.toInt(), etaInSeconds, uid)
                    }
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


       GlobalScope.launch {
            val finalDownload = repository.getDownloadById(uid)
           finalDownload.downloadPath = destUri.toString()
           repository.updateDownloads(finalDownload)
       }

        return Result.success()
    }

    private fun showProgress(id: Int, name: String, progress: Int = 0, eta: Long = 0, uid: Int) {
        val downloadsDao = DownloadDatabase.getDatabase(
            applicationContext
        ).downloadsDao()
        val repository =
            DownloadsRepository(downloadsDao)

        GlobalScope.launch {
            val download = repository.getDownloadById(uid)
            download.downloadPercent = progress.toDouble()
            repository.updateDownloads(download)
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(name)
            .setProgress(100, progress, false)
            .setSubText(applicationContext.resources.getString(R.string.eta, eta))
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
        const val videoId = "vid"
    }
}

