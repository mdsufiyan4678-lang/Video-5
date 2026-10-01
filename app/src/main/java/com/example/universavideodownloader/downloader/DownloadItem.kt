package com.example.universavideodownloader.downloader

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    PREPARING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val downloadManagerId: Long = -1L,
    val title: String,
    val originalUrl: String,
    val downloadUrl: String,
    val localFilePath: String? = null,
    val localUriString: String? = null,
    val platform: String,
    val format: String = "MP4",
    val quality: String = "720p",
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progressPercent: Int = 0,
    val status: DownloadStatus = DownloadStatus.PREPARING,
    val thumbnailUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
