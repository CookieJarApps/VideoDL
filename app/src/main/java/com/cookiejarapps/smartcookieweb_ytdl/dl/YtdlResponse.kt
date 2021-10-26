package com.cookiejarapps.smartcookieweb_ytdl.dl

class YtdlResponse(
    val command: List<String>,
    val exitCode: Int,
    val elapsedTime: Long,
    val out: String,
    val err: String
)