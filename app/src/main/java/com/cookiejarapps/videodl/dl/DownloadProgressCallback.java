package com.cookiejarapps.videodl.dl;

public interface DownloadProgressCallback {
    void onProgressUpdate(float progress, long etaInSeconds);
}