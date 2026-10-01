package com.example.universavideodownloader.resolver

import android.net.Uri
import com.example.universavideodownloader.utils.UrlValidator
import java.net.URI
import java.util.Locale

enum class Platform(
    val displayName: String,
    val iconKey: String,
    val requiresAuthorizedResource: Boolean,
    val note: String
) {
    YOUTUBE(
        displayName = "YouTube",
        iconKey = "youtube",
        requiresAuthorizedResource = true,
        note = "Platform protected streams. Direct download is only available if authorized direct link is provided."
    ),
    FACEBOOK(
        displayName = "Facebook",
        iconKey = "facebook",
        requiresAuthorizedResource = true,
        note = "Authentication/session protected. Only public direct video resources can be downloaded."
    ),
    INSTAGRAM(
        displayName = "Instagram",
        iconKey = "instagram",
        requiresAuthorizedResource = true,
        note = "Access-controlled media. Only publicly accessible direct MP4 resources are supported."
    ),
    TWITTER(
        displayName = "X / Twitter",
        iconKey = "twitter",
        requiresAuthorizedResource = true,
        note = "Platform streams. Requires direct authorized media URL."
    ),
    TIKTOK(
        displayName = "TikTok",
        iconKey = "tiktok",
        requiresAuthorizedResource = true,
        note = "Requires authorized public media link."
    ),
    VIMEO(
        displayName = "Vimeo",
        iconKey = "vimeo",
        requiresAuthorizedResource = true,
        note = "Supports public direct download URLs when granted by creator."
    ),
    REDDIT(
        displayName = "Reddit",
        iconKey = "reddit",
        requiresAuthorizedResource = true,
        note = "Supports direct public video URLs."
    ),
    DAILYMOTION(
        displayName = "Dailymotion",
        iconKey = "dailymotion",
        requiresAuthorizedResource = true,
        note = "Requires direct video resource stream."
    ),
    GENERIC(
        displayName = "Direct Link / Web",
        iconKey = "generic",
        requiresAuthorizedResource = false,
        note = "Direct downloadable MP4/WebM/MKV file."
    ),
    UNKNOWN(
        displayName = "Unknown Source",
        iconKey = "unknown",
        requiresAuthorizedResource = true,
        note = "Unrecognized link format."
    )
}

object PlatformDetector {

    fun detectPlatform(rawUrl: String?): Platform {
        if (rawUrl.isNullOrBlank() || !UrlValidator.isValidUrl(rawUrl)) {
            return Platform.UNKNOWN
        }

        val trimmed = rawUrl.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        val host = try {
            URI(trimmed).host?.lowercase(Locale.ROOT)
        } catch (_: Exception) {
            null
        } ?: try {
            Uri.parse(trimmed).host?.lowercase(Locale.ROOT)
        } catch (_: Exception) {
            null
        } ?: ""

        return when {
            // YouTube
            host.contains("youtube.com") || host.contains("youtu.be") || lower.contains("youtube.com/") || lower.contains("youtu.be/") -> Platform.YOUTUBE

            // Facebook
            host.contains("facebook.com") || host.contains("fb.watch") || host.contains("fb.com") -> Platform.FACEBOOK

            // Instagram
            host.contains("instagram.com") || host.contains("instagr.am") -> Platform.INSTAGRAM

            // Twitter / X
            host.contains("twitter.com") || host.contains("x.com") || host.contains("t.co") -> Platform.TWITTER

            // TikTok
            host.contains("tiktok.com") -> Platform.TIKTOK

            // Vimeo
            host.contains("vimeo.com") -> Platform.VIMEO

            // Reddit
            host.contains("reddit.com") || host.contains("redd.it") -> Platform.REDDIT

            // Dailymotion
            host.contains("dailymotion.com") || host.contains("dai.ly") -> Platform.DAILYMOTION

            // Direct video link or generic website
            UrlValidator.hasDirectVideoExtension(trimmed) -> Platform.GENERIC

            else -> Platform.GENERIC
        }
    }
}
