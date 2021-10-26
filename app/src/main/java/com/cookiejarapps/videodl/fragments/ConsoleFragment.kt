package com.cookiejarapps.videodl.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import com.cookiejarapps.videodl.R
import com.cookiejarapps.videodl.dl.YtdlException
import kotlinx.android.synthetic.main.fragment_console.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*


class ConsoleFragment : Fragment() {

    private var command: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        getActivity()!!.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return inflater.inflate(R.layout.fragment_console, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val edittext = consoleEditText as EditText
        edittext.setOnKeyListener(object : View.OnKeyListener {
            @SuppressLint("SetTextI18n")
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && isStoragePermissionGranted()) {
                    addLine(consoleTextView, "> youtube-dl ${edittext.text}")
                    addLine(consoleTextView, "Running command...")

                    GlobalScope.launch {
                        runCommand(edittext.text.toString())
                    }

                    consoleEditText.setText("")
                    return true
                }
                return false
            }
        })
    }

    private fun addLine(textView: TextView, line: String){
        textView.text = "${textView.text} \n ${line}"
    }

    private fun runCommand(command: String) {
        val baseDir = File(requireContext().noBackupFilesDir, "youtubedl-android")
        val packagesDir = File(baseDir, "packages")
        val binDir = File(requireContext().applicationInfo.nativeLibraryDir)
        val pythonPath = File(binDir, "libpython.bin.so")
        val pythonDir = File(packagesDir, "python")
        val ffmpegDir = File(packagesDir, "ffmpeg")

        val youtubeDLDir = File(baseDir, "youtube-dl")
        val youtubeDLPath = File(youtubeDLDir, "__main__.py")

        val process: Process

        val args: List<String> = listOf(command)
        val commands: MutableList<String> = ArrayList()
        commands.addAll(Arrays.asList(pythonPath.getAbsolutePath(), youtubeDLPath.getAbsolutePath()))
        commands.addAll(args)

        val ENV_LD_LIBRARY_PATH = pythonDir.getAbsolutePath() + "/usr/lib" + ":" + ffmpegDir.getAbsolutePath() + "/usr/lib";
        val ENV_SSL_CERT_FILE = pythonDir.getAbsolutePath() + "/usr/etc/tls/cert.pem";
        val ENV_PYTHONHOME = pythonDir.getAbsolutePath() + "/usr";

        val processBuilder = ProcessBuilder(commands)
        val env = processBuilder.environment()
        env["LD_LIBRARY_PATH"] = ENV_LD_LIBRARY_PATH
        env["SSL_CERT_FILE"] = ENV_SSL_CERT_FILE
        env["PATH"] = System.getenv("PATH") + ":" + binDir.getAbsolutePath()
        env["PYTHONHOME"] = ENV_PYTHONHOME

        try {
            process = processBuilder.start()
        } catch (e: IOException) {
            throw YtdlException(e)
        }

        requireActivity().runOnUiThread{
            val outStream: InputStream = process.getInputStream()
            val errStream: InputStream = process.getErrorStream()

            addLine(consoleTextView, IOUtils.toString(outStream))
            addLine(consoleTextView, IOUtils.toString(errStream))

            outStream.close()
            errStream.close()
        }
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            runCommand(command!!)
        }
    }
}
