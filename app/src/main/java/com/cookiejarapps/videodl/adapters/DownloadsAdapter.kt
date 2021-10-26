package com.cookiejarapps.videodl.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.videodl.R
import com.cookiejarapps.videodl.database.Download
import kotlinx.android.synthetic.main.download_row.view.*


class DownloadsAdapter : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

    private var downloadList: List<Download> = emptyList()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    fun updateDataSet(items: List<Download>) {
        downloadList = items
        notifyDataSetChanged()
    }

    fun getDownloadList(): List<Download>{
        return downloadList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.download_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.itemView) {
            download_title.text = downloadList[position].name
            download_progress.setProgressCompat(downloadList[position].downloadPercent.toInt(), true)
            download_info.text = "${downloadList[position].downloadPercent}% "
            when(downloadList[position].fileType) {
                "audio" -> icon.setImageResource(R.drawable.ic_audio)
                "video" ->icon.setImageResource(R.drawable.ic_video)
                else ->icon.setImageResource(R.drawable.ic_download)
            }
        }
    }

    override fun getItemCount() = downloadList.size

}
