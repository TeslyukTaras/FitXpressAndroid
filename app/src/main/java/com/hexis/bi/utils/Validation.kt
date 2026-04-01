package com.hexis.bi.utils

private val EMAIL_REGEX = Regex("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")

fun String.isValidEmail(): Boolean = EMAIL_REGEX.matches(this.trim())
