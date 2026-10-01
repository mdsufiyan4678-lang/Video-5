package com.example.universavideodownloader

import android.app.Application
import com.example.universavideodownloader.billing.BillingManager
import com.example.universavideodownloader.downloader.AppDatabase
import com.example.universavideodownloader.downloader.DownloadMonitor
import com.example.universavideodownloader.downloader.DownloadRepository
import com.example.universavideodownloader.downloader.VideoDownloader
import com.example.universavideodownloader.resolver.VideoResolver
import com.example.universavideodownloader.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class VideoDownloaderApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { DownloadRepository(database.downloadDao()) }
    val preferenceManager by lazy { PreferenceManager.getInstance(this) }
    val downloadMonitor by lazy { DownloadMonitor(this, repository, applicationScope) }
    val videoDownloader by lazy { VideoDownloader(this, repository, downloadMonitor, preferenceManager) }
    val videoResolver by lazy { VideoResolver() }
    val billingManager by lazy { BillingManager(this, preferenceManager, applicationScope) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: VideoDownloaderApplication
            private set
    }
}
