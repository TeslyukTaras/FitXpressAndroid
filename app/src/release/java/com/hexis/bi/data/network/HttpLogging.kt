package com.hexis.bi.data.network

import okhttp3.Interceptor

/** Release no-op; debug variant supplies the real interceptor. */
fun httpLoggingInterceptor(): Interceptor? = null
