package com.cookiejarapps.smartcookieweb_ytdl.fragments

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.work.*
import com.cookiejarapps.smartcookieweb_ytdl.MainActivity
import com.cookiejarapps.smartcookieweb_ytdl.R
import com.cookiejarapps.smartcookieweb_ytdl.item.VideoInfoItem
import com.cookiejarapps.smartcookieweb_ytdl.models.VideoInfoViewModel
import com.cookiejarapps.smartcookieweb_ytdl.worker.DownloadWorker
import com.cookiejarapps.smartcookieweb_ytdl.worker.DownloadWorker.Companion.audioCodecKey
import com.cookiejarapps.smartcookieweb_ytdl.worker.DownloadWorker.Companion.downloadDirKey
import com.cookiejarapps.smartcookieweb_ytdl.worker.DownloadWorker.Companion.formatIdKey
import com.cookiejarapps.smartcookieweb_ytdl.worker.DownloadWorker.Companion.nameKey
import com.cookiejarapps.smartcookieweb_ytdl.worker.DownloadWorker.Companion.sizeKey
import com.cookiejarapps.smartcookieweb_ytdl.worker.DownloadWorker.Companion.urlKey
import com.cookiejarapps.smartcookieweb_ytdl.worker.DownloadWorker.Companion.videoCodecKey
import com.cookiejarapps.smartcookieweb_ytdl.worker.DownloadWorker.Companion.videoId
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment(),
    SAFDialogFragment.DialogListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        initialize(view)
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun initialize(view: View) {

        urlEditText.setOnEditorActionListener { v, actionId, event ->
            val handled = false
            if (actionId == EditorInfo.IME_ACTION_GO) {
                /*val vidFormatsVm =
                    ViewModelProvider(activity as MainActivity).get(VideoInfoViewModel::class.java)
                vidFormatsVm.fetchInfo(urlEditText.text.toString())*/
                view.let { activity?.hideKeyboard(it) }
                openBottomSheet(urlEditText.text.toString())
            }
            handled
        }

        urlInputLayout.setEndIconOnClickListener {
           /* val vidFormatsVm =
                ViewModelProvider(activity as MainActivity).get(VideoInfoViewModel::class.java)
            vidFormatsVm.fetchInfo(urlEditText.text.toString())*/
            view.let { activity?.hideKeyboard(it) }
            openBottomSheet(urlEditText.text.toString())
        }

        urlEditText.setOnFocusChangeListener { _: View, b: Boolean ->
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipboardItem = clipboard.primaryClip?.getItemAt(0)
            val pasteData = clipboardItem?.text

            if (pasteData != null && URLUtil.isValidUrl("" + pasteData)) {
                val suggestions = arrayOf("" + pasteData)

                val autoCompleteAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line, suggestions
                )
                urlEditText.setAdapter(autoCompleteAdapter)
            }

            if (!b) {
                val inputMethodManager = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }
            else{
                urlEditText.showDropDown()
            }
        }
    }

    fun openBottomSheet(url: String){
        val videoBottomSheetFragment: VideoBottomSheetFragment =
            VideoBottomSheetFragment.newInstance(url)
        videoBottomSheetFragment.show(
            requireActivity().supportFragmentManager,
            "video_bottom_sheet"
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OPEN_DIRECTORY_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let {
                        activity?.contentResolver?.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        setDownloadLocation(it.toString())
                        val videoFormatsModel =
                            ViewModelProvider(activity as MainActivity).get(VideoInfoViewModel::class.java)
                        startDownload(videoFormatsModel.selectedItem, it.toString())
                    }
                }
            }
        }
    }

    private fun setDownloadLocation(path: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.getString(DOWNLOAD_LOCATION, null) ?: prefs.edit()
            .putString(DOWNLOAD_LOCATION, path).apply()
    }

    private fun startDownload(vidFormatItem: VideoInfoItem.VideoFormatItem, downloadDir: String) {
        val videoInfo = vidFormatItem.vidInfo
        val videoFormat = vidFormatItem.vidFormat
        val workTag = videoInfo.id
        val workManager = WorkManager.getInstance(activity?.applicationContext!!)
        val state = workManager.getWorkInfosByTag(workTag).get()?.getOrNull(0)?.state
        val running = state === WorkInfo.State.RUNNING || state === WorkInfo.State.ENQUEUED
        if (running) {
            Toast.makeText(
                context,
                R.string.download_already_running,
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val workData = workDataOf(
            urlKey to videoInfo.webpageUrl,
            nameKey to videoInfo.title,
            formatIdKey to videoFormat.formatId,
            audioCodecKey to videoFormat.acodec,
            videoCodecKey to videoFormat.vcodec,
            downloadDirKey to downloadDir,
            sizeKey to videoFormat.filesize,
            videoId to videoInfo.id
        )
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag(workTag)
            .setInputData(workData)
            .build()

        workManager.enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        Toast.makeText(
            context,
            R.string.download_queued,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onAccept(dialog: SAFDialogFragment) {
        val videoFormatsModel =
            ViewModelProvider(activity as MainActivity).get(VideoInfoViewModel::class.java)
        val path = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(DOWNLOAD_LOCATION, null)
        if (path == null) {
            Toast.makeText(context, R.string.invalid_download_location, Toast.LENGTH_SHORT).show()
            return
        }
        startDownload(videoFormatsModel.selectedItem, path)
    }

    override fun onPickFile(dialog: SAFDialogFragment) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
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
            SAFDialogFragment().show(
                childFragmentManager,
                downloadLocationDialogTag
            )
        }
    }

    companion object {
        const val downloadLocationDialogTag = "download_location_chooser_dialog"
        private const val OPEN_DIRECTORY_REQUEST_CODE = 37321
        private const val DOWNLOAD_LOCATION = "download"
    }

}
