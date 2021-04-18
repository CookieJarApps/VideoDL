package com.cookiejarapps.smartcookieweb_ytdl.fragments

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.work.*
import com.cookiejarapps.smartcookieweb_ytdl.MainActivity
import com.cookiejarapps.smartcookieweb_ytdl.R
import com.cookiejarapps.smartcookieweb_ytdl.adapters.VideoAdapter
import com.cookiejarapps.smartcookieweb_ytdl.adapters.VideoInfoListener
import com.cookiejarapps.smartcookieweb_ytdl.item.VideoInfoItem
import com.cookiejarapps.smartcookieweb_ytdl.models.LoadState
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
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import com.huxq17.download.DownloadProvider
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : Fragment(),
    SAFDialogFragment.DialogListener {

    lateinit var player: SimpleExoPlayer

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
        player = SimpleExoPlayer.Builder(DownloadProvider.context).build()
        video_player.player = player
        video_player.resizeMode = RESIZE_MODE_ZOOM

        urlEditText.setOnEditorActionListener { v, actionId, event ->
            val handled = false
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val vidFormatsVm =
                    ViewModelProvider(activity as MainActivity).get(VideoInfoViewModel::class.java)
                vidFormatsVm.fetchInfo(urlEditText.text.toString())
                view.let { activity?.hideKeyboard(it) }
            }
            handled
        }

        urlInputLayout.setEndIconOnClickListener {
            val vidFormatsVm =
                ViewModelProvider(activity as MainActivity).get(VideoInfoViewModel::class.java)
            vidFormatsVm.fetchInfo(urlEditText.text.toString())
            view.let { activity?.hideKeyboard(it) }
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


        val videoFormatsModel =
            ViewModelProvider(activity as MainActivity).get(VideoInfoViewModel::class.java)
        with(view.video_list) {
            adapter =
                VideoAdapter(VideoInfoListener listener@{
                    videoFormatsModel.selectedItem = it
                    if (!isStoragePermissionGranted()) {
                        return@listener
                    }
                    SAFDialogFragment().show(
                        childFragmentManager,
                        downloadLocationDialogTag
                    )

                })
        }
        videoFormatsModel.vidFormats.observe(viewLifecycleOwner, { t ->
            (video_list.adapter as VideoAdapter).updateAdapter(t)
        })
        videoFormatsModel.loadState.observe(viewLifecycleOwner, { t ->
            when (t) {
                LoadState.INITIAL -> {
                    loading_indicator.visibility = GONE
                    loading_text.visibility = GONE
                    //video_list.visibility = GONE
                    urlEditText.visibility = VISIBLE
                    urlInputLayout.visibility = VISIBLE
                    start_text.visibility = VISIBLE
                    error_text.visibility = GONE
                    app_icon.visibility = VISIBLE
                    video_player.visibility = GONE
                }
                LoadState.LOADING -> {
                    loading_indicator.visibility = VISIBLE
                    loading_text.visibility = VISIBLE
                    urlEditText.visibility = GONE
                    urlInputLayout.visibility = GONE
                    start_text.visibility = GONE
                    video_list.visibility = GONE
                    error_text.visibility = GONE
                    app_icon.visibility = GONE
                    video_player.visibility = GONE
                }
                LoadState.LOADED -> {
                    loading_indicator.visibility = GONE
                    loading_text.visibility = GONE
                    start_text.visibility = GONE
                    urlEditText.visibility = GONE
                    urlInputLayout.visibility = GONE
                    video_list.visibility = VISIBLE
                    app_icon.visibility = GONE
                    video_player.visibility = VISIBLE
                }
                LoadState.ERRORED -> {
                    loading_indicator.visibility = GONE
                    loading_text.visibility = GONE
                    video_list.visibility = GONE
                    urlEditText.visibility = VISIBLE
                    start_text.visibility = VISIBLE
                    error_text.visibility = VISIBLE
                    video_player.visibility = GONE
                }
            }
        })
        videoFormatsModel.url.observe(viewLifecycleOwner, Observer {
            it?.apply {
                val mediaItem: MediaItem = MediaItem.fromUri(it)
                player.setMediaItem(mediaItem)
                player.prepare()
            }
        })
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

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    companion object {
        const val downloadLocationDialogTag = "download_location_chooser_dialog"
        private const val OPEN_DIRECTORY_REQUEST_CODE = 37321
        private const val DOWNLOAD_LOCATION = "download"
    }

}
