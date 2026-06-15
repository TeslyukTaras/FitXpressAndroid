package com.hexis.bi.data.network

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

/** Debug-only OkHttp logging interceptor (release variant returns null). */
fun httpLoggingInterceptor(): Interceptor? =
    HttpLoggingInterceptor { line -> Timber.tag("OkHttp").d(line) }
        .setLevel(HttpLoggingInterceptor.Level.BODY)
