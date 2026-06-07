package com.example.diplom.data

import com.example.diplom.BuildConfig

/** Resolves relative paths from the API (e.g. `/uploads/...`) against [BuildConfig.API_BASE_URL]. */
fun apiAbsoluteUrl(pathOrUrl: String?): String? {
    if (pathOrUrl.isNullOrBlank()) return null
    if (pathOrUrl.startsWith("http://", ignoreCase = true) ||
        pathOrUrl.startsWith("https://", ignoreCase = true)
    ) {
        return pathOrUrl
    }
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    val path = pathOrUrl.trimStart('/')
    return "$base/$path"
}
