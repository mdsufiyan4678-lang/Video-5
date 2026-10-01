package com.example.universavideodownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.MyApplicationTheme
import com.example.universavideodownloader.ui.DownloadsScreen
import com.example.universavideodownloader.utils.PreferenceManager
import kotlinx.coroutines.launch

class DownloadsActivity : ComponentActivity() {

    private val app by lazy { application as VideoDownloaderApplication }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by app.preferenceManager.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                PreferenceManager.ThemeMode.DARK -> true
                PreferenceManager.ThemeMode.LIGHT -> false
                PreferenceManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val downloads by app.repository.allDownloads.collectAsStateWithLifecycle(initialValue = emptyList())
            val isPremium by app.preferenceManager.isPremium.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Downloads") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    DownloadsScreen(
                        downloads = downloads,
                        onOpenClick = { item ->
                            com.example.universavideodownloader.utils.FileUtils.openVideo(
                                this@DownloadsActivity,
                                item.localFilePath,
                                item.localUriString,
                                item.title
                            )
                        },
                        onShareClick = { item ->
                            com.example.universavideodownloader.utils.FileUtils.shareVideo(
                                this@DownloadsActivity,
                                item.localFilePath,
                                item.localUriString,
                                item.title
                            )
                        },
                        onDeleteClick = { item ->
                            lifecycleScope.launch {
                                app.videoDownloader.deleteDownload(item, deleteFile = true)
                            }
                        },
                        onCancelClick = { item ->
                            lifecycleScope.launch {
                                app.videoDownloader.cancelDownload(item)
                            }
                        },
                        isPremium = isPremium,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
