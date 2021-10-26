package com.cookiejarapps.smartcookieweb_ytdl.dl

import java.util.ArrayList
import java.util.LinkedHashMap

class YtdlOptions {
    private val options: MutableMap<String, String?> = LinkedHashMap()
    fun addOption(key: String, value: String): YtdlOptions {
        options[key] = value
        return this
    }

    fun addOption(key: String, value: Number): YtdlOptions {
        options[key] = value.toString()
        return this
    }

    fun addOption(key: String): YtdlOptions {
        options[key] = null
        return this
    }

    fun getOption(key: String): Any? {
        return options[key]
    }

    fun buildOptions(): List<String> {
        val optionsList: MutableList<String> = ArrayList()
        for ((name, value) in options) {
            optionsList.add(name)
            if (null != value) optionsList.add(value)
        }
        return optionsList
    }
}