package com.cookiejarapps.videodl.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cookiejarapps.videodl.R
import com.cookiejarapps.videodl.dl.Ytdl
import com.cookiejarapps.videodl.dl.YtdlRequest
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


class CommandWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager?


    override suspend fun doWork(): Result {
        val command = inputData.getString(commandKey)!!

        createNotificationChannel()
        val notificationId = id.hashCode()
        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.youtubedl))
            .build()

        val foregroundInfo = ForegroundInfo(notificationId, notification)
        setForeground(foregroundInfo)

        // this is not the recommended way to add options/flags/url and might break in future
        // use the constructor for url, addOption(key) for flags, addOption(key, value) for options
        val request = YtdlRequest(Collections.emptyList())
        val m: Matcher = Pattern.compile(commandRegex).matcher(command)
        while (m.find()) {
            if (m.group(1) != null) {
                request.addOption(m.group(1))
            } else {
                request.addOption(m.group(2))
            }
        }

          Ytdl.instance
            .execute(request) { progress, etaInSeconds ->
                showProgress(id.hashCode(), progress.toInt(), etaInSeconds)
            }

        return Result.success()
    }


    private fun showProgress(id: Int, progress: Int, eta: Long) {
        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.youtubedl))
            .setSubText(applicationContext.resources.getString(R.string.eta, eta))
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
        private const val channelId = "scw_command"
        const val commandKey = "command"
        const val commandWorkTag = "command_work"
        private const val commandRegex = "\"([^\"]*)\"|(\\S+)"
    }
}

