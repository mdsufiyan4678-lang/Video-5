package com.example.universavideodownloader.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DownloadRepository(private val dao: DownloadDao) {

    val allDownloads: Flow<List<DownloadItem>> = dao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadItem>> = dao.getActiveDownloads()

    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>> =
        dao.getDownloadsByStatus(status)

    suspend fun getDownloadById(id: Long): DownloadItem? = withContext(Dispatchers.IO) {
        dao.getDownloadById(id)
    }

    suspend fun getDownloadByManagerId(dmId: Long): DownloadItem? = withContext(Dispatchers.IO) {
        dao.getDownloadByManagerId(dmId)
    }

    suspend fun insert(item: DownloadItem): Long = withContext(Dispatchers.IO) {
        dao.insert(item)
    }

    suspend fun update(item: DownloadItem) = withContext(Dispatchers.IO) {
        dao.update(item)
    }

    suspend fun updateProgress(
        dmId: Long,
        downloaded: Long,
        total: Long,
        progress: Int,
        status: DownloadStatus
    ) = withContext(Dispatchers.IO) {
        dao.updateProgressByManagerId(dmId, downloaded, total, progress, status)
    }

    suspend fun markCompleted(dmId: Long, filePath: String?, localUri: String?) =
        withContext(Dispatchers.IO) {
            dao.markCompleted(dmId, DownloadStatus.COMPLETED, filePath, localUri)
        }

    suspend fun markFailed(dmId: Long, error: String?) = withContext(Dispatchers.IO) {
        dao.markFailed(dmId, DownloadStatus.FAILED, error)
    }

    suspend fun delete(item: DownloadItem) = withContext(Dispatchers.IO) {
        dao.delete(item)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }
}
