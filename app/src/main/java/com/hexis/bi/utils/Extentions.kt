package com.hexis.bi.utils

/** Converts `snake_case` / `snake_case_words` to `camelCase`. Passes through strings with no underscores. */
fun String.snakeToCamel(): String {
    if (!contains('_')) return this
    return split('_').mapIndexed { i, part ->
        if (i == 0) part else part.replaceFirstChar { it.uppercase() }
    }.joinToString("")
}
