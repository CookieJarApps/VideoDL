package com.cookiejarapps.videodl.dl

import java.lang.Exception

class YtdlException : Exception {
    constructor(message: String?) : super(message) {}
    constructor(message: String?, e: Throwable?) : super(message, e) {}
    constructor(e: Throwable?) : super(e) {}
}