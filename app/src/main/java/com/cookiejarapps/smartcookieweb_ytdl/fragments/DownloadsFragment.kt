package com.cookiejarapps.smartcookieweb_ytdl.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import com.cookiejarapps.smartcookieweb_ytdl.R
import com.cookiejarapps.smartcookieweb_ytdl.adapters.DownloadsAdapter
import com.cookiejarapps.smartcookieweb_ytdl.database.DownloadDatabase
import com.cookiejarapps.smartcookieweb_ytdl.database.DownloadsRepository
import com.cookiejarapps.smartcookieweb_ytdl.listener.DownloadListListener
import com.cookiejarapps.smartcookieweb_ytdl.listener.RecyclerViewClickListener
import com.cookiejarapps.smartcookieweb_ytdl.models.DownloadsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class DownloadsFragment : Fragment() {

    private lateinit var downloadsViewModel: DownloadsViewModel

    private fun openFile(filePath: String, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
        val fileURI = Uri.parse(filePath)
        val mimeType = context.contentResolver.getType(fileURI) ?: "*/*"
        intent.setDataAndType(fileURI, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (intent.resolveActivity(context.packageManager) != null) {
            ContextCompat.startActivity(context, intent, null)
        } else {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val list: RecyclerView = inflater.inflate(
            R.layout.fragment_downloads_list,
            container,
            false
        ) as RecyclerView

        list.adapter = DownloadsAdapter()

        downloadsViewModel = ViewModelProvider(this).get(DownloadsViewModel::class.java)
        downloadsViewModel.downloadList.observe(viewLifecycleOwner, { downloads ->
            downloads?.let { (list.adapter as DownloadsAdapter).updateDataSet(downloads) }
        })

        list.addOnItemTouchListener(
            RecyclerViewClickListener(
                activity,
                list,
                object : DownloadListListener {
                    override fun onClick(view: View?, position: Int) {
                        if((list.adapter as DownloadsAdapter).getDownloadList()[position].downloadPercent == 100.00){
                            openFile((list.adapter as DownloadsAdapter).getDownloadList()[position].downloadPath!!, requireContext())
                        }
                    }

                    override fun onLongClick(view: View?, position: Int) {
                        if((list.adapter as DownloadsAdapter).getDownloadList()[position].downloadPercent == 100.00) {
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setTitle((list.adapter as DownloadsAdapter).getDownloadList()[position].name)

                            val downloads = arrayOf(
                                resources.getString(R.string.delete_download),
                                resources.getString(R.string.delete_download_device)
                            )
                            builder.setItems(downloads) { _, which ->
                                when (which) {
                                    0 -> {
                                        val downloadsDao = DownloadDatabase.getDatabase(
                                            context!!
                                        ).downloadsDao()
                                        val repository =
                                            DownloadsRepository(downloadsDao)

                                        GlobalScope.launch {
                                            repository.deleteDownloads((list.adapter as DownloadsAdapter).getDownloadList()[position])
                                        }

                                        downloadsViewModel.downloadList.observe(viewLifecycleOwner, { downloads ->
                                            downloads?.let { (list.adapter as DownloadsAdapter).updateDataSet(downloads) }
                                        })
                                    }
                                    1 -> {
                                        val downloadsDao = DownloadDatabase.getDatabase(
                                            context!!
                                        ).downloadsDao()
                                        val repository =
                                            DownloadsRepository(downloadsDao)

                                        GlobalScope.launch {
                                            repository.deleteDownloads((list.adapter as DownloadsAdapter).getDownloadList()[position])
                                        }

                                        DocumentFile.fromSingleUri(
                                            requireContext(),
                                            (list.adapter as DownloadsAdapter).getDownloadList()[position].downloadPath!!.toUri()
                                        )?.delete()

                                        downloadsViewModel.downloadList.observe(viewLifecycleOwner, { downloads ->
                                            downloads?.let { (list.adapter as DownloadsAdapter).updateDataSet(downloads) }
                                        })
                                    }
                                }
                            }

                            val dialog = builder.create()
                            dialog.show()
                        }
                        else{
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setTitle((list.adapter as DownloadsAdapter).getDownloadList()[position].name)

                            val downloads = arrayOf(
                                resources.getString(R.string.cancel)
                            )
                            builder.setItems(downloads) { _, which ->
                                when (which) {
                                    0 -> {
                                        val downloadsDao = DownloadDatabase.getDatabase(
                                            context!!
                                        ).downloadsDao()
                                        val repository =
                                            DownloadsRepository(downloadsDao)

                                        GlobalScope.launch {
                                            repository.deleteDownloads((list.adapter as DownloadsAdapter).getDownloadList()[position])
                                        }

                                        val workManager = WorkManager.getInstance(activity?.applicationContext!!)
                                        workManager.cancelUniqueWork((list.adapter as DownloadsAdapter).getDownloadList()[which].timestamp.toString())

                                        downloadsViewModel.downloadList.observe(viewLifecycleOwner, { downloads ->
                                            downloads?.let { (list.adapter as DownloadsAdapter).updateDataSet(downloads) }
                                        })
                                    }
                                }
                            }

                            val dialog = builder.create()
                            dialog.show()
                        }
                    }
                })
        )

        return list
    }

}
