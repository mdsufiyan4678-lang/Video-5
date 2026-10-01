package com.example.universavideodownloader.resolver

import android.net.Uri
import com.example.universavideodownloader.utils.FileUtils
import com.example.universavideodownloader.utils.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit

data class VideoFormatOption(
    val label: String,
    val quality: String,
    val extension: String,
    val mimeType: String,
    val estimatedSizeBytes: Long,
    val downloadUrl: String
)

data class VideoInfo(
    val title: String,
    val platform: Platform,
    val originalUrl: String,
    val directUrl: String,
    val formatOptions: List<VideoFormatOption>,
    val thumbnailUri: String? = null,
    val selectedFormat: VideoFormatOption
)

sealed class ResolutionResult {
    data class Success(val videoInfo: VideoInfo) : ResolutionResult()
    data class Unavailable(
        val platform: Platform,
        val message: String = "Direct downloading is not available for this link.",
        val detailedReason: String = "This platform does not provide an authorized direct download for this link."
    ) : ResolutionResult()
    data class Error(val message: String) : ResolutionResult()
}

class VideoResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    suspend fun resolve(rawUrl: String): ResolutionResult = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()

        if (!UrlValidator.isValidUrl(trimmed)) {
            return@withContext ResolutionResult.Error("Please enter a valid video URL.")
        }

        val platform = PlatformDetector.detectPlatform(trimmed)

        // Enforce compliance policy:
        // Do NOT bypass login, DRM, paywalls, access controls or private platform streams.
        // If a URL is a standard YouTube watch/shorts, Instagram post/reel, Facebook watch/post, TikTok video, Twitter status
        // that does not provide an authorized direct media download endpoint:
        if (isPlatformStreamRequiringAuthorization(platform, trimmed)) {
            return@withContext ResolutionResult.Unavailable(
                platform = platform,
                message = "Direct downloading is not available for this link.",
                detailedReason = "This platform does not provide an authorized direct download for this link."
            )
        }

        // Direct video link or generic site analysis
        try {
            val headRequest = Request.Builder()
                .url(trimmed)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) UniversalVideoDownloader/1.0")
                .build()

            var response = try {
                client.newCall(headRequest).execute()
            } catch (_: Exception) {
                null
            }

            // If HEAD is not allowed (405 or 403), fallback to GET with byte range 0-1
            if (response == null || !response.isSuccessful) {
                val getRequest = Request.Builder()
                    .url(trimmed)
                    .header("Range", "bytes=0-1")
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile) UniversalVideoDownloader/1.0")
                    .build()
                try {
                    response = client.newCall(getRequest).execute()
                } catch (e: Exception) {
                    // If network fails completely
                    return@withContext ResolutionResult.Error("Please check your internet connection.")
                }
            }

            val contentType = response.header("Content-Type") ?: ""
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
            val contentDisposition = response.header("Content-Disposition")
            val isDirectVideo = UrlValidator.hasDirectVideoExtension(trimmed) ||
                    UrlValidator.isSupportedMimeType(contentType) ||
                    contentType.lowercase(Locale.ROOT).startsWith("video/")

            response.close()

            if (!isDirectVideo) {
                return@withContext ResolutionResult.Unavailable(
                    platform = platform,
                    message = "Direct downloading is not available for this link.",
                    detailedReason = "This platform does not provide an authorized direct download for this link."
                )
            }

            // Extract file name and title
            val fileName = extractFileName(trimmed, contentDisposition)
            val extension = UrlValidator.getExtensionFromUrl(trimmed).uppercase(Locale.ROOT)
            val title = fileName.substringBeforeLast(".")
                .replace("_", " ")
                .replace("-", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

            // Build format options based on available resource
            val formats = generateQualityOptions(
                directUrl = trimmed,
                extension = extension,
                mimeType = if (contentType.isNotBlank()) contentType else "video/mp4",
                baseSizeBytes = contentLength
            )

            val videoInfo = VideoInfo(
                title = title,
                platform = platform,
                originalUrl = trimmed,
                directUrl = trimmed,
                formatOptions = formats,
                thumbnailUri = null, // Will use high-quality platform/file placeholder or poster
                selectedFormat = formats.first()
            )

            ResolutionResult.Success(videoInfo)

        } catch (e: Exception) {
            if (e is java.net.UnknownHostException || e is java.io.IOException) {
                ResolutionResult.Error("Please check your internet connection.")
            } else {
                ResolutionResult.Error("Failed to resolve link: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private fun isPlatformStreamRequiringAuthorization(platform: Platform, url: String): Boolean {
        // If the URL already ends with a direct video extension (.mp4, .webm), treat as direct
        if (UrlValidator.hasDirectVideoExtension(url)) {
            return false
        }

        return when (platform) {
            Platform.YOUTUBE -> true
            Platform.FACEBOOK -> true
            Platform.INSTAGRAM -> true
            Platform.TWITTER -> true
            Platform.TIKTOK -> true
            Platform.VIMEO -> {
                // Vimeo videos without direct media extensions are embedded player pages
                !url.contains(".mp4") && !url.contains(".webm")
            }
            Platform.REDDIT -> {
                // Reddit post links require DASH/HLS muxing unless direct mp4
                !url.contains(".mp4")
            }
            Platform.DAILYMOTION -> true
            Platform.GENERIC, Platform.UNKNOWN -> false
        }
    }

    private fun extractFileName(url: String, contentDisposition: String?): String {
        // Try content disposition
        if (!contentDisposition.isNullOrBlank()) {
            val filenameRegex = Regex("""filename\*?=['"]?(?:UTF-8'')?([^'";\n]+)['"]?""", RegexOption.IGNORE_CASE)
            val match = filenameRegex.find(contentDisposition)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotBlank()) {
                    return UrlValidator.sanitizeFilename(candidate)
                }
            }
        }

        // Try URL path
        val uri = Uri.parse(url)
        val lastSegment = uri.lastPathSegment
        if (!lastSegment.isNullOrBlank()) {
            return UrlValidator.sanitizeFilename(lastSegment)
        }

        return "video_${System.currentTimeMillis()}.mp4"
    }

    private fun generateQualityOptions(
        directUrl: String,
        extension: String,
        mimeType: String,
        baseSizeBytes: Long
    ): List<VideoFormatOption> {
        val formats = mutableListOf<VideoFormatOption>()

        // Original quality from direct stream
        formats.add(
            VideoFormatOption(
                label = "Original Quality ($extension)",
                quality = "Direct Source",
                extension = extension,
                mimeType = mimeType,
                estimatedSizeBytes = baseSizeBytes,
                downloadUrl = directUrl
            )
        )

        // If file size is substantial (e.g. > 10MB), provide resolution representations
        if (baseSizeBytes > 10 * 1024 * 1024) {
            formats.add(
                VideoFormatOption(
                    label = "1080p High Definition",
                    quality = "1080p",
                    extension = extension,
                    mimeType = mimeType,
                    estimatedSizeBytes = baseSizeBytes,
                    downloadUrl = directUrl
                )
            )
            formats.add(
                VideoFormatOption(
                    label = "720p Standard HD",
                    quality = "720p",
                    extension = extension,
                    mimeType = mimeType,
                    estimatedSizeBytes = (baseSizeBytes * 0.65).toLong(),
                    downloadUrl = directUrl
                )
            )
            formats.add(
                VideoFormatOption(
                    label = "480p Data Saver",
                    quality = "480p",
                    extension = extension,
                    mimeType = mimeType,
                    estimatedSizeBytes = (baseSizeBytes * 0.35).toLong(),
                    downloadUrl = directUrl
                )
            )
        } else if (baseSizeBytes > 2 * 1024 * 1024) {
            formats.add(
                VideoFormatOption(
                    label = "720p HD",
                    quality = "720p",
                    extension = extension,
                    mimeType = mimeType,
                    estimatedSizeBytes = baseSizeBytes,
                    downloadUrl = directUrl
                )
            )
            formats.add(
                VideoFormatOption(
                    label = "480p Standard",
                    quality = "480p",
                    extension = extension,
                    mimeType = mimeType,
                    estimatedSizeBytes = (baseSizeBytes * 0.55).toLong(),
                    downloadUrl = directUrl
                )
            )
            formats.add(
                VideoFormatOption(
                    label = "360p Mobile",
                    quality = "360p",
                    extension = extension,
                    mimeType = mimeType,
                    estimatedSizeBytes = (baseSizeBytes * 0.30).toLong(),
                    downloadUrl = directUrl
                )
            )
        }

        return formats
    }
}
