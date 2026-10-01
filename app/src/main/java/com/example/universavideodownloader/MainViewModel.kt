package com.example.universavideodownloader

import android.app.Activity
import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.universavideodownloader.downloader.DownloadItem
import com.example.universavideodownloader.downloader.DownloadStatus
import com.example.universavideodownloader.resolver.Platform
import com.example.universavideodownloader.resolver.ResolutionResult
import com.example.universavideodownloader.resolver.VideoFormatOption
import com.example.universavideodownloader.utils.FileUtils
import com.example.universavideodownloader.utils.PreferenceManager
import com.example.universavideodownloader.utils.UrlValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VideoDownloaderApplication
    private val repository = app.repository
    private val resolver = app.videoResolver
    private val downloader = app.videoDownloader
    private val billingManager = app.billingManager
    private val preferenceManager = app.preferenceManager

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _resolutionResult = MutableStateFlow<ResolutionResult?>(null)
    val resolutionResult: StateFlow<ResolutionResult?> = _resolutionResult.asStateFlow()

    private val _selectedFormat = MutableStateFlow<VideoFormatOption?>(null)
    val selectedFormat: StateFlow<VideoFormatOption?> = _selectedFormat.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<List<DownloadItem>> = repository.activeDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPremium: StateFlow<Boolean> = preferenceManager.isPremium
    val themeMode: StateFlow<PreferenceManager.ThemeMode> = preferenceManager.themeMode
    val wifiOnly: StateFlow<Boolean> = preferenceManager.wifiOnly
    val notificationsEnabled: StateFlow<Boolean> = preferenceManager.notificationsEnabled

    fun setUrlInput(url: String) {
        _urlInput.value = url
        // Reset analysis when input changes significantly
        if (_resolutionResult.value != null) {
            _resolutionResult.value = null
            _selectedFormat.value = null
        }
    }

    fun pasteFromClipboard() {
        try {
            val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pasted = clip.getItemAt(0).coerceToText(app).toString().trim()
                if (pasted.isNotBlank()) {
                    setUrlInput(pasted)
                    Toast.makeText(app, "Link pasted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(app, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(app, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(app, "Could not access clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearUrl() {
        _urlInput.value = ""
        _resolutionResult.value = null
        _selectedFormat.value = null
    }

    fun analyzeUrl() {
        val currentUrl = _urlInput.value.trim()
        if (currentUrl.isBlank()) {
            Toast.makeText(app, "Please enter a valid video URL.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!UrlValidator.isValidUrl(currentUrl)) {
            _resolutionResult.value = ResolutionResult.Error("Please enter a valid video URL.")
            return
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            _resolutionResult.value = null
            _selectedFormat.value = null

            val result = resolver.resolve(currentUrl)
            _resolutionResult.value = result

            if (result is ResolutionResult.Success) {
                _selectedFormat.value = result.videoInfo.selectedFormat
            }
            _isAnalyzing.value = false
        }
    }

    fun selectFormat(format: VideoFormatOption) {
        _selectedFormat.value = format
    }

    fun startDownload(onStarted: () -> Unit = {}) {
        val currentRes = _resolutionResult.value
        val format = _selectedFormat.value
        if (currentRes !is ResolutionResult.Success || format == null) {
            Toast.makeText(app, "Please analyze a valid video link first", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _isDownloading.value = true
            val result = downloader.downloadVideo(currentRes.videoInfo, format)
            if (result.isSuccess) {
                Toast.makeText(app, "Download started! Tracking in Downloads.", Toast.LENGTH_SHORT).show()
                onStarted()
            } else {
                Toast.makeText(
                    app,
                    "Download failed: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
            _isDownloading.value = false
        }
    }

    fun cancelDownload(item: DownloadItem) {
        viewModelScope.launch {
            downloader.cancelDownload(item)
            Toast.makeText(app, "Download cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteDownload(item: DownloadItem, deleteFile: Boolean = true) {
        viewModelScope.launch {
            downloader.deleteDownload(item, deleteFile)
            Toast.makeText(app, "Download removed", Toast.LENGTH_SHORT).show()
        }
    }

    fun openDownload(context: Context, item: DownloadItem) {
        FileUtils.openVideo(context, item.localFilePath, item.localUriString, item.title)
    }

    fun shareDownload(context: Context, item: DownloadItem) {
        FileUtils.shareVideo(context, item.localFilePath, item.localUriString, item.title)
    }

    fun buyPremium(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    fun restorePurchases() {
        billingManager.restorePurchase { /* result handled in manager */ }
    }

    fun setThemeMode(mode: PreferenceManager.ThemeMode) {
        preferenceManager.setThemeMode(mode)
    }

    fun setWifiOnly(enabled: Boolean) {
        preferenceManager.setWifiOnly(enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferenceManager.setNotificationsEnabled(enabled)
    }
}
