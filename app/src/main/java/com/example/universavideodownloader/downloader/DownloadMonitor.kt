package com.example.universavideodownloader.downloader

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import com.example.universavideodownloader.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class DownloadProgressInfo(
    val downloadId: Long,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percentage: Int,
    val status: DownloadStatus,
    val localUri: String? = null,
    val reasonMessage: String? = null
)

class DownloadMonitor(
    private val context: Context,
    private val repository: DownloadRepository,
    private val scope: CoroutineScope
) {
    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private var monitoringJob: Job? = null
    private val trackedIds = mutableSetOf<Long>()

    fun startTracking(dmId: Long) {
        synchronized(trackedIds) {
            trackedIds.add(dmId)
        }
        ensureMonitoringRunning()
    }

    fun stopTracking(dmId: Long) {
        synchronized(trackedIds) {
            trackedIds.remove(dmId)
        }
    }

    private fun ensureMonitoringRunning() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentIds = synchronized(trackedIds) { trackedIds.toSet() }
                if (currentIds.isEmpty()) {
                    delay(1000)
                    continue
                }

                for (dmId in currentIds) {
                    val progressInfo = queryDownload(dmId)
                    if (progressInfo != null) {
                        repository.updateProgress(
                            dmId = dmId,
                            downloaded = progressInfo.downloadedBytes,
                            total = progressInfo.totalBytes,
                            progress = progressInfo.percentage,
                            status = progressInfo.status
                        )

                        if (progressInfo.status == DownloadStatus.COMPLETED) {
                            var filePath: String? = null
                            if (progressInfo.localUri != null) {
                                val uri = Uri.parse(progressInfo.localUri)
                                filePath = if (uri.scheme == "file") uri.path else null
                            }
                            if (filePath != null) {
                                try {
                                    MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(filePath),
                                        arrayOf(FileUtils.getMimeType(filePath)),
                                        null
                                    )
                                } catch (_: Exception) {}
                            }
                            repository.markCompleted(dmId, filePath, progressInfo.localUri)
                            stopTracking(dmId)
                        } else if (progressInfo.status == DownloadStatus.FAILED ||
                            progressInfo.status == DownloadStatus.CANCELLED
                        ) {
                            repository.markFailed(dmId, progressInfo.reasonMessage)
                            stopTracking(dmId)
                        }
                    } else {
                        // Download not found in DM (e.g. removed or cancelled)
                        repository.markFailed(dmId, "Download removed")
                        stopTracking(dmId)
                    }
                }

                delay(600)
            }
        }
    }

    fun queryDownload(downloadId: Long): DownloadProgressInfo? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        var cursor: Cursor? = null
        try {
            cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                val bytesDownloaded = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                val totalBytes = if (totalBytesIndex >= 0) cursor.getLong(totalBytesIndex) else 0L
                val dmStatus = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                val localUri = if (localUriIndex >= 0) cursor.getString(localUriIndex) else null
                val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else 0

                val percentage = if (totalBytes > 0) {
                    ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                } else 0

                val status = when (dmStatus) {
                    DownloadManager.STATUS_PENDING -> DownloadStatus.PREPARING
                    DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                    DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                    DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                    else -> DownloadStatus.DOWNLOADING
                }

                val reasonMessage = if (dmStatus == DownloadManager.STATUS_FAILED) {
                    "Error code $reason"
                } else null

                return DownloadProgressInfo(
                    downloadId = downloadId,
                    downloadedBytes = bytesDownloaded,
                    totalBytes = totalBytes,
                    percentage = percentage,
                    status = status,
                    localUri = localUri,
                    reasonMessage = reasonMessage
                )
            }
        } catch (_: Exception) {
            return null
        } finally {
            cursor?.close()
        }
        return null
    }

    fun stopAll() {
        monitoringJob?.cancel()
        synchronized(trackedIds) {
            trackedIds.clear()
        }
    }
}
