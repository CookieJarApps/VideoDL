package com.cookiejarapps.smartcookieweb_ytdl.models

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookiejarapps.smartcookieweb_ytdl.item.VideoInfoItem
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoInfoViewModel : ViewModel() {

    val vidFormats: MutableLiveData<VideoInfo> = MutableLiveData()
    val loadState: MutableLiveData<LoadState> = MutableLiveData(LoadState.INITIAL)
    val url: MutableLiveData<String> = MutableLiveData()
    val thumbnail: MutableLiveData<String> = MutableLiveData()
    val error: MutableLiveData<String> = MutableLiveData()
    lateinit var selectedItem: VideoInfoItem.VideoFormatItem

    private fun submit(vidInfoItems: VideoInfo?) {
        vidFormats.postValue(vidInfoItems)
    }

    private fun updateLoading(loadState: LoadState) {
        this.loadState.postValue(loadState)
    }

    private fun updateUrl(url: String?) {
        this.url.postValue(url)
    }

    private fun updateThumbnail(thumbnail: String?) {
        this.thumbnail.postValue(thumbnail)
    }

    private fun updateErrorMessage(error: String?) {
        this.error.postValue(error)
    }

    fun fetchInfo(url: String) {
        viewModelScope.launch {
            updateLoading(LoadState.LOADING)
            submit(null)
            updateUrl(null)
            lateinit var vidInfo: VideoInfo
            lateinit var bestQualityUrl: String
            try {
                withContext(Dispatchers.IO) {
                    // Get video data
                    vidInfo = YoutubeDL.getInstance().getInfo(url)

                    // Get high quality URL for playback
                    val request = YoutubeDLRequest(url)
                    request.addOption("-f", "best")
                    val streamInfo = YoutubeDL.getInstance().getInfo(request)
                    bestQualityUrl = streamInfo.url
                }
            } catch (e: Exception) {
                updateLoading(LoadState.ERRORED)
                updateErrorMessage(e.localizedMessage)
                return@launch
            }

            updateLoading(LoadState.LOADED)
            updateThumbnail(vidInfo.thumbnail)
            updateUrl(bestQualityUrl)
            submit(vidInfo)
        }
    }

}

enum class LoadState {
    INITIAL, LOADING, LOADED, ERRORED
}
