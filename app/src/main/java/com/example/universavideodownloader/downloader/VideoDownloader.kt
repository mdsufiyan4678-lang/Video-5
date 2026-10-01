package com.example.universavideodownloader.downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.universavideodownloader.resolver.VideoFormatOption
import com.example.universavideodownloader.resolver.VideoInfo
import com.example.universavideodownloader.utils.FileUtils
import com.example.universavideodownloader.utils.PreferenceManager
import com.example.universavideodownloader.utils.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoDownloader(
    private val context: Context,
    private val repository: DownloadRepository,
    private val monitor: DownloadMonitor,
    private val preferenceManager: PreferenceManager
) {
    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    suspend fun downloadVideo(
        videoInfo: VideoInfo,
        formatOption: VideoFormatOption
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val downloadUri = Uri.parse(formatOption.downloadUrl)

            // Safe filename with extension
            val rawFilename = "${videoInfo.title}_${formatOption.quality}"
            val sanitized = UrlValidator.sanitizeFilename(rawFilename, formatOption.extension.lowercase())

            val request = DownloadManager.Request(downloadUri).apply {
                setTitle(videoInfo.title)
                setDescription("Downloading ${formatOption.quality} • ${videoInfo.platform.displayName}")
                setMimeType(formatOption.mimeType)

                // Save to Downloads/UniversalVideoDownloader/
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "UniversalVideoDownloader/$sanitized"
                )

                // Notification visibility
                val showNotifications = preferenceManager.isNotificationsEnabled()
                if (showNotifications) {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                } else {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                }

                // Wi-Fi constraint
                if (preferenceManager.isWifiOnly()) {
                    setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                } else {
                    setAllowedNetworkTypes(
                        DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                    )
                }

                setAllowedOverRoaming(false)
            }

            val dmId = downloadManager.enqueue(request)

            val downloadItem = DownloadItem(
                downloadManagerId = dmId,
                title = videoInfo.title,
                originalUrl = videoInfo.originalUrl,
                downloadUrl = formatOption.downloadUrl,
                localFilePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/UniversalVideoDownloader/$sanitized",
                platform = videoInfo.platform.displayName,
                format = formatOption.extension.uppercase(),
                quality = formatOption.quality,
                totalBytes = formatOption.estimatedSizeBytes,
                downloadedBytes = 0L,
                progressPercent = 0,
                status = DownloadStatus.DOWNLOADING,
                thumbnailUri = videoInfo.thumbnailUri,
                createdAt = System.currentTimeMillis()
            )

            val dbId = repository.insert(downloadItem)
            monitor.startTracking(dmId)

            Result.success(dbId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelDownload(item: DownloadItem): Boolean = withContext(Dispatchers.IO) {
        try {
            if (item.downloadManagerId > 0) {
                downloadManager.remove(item.downloadManagerId)
                monitor.stopTracking(item.downloadManagerId)
            }
            repository.update(
                item.copy(
                    status = DownloadStatus.CANCELLED,
                    errorMessage = "Cancelled by user"
                )
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteDownload(item: DownloadItem, deleteFile: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            if (item.downloadManagerId > 0) {
                try {
                    downloadManager.remove(item.downloadManagerId)
                } catch (_: Exception) {}
                monitor.stopTracking(item.downloadManagerId)
            }

            if (deleteFile) {
                FileUtils.deletePhysicalFile(context, item.localFilePath, item.localUriString)
            }

            repository.delete(item)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun queryDownload(dmId: Long): DownloadProgressInfo? {
        return monitor.queryDownload(dmId)
    }

    suspend fun getDownloadStatus(dbId: Long): DownloadStatus? = withContext(Dispatchers.IO) {
        repository.getDownloadById(dbId)?.status
    }
}
