package com.cookiejarapps.videodl.dl

import android.content.Context
import kotlin.Throws
import com.cookiejarapps.videodl.R
import com.cookiejarapps.videodl.SharedPrefsHelper
import com.cookiejarapps.videodl.ZipUtils
import com.cookiejarapps.videodl.dl.mapper.VideoInfo
import kotlin.jvm.JvmOverloads
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*

open class Ytdl private constructor() {
    private var initialized = false
    private var pythonPath: File? = null
    private var youtubeDLPath: File? = null
    private var binDir: File? = null
    private var ENV_LD_LIBRARY_PATH: String? = null
    private var ENV_SSL_CERT_FILE: String? = null
    private var ENV_PYTHONHOME: String? = null

    @Synchronized
    fun init(appContext: Context) {
        if (initialized) return
        val baseDir = File(appContext.noBackupFilesDir, baseName)
        val pref =
            appContext.getSharedPreferences("youtubedl-android", Context.MODE_PRIVATE)
        if(pref.getBoolean("firstLaunchYtdl", true)){
            baseDir.deleteRecursively()
            val editor = pref.edit()
            editor.putBoolean("firstLaunchYtdl", false)
            editor.apply()
        }
        if (!baseDir.exists()) baseDir.mkdir()
        val packagesDir = File(baseDir, packagesRoot)
        binDir = File(appContext.applicationInfo.nativeLibraryDir)
        pythonPath = File(binDir, pythonBinName)
        val pythonDir = File(packagesDir, pythonDirName)
        val ffmpegDir = File(packagesDir, ffmpegDirName)
        val youtubeDLDir = File(baseDir, youtubeDLDirName)
        youtubeDLPath = File(youtubeDLDir, youtubeDLBin)
        ENV_LD_LIBRARY_PATH =
            pythonDir.absolutePath + "/usr/lib" + ":" + ffmpegDir.absolutePath + "/usr/lib"
        ENV_SSL_CERT_FILE = pythonDir.absolutePath + "/usr/etc/tls/cert.pem"
        ENV_PYTHONHOME = pythonDir.absolutePath + "/usr"
        initPython(appContext, pythonDir)
        initYoutubeDL(appContext, youtubeDLDir)
        initialized = true
    }

    fun initYoutubeDL(appContext: Context, youtubeDLDir: File) {
        if (!youtubeDLDir.exists()) {
            youtubeDLDir.mkdirs()
            try {
                ZipUtils.unzip(appContext.resources.openRawResource(R.raw.yt_dlp), youtubeDLDir)
            } catch (e: Exception) {
                FileUtils.deleteQuietly(youtubeDLDir)
                throw YtdlException("failed to initialize", e)
            }
        }
    }

    private fun initPython(appContext: Context, pythonDir: File) {
        val pythonLib = File(binDir, pythonLibName)
        // using size of lib as version
        val pythonSize = pythonLib.length().toString()
        if (!pythonDir.exists() || shouldUpdatePython(appContext, pythonSize)) {
            FileUtils.deleteQuietly(pythonDir)
            pythonDir.mkdirs()
            try {
                ZipUtils.unzip(pythonLib, pythonDir)
            } catch (e: Exception) {
                FileUtils.deleteQuietly(pythonDir)
                throw YtdlException("failed to initialize", e)
            }
            updatePython(appContext, pythonSize)
        }
    }

    private fun shouldUpdatePython(appContext: Context, version: String): Boolean {
        return version != SharedPrefsHelper.get(
            appContext,
            pythonLibVersion
        )
    }

    private fun updatePython(appContext: Context, version: String) {
        SharedPrefsHelper.update(appContext, pythonLibVersion, version)
    }

    private fun assertInit() {
        check(initialized) { "instance not initialized" }
    }

    @Throws(YtdlException::class, InterruptedException::class)
    fun getInfo(url: String?): VideoInfo {
        val request = YtdlRequest(url!!)
        return getInfo(request)
    }

    @Throws(YtdlException::class, InterruptedException::class)
    fun getInfo(request: YtdlRequest): VideoInfo {
        request.addOption("--dump-json")
        val response = execute(request, null)
        return try {
            objectMapper.readValue(response.out, VideoInfo::class.java)
        } catch (e: IOException) {
            throw YtdlException("Unable to parse video information", e)
        }
    }

    @JvmOverloads
    @Throws(YtdlException::class, InterruptedException::class)
    fun execute(
        request: YtdlRequest,
        callback: DownloadProgressCallback?
    ): YtdlResponse {
        assertInit()

        // disable caching unless explicitly requested
        if (request.getOption("--cache-dir") == null) {
            request.addOption("--no-cache-dir")
        }
        val ytdlResponse: YtdlResponse
        val process: Process
        val exitCode: Int
        val outBuffer = StringBuffer() //stdout
        val errBuffer = StringBuffer() //stderr
        val startTime = System.currentTimeMillis()
        val args = request.buildCommand()
        val command: MutableList<String> = ArrayList()
        command.addAll(Arrays.asList(pythonPath!!.absolutePath, youtubeDLPath!!.absolutePath))
        command.addAll(args)
        val processBuilder = ProcessBuilder(command)
        val env = processBuilder.environment()
        env["LD_LIBRARY_PATH"] = ENV_LD_LIBRARY_PATH
        env["SSL_CERT_FILE"] = ENV_SSL_CERT_FILE
        env["PATH"] = System.getenv("PATH") + ":" + binDir!!.absolutePath
        env["PYTHONHOME"] = ENV_PYTHONHOME
        process = try {
            processBuilder.start()
        } catch (e: IOException) {
            throw YtdlException(e)
        }
        val outStream = process.inputStream
        val errStream = process.errorStream
        val stdOutProcessor = StreamProcessExtractor(outBuffer, outStream, callback)
        val stdErrProcessor = StreamGobbler(errBuffer, errStream)
        exitCode = try {
            stdOutProcessor.join()
            stdErrProcessor.join()
            process.waitFor()
        } catch (e: InterruptedException) {
            process.destroy()
            throw e
        }
        val out = outBuffer.toString()
        val err = errBuffer.toString()
        if (exitCode > 0) {
            throw YtdlException(err)
        }
        val elapsedTime = System.currentTimeMillis() - startTime
        ytdlResponse = YtdlResponse(command, exitCode, elapsedTime, out, err)
        return ytdlResponse
    }

    fun version(appContext: Context?): String? {
        return SharedPrefsHelper.get(appContext, "youtubeDLVersion")
    }

    companion object {
        val instance = Ytdl()
        const val baseName = "youtubedl-android"
        private const val packagesRoot = "packages"
        private const val pythonBinName = "libpython.bin.so"
        private const val pythonLibName = "libpython.zip.so"
        private const val pythonDirName = "python"
        private const val ffmpegDirName = "ffmpeg"
        const val youtubeDLDirName = "youtube-dl"
        private const val youtubeDLBin = "__main__.py"
        private const val pythonLibVersion = "pythonLibVersion"
        @kotlin.jvm.JvmField
        val objectMapper = ObjectMapper()
    }
}