package com.example.universavideodownloader.downloader

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status IN ('PREPARING', 'DOWNLOADING', 'PAUSED') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): DownloadItem?

    @Query("SELECT * FROM downloads WHERE downloadManagerId = :dmId LIMIT 1")
    suspend fun getDownloadByManagerId(dmId: Long): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadItem): Long

    @Update
    suspend fun update(item: DownloadItem)

    @Query("UPDATE downloads SET downloadedBytes = :downloaded, totalBytes = :total, progressPercent = :progress, status = :status WHERE downloadManagerId = :dmId")
    suspend fun updateProgressByManagerId(
        dmId: Long,
        downloaded: Long,
        total: Long,
        progress: Int,
        status: DownloadStatus
    )

    @Query("UPDATE downloads SET status = :status, localFilePath = :filePath, localUriString = :localUri, progressPercent = 100 WHERE downloadManagerId = :dmId")
    suspend fun markCompleted(dmId: Long, status: DownloadStatus, filePath: String?, localUri: String?)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE downloadManagerId = :dmId")
    suspend fun markFailed(dmId: Long, status: DownloadStatus, error: String?)

    @Delete
    suspend fun delete(item: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)
}
