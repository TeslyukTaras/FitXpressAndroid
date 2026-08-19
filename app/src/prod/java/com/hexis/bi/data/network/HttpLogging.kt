package com.hexis.bi.data.network

import okhttp3.Interceptor

/** Network payload logging is intentionally disabled in production-like builds. */
fun httpLoggingInterceptor(): Interceptor? = null
