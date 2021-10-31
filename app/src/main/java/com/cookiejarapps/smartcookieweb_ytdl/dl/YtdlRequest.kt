package com.cookiejarapps.smartcookieweb_ytdl.dl


import java.util.*

class YtdlRequest {
    private var urls: List<String>
    private val options = YtdlOptions()

    constructor(url: String) {
        urls = Arrays.asList(url)
    }

    constructor(urls: List<String>) {
        this.urls = urls
    }

    fun addOption(key: String, value: String): YtdlRequest {
        options.addOption(key, value)
        return this
    }

    fun addOption(key: String, value: Number): YtdlRequest {
        options.addOption(key, value)
        return this
    }

    fun addOption(key: String?): YtdlRequest {
        options.addOption(key!!)
        return this
    }

    fun getOption(key: String?): Any? {
        return options.getOption(key!!)
    }

    fun buildCommand(): List<String> {
        val command: MutableList<String> = ArrayList()
        command.addAll(options.buildOptions())
        command.addAll(urls)
        return command
    }
}