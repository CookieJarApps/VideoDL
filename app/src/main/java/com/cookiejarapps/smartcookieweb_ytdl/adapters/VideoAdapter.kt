package com.cookiejarapps.smartcookieweb_ytdl.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.cookiejarapps.smartcookieweb_ytdl.R
import com.cookiejarapps.smartcookieweb_ytdl.Utils
import com.cookiejarapps.smartcookieweb_ytdl.item.VideoInfoItem
import kotlinx.android.synthetic.main.video_info.view.*
import kotlinx.android.synthetic.main.video_row.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class VideoAdapter(private val clickListener: VideoInfoListener) :
    ListAdapter<VideoInfoItem, RecyclerView.ViewHolder>(
        VideoInfoDiffCallback()
    ) {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    fun updateAdapter(vidInfo: VideoInfo?) {
        adapterScope.launch {
            if (vidInfo == null) {
                submitList(emptyList())
                return@launch
            }
            val items = mutableListOf<VideoInfoItem>()
            withContext(Dispatchers.Default) {
                vidInfo.formats?.forEach { format ->
                    items.add(
                        VideoInfoItem.VideoFormatItem(
                            vidInfo,
                            format.formatId
                        )
                    )
                }
                items.reverse()
                items.add(0, VideoInfoItem.VideoHeaderItem(vidInfo))
            }

            withContext(Dispatchers.Main) {
                submitList(items.toList())
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val vidItem = getItem(position) as VideoInfoItem.VideoFormatItem
                val vidFormat = vidItem.vidFormat
                val size = Utils.fileSizeToString(vidFormat.filesize)

                with(holder.itemView) {
                    name.text = vidFormat.format
                    if (vidFormat.acodec != "none" && vidFormat.vcodec == "none") {
                        icon.setImageResource(R.drawable.ic_audio)
                        info.text =
                            "${vidFormat.ext}, ${size}"
                    } else {
                        icon.setImageResource(R.drawable.ic_video)
                        info.text =
                            "${vidFormat.ext}, ${size}, ${vidItem.vidFormat.width}x${vidItem.vidFormat.height}"
                    }
                    setOnClickListener { clickListener.onClick(vidItem) }
                }
            }
            else -> {
                val videoItem = getItem(position) as VideoInfoItem.VideoHeaderItem
                val videoInfo = videoItem.vidInfo

                with(holder.itemView) {
                    download_title.text = videoInfo.title
                    author.text = videoInfo.uploader
                    author.isSelected = true
                    website.text = videoInfo.webpageUrl
                    if(videoInfo.uploadDate != null){
                        val parseFormat = SimpleDateFormat("yyyyMMDD")
                        val parsedDate: Date? = parseFormat.parse(videoInfo.uploadDate)
                        val format = SimpleDateFormat("DD MMM yyyy")
                        upload_date.text = format.format(parsedDate)
                    }
                    videoInfo.duration.toLong().apply {
                        val minutes = TimeUnit.SECONDS.toMinutes(this)
                        val seconds = this - TimeUnit.MINUTES.toSeconds(minutes)
                        length.text = "$minutes:$seconds"
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> HeaderViewHolder.from(
                parent
            )
            1 -> ViewHolder.from(
                parent
            )
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is VideoInfoItem.VideoHeaderItem -> 0
            is VideoInfoItem.VideoFormatItem -> 1
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.video_info, parent, false)
                return HeaderViewHolder(
                    view
                )
            }
        }
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.video_row, parent, false)
                return ViewHolder(view)
            }
        }
    }
}

class VideoInfoDiffCallback : DiffUtil.ItemCallback<VideoInfoItem>() {
    override fun areItemsTheSame(oldItem: VideoInfoItem, newItem: VideoInfoItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: VideoInfoItem, newItem: VideoInfoItem): Boolean {
        return oldItem == newItem
    }
}


class VideoInfoListener(val clickListener: (VideoInfoItem.VideoFormatItem) -> Unit) {
    fun onClick(vidInfo: VideoInfoItem.VideoFormatItem) = clickListener(vidInfo)
}

