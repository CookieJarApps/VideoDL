package com.cookiejarapps.videodl.item

import com.cookiejarapps.videodl.dl.mapper.VideoFormat
import com.cookiejarapps.videodl.dl.mapper.VideoInfo

sealed class VideoInfoItem {
    abstract val id: String

    data class VideoFormatItem(val vidInfo: VideoInfo, val formatId: String) : VideoInfoItem() {
        override val id = vidInfo.id + "_" + formatId
        val vidFormat: VideoFormat = vidInfo.formats.find { f -> f.formatId == formatId }!!
    }

    data class VideoHeaderItem(val vidInfo: VideoInfo) : VideoInfoItem() {
        override val id: String = vidInfo.id
    }
}