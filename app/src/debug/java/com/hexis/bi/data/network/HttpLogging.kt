package com.hexis.bi.data.network

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

fun httpLoggingInterceptor(): Interceptor? =
    HttpLoggingInterceptor { line -> Timber.tag("OkHttp").d(line) }
        .setLevel(HttpLoggingInterceptor.Level.BODY)
