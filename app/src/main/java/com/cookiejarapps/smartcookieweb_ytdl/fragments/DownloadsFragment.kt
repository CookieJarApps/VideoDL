package com.cookiejarapps.smartcookieweb_ytdl.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemLongClickListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.smartcookieweb_ytdl.R
import com.cookiejarapps.smartcookieweb_ytdl.adapters.DownloadsAdapter
import com.cookiejarapps.smartcookieweb_ytdl.models.DownloadsViewModel


class DownloadsFragment : Fragment() {

    private lateinit var downloadsViewModel: DownloadsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val list: RecyclerView = inflater.inflate(R.layout.fragment_downloads_list, container, false) as RecyclerView

        list.adapter = DownloadsAdapter()

        downloadsViewModel = ViewModelProvider(this).get(DownloadsViewModel::class.java)
        downloadsViewModel.downloadList.observe(viewLifecycleOwner, { downloads ->
            downloads?.let { (list.adapter as DownloadsAdapter).updateDataSet(downloads) }
        })

        return list
    }

}
