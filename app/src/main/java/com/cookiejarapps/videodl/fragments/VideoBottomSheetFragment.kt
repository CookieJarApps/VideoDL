package com.cookiejarapps.videodl.fragments

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.*
import com.cookiejarapps.videodl.R
import com.cookiejarapps.videodl.MainActivity
import com.cookiejarapps.videodl.adapters.VideoAdapter
import com.cookiejarapps.videodl.adapters.VideoInfoListener
import com.cookiejarapps.videodl.item.VideoInfoItem
import com.cookiejarapps.videodl.models.LoadState
import com.cookiejarapps.videodl.models.VideoInfoViewModel
import com.cookiejarapps.videodl.worker.DownloadWorker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_video_info.*
import kotlinx.android.synthetic.main.fragment_video_info.view.*


class VideoBottomSheetFragment : BottomSheetDialogFragment(),
    SAFDialogFragment.DialogListener {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // get the views and attach the listener
        return inflater.inflate(
            R.layout.fragment_video_info, container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = arguments?.getString("url")

        val vidFormatsVm =
            ViewModelProvider(activity as MainActivity).get(VideoInfoViewModel::class.java)
        if (url != null) {
            vidFormatsVm.fetchInfo(url)
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
            layoutManager = GridLayoutManager(context, 4)
        }
        videoFormatsModel.vidFormats.observe(viewLifecycleOwner, { t ->
            (video_list.adapter as VideoAdapter).updateAdapter(t)

            if (t != null) {
                videoName.text = t.title
            }
        })
        videoFormatsModel.loadState.observe(viewLifecycleOwner) { t ->
            when (t) {
                LoadState.INITIAL -> {
                    openInApp.visibility = View.GONE
                    videoThumbnail.visibility = View.GONE
                    videoName.visibility = View.GONE
                    video_list.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                    errorMessage.visibility = View.GONE
                    errorDetails.visibility = View.GONE
                }
                LoadState.LOADING -> {
                    openInApp.visibility = View.GONE
                    videoThumbnail.visibility = View.GONE
                    videoName.visibility = View.GONE
                    video_list.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                    errorMessage.visibility = View.GONE
                    errorDetails.visibility = View.GONE
                }
                LoadState.LOADED -> {
                    openInApp.visibility = View.VISIBLE
                    videoThumbnail.visibility = View.VISIBLE
                    videoName.visibility = View.VISIBLE
                    video_list.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    errorMessage.visibility = View.GONE
                    errorDetails.visibility = View.GONE
                }
                LoadState.ERRORED -> {
                    openInApp.visibility = View.GONE
                    videoThumbnail.visibility = View.GONE
                    videoName.visibility = View.GONE
                    video_list.visibility = View.GONE
                    progressBar.visibility = View.GONE
                    errorMessage.visibility = View.VISIBLE
                    errorDetails.visibility = View.GONE
                }
            }
        }
        // If VideoInfoViewModel sends an error message, show it
        vidFormatsVm.error.observe(viewLifecycleOwner) { error ->
            errorDetails.visibility = View.VISIBLE
            errorDetails.text = error
        }
        vidFormatsVm.thumbnail.observe(viewLifecycleOwner) {
            it?.apply {
                val picasso = Picasso.get()
                picasso.load(this)
                    .into(videoThumbnail)
            } ?: videoThumbnail.setImageResource(R.drawable.ic_video)
        }
        vidFormatsVm.url.observe(viewLifecycleOwner) { bestQualityUrl ->
            openInApp.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(bestQualityUrl), "video/mp4")
                startActivity(intent)
            }
        }
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
    private fun startDownload(vidFormatItem: VideoInfoItem.VideoFormatItem, downloadDir: String) {
        val downloader = PreferenceManager.getDefaultSharedPreferences(context).getString(
            DOWNLOAD_MANAGER, resources.getString(
                R.string.internal
            )
        )
        val downloaderActivity = PreferenceManager.getDefaultSharedPreferences(context).getString(
            DOWNLOAD_MANAGER_ACTIVITY, ""
        )

        if(downloader == resources.getString(R.string.internal)){
            val videoInfo = vidFormatItem.vidInfo
            val videoFormat = vidFormatItem.vidFormat
            val workManager = WorkManager.getInstance(activity?.applicationContext!!)
            val workData = workDataOf(
                DownloadWorker.urlKey to videoInfo.webpageUrl,
                DownloadWorker.nameKey to videoInfo.title,
                DownloadWorker.formatIdKey to videoFormat.formatId,
                DownloadWorker.audioCodecKey to videoFormat.acodec,
                DownloadWorker.videoCodecKey to videoFormat.vcodec,
                DownloadWorker.downloadDirKey to downloadDir,
                DownloadWorker.sizeKey to videoFormat.filesize,
                DownloadWorker.videoId to videoInfo.id
            )
            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workData)
                .build()

            workManager.enqueue(
                workRequest
            )
            Toast.makeText(
                context,
                R.string.download_queued,
                Toast.LENGTH_LONG
            ).show()


            val navController = Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment
            )
            val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
            navController.navigate(R.id.downloads_fragment, null, navOptions)

            dismiss()
        }
        else{
            val launchIntent: Intent = requireActivity().applicationContext.packageManager
                .getLaunchIntentForPackage(downloader!!)!!
            launchIntent.setComponent(ComponentName(downloader!!, downloaderActivity!!))
            launchIntent.action = Intent.ACTION_VIEW
            launchIntent.data = Uri.parse(vidFormatItem.vidFormat.url)
            startActivity(launchIntent)
        }
    }

    private fun setDownloadLocation(path: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.getString(DOWNLOAD_LOCATION, null) ?: prefs.edit()
            .putString(DOWNLOAD_LOCATION, path).apply()
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
            if (ContextCompat.checkSelfPermission(
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

    companion object {
        fun newInstance(url: String): VideoBottomSheetFragment {
            val video = VideoBottomSheetFragment()

            val args = Bundle()
            args.putString("url", url)
            video.arguments = args

            return video
        }

        const val downloadLocationDialogTag = "download_location_chooser_dialog"
        private const val OPEN_DIRECTORY_REQUEST_CODE = 37321
        private const val DOWNLOAD_LOCATION = "download"
        private const val DOWNLOAD_MANAGER = "download_manager"
        private const val DOWNLOAD_MANAGER_ACTIVITY = "download_manager_activity"
    }
}