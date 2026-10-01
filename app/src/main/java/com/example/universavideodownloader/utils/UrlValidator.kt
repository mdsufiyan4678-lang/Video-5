package com.example.universavideodownloader.utils

import android.net.Uri
import java.net.URI
import java.util.Locale

object UrlValidator {

    private val SUPPORTED_SCHEMES = setOf("http", "https")
    private val FORBIDDEN_SCHEMES = setOf("javascript", "file", "content", "intent", "data", "about")

    private val SUPPORTED_EXTENSIONS = listOf(
        ".mp4",
        ".webm",
        ".mov",
        ".mkv",
        ".3gp"
    )

    private val SUPPORTED_MIME_TYPES = setOf(
        "video/mp4",
        "video/webm",
        "video/quicktime",
        "video/x-matroska",
        "video/3gpp"
    )

    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()

        val lower = trimmed.lowercase(Locale.ROOT)
        for (forbidden in FORBIDDEN_SCHEMES) {
            if (lower.startsWith("$forbidden:")) {
                return false
            }
        }

        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false
        }

        return try {
            val javaUri = URI(trimmed)
            val scheme = javaUri.scheme?.lowercase(Locale.ROOT)
            val host = javaUri.host
            SUPPORTED_SCHEMES.contains(scheme) && !host.isNullOrBlank()
        } catch (_: Exception) {
            try {
                val androidUri = Uri.parse(trimmed)
                val scheme = androidUri.scheme?.lowercase(Locale.ROOT)
                val host = androidUri.host
                SUPPORTED_SCHEMES.contains(scheme) && !host.isNullOrBlank()
            } catch (_: Exception) {
                false
            }
        }
    }

    fun hasDirectVideoExtension(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT).split("?").firstOrNull() ?: ""
        return SUPPORTED_EXTENSIONS.any { lower.endsWith(it) }
    }

    fun getExtensionFromUrl(url: String): String {
        val cleanPath = url.lowercase(Locale.ROOT).split("?").firstOrNull() ?: ""
        for (ext in SUPPORTED_EXTENSIONS) {
            if (cleanPath.endsWith(ext)) {
                return ext.removePrefix(".")
            }
        }
        return "mp4"
    }

    fun isSupportedMimeType(mimeType: String?): Boolean {
        if (mimeType.isNullOrBlank()) return false
        val clean = mimeType.split(";").first().trim().lowercase(Locale.ROOT)
        return SUPPORTED_MIME_TYPES.contains(clean) || clean.startsWith("video/")
    }

    fun sanitizeFilename(candidate: String, fallbackExtension: String = "mp4"): String {
        val sanitized = candidate
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(60)
            .trim('_')
        val finalBase = if (sanitized.isBlank()) "video_${System.currentTimeMillis()}" else sanitized
        return if (SUPPORTED_EXTENSIONS.any { finalBase.lowercase(Locale.ROOT).endsWith(it) }) {
            finalBase
        } else {
            "$finalBase.$fallbackExtension"
        }
    }
}
