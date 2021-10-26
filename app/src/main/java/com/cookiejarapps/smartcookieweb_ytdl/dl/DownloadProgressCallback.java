package com.cookiejarapps.smartcookieweb_ytdl.dl;

public interface DownloadProgressCallback {
    void onProgressUpdate(float progress, long etaInSeconds);
}