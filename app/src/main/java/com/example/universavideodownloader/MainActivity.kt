package com.example.universavideodownloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import com.example.universavideodownloader.ui.AboutDialog
import com.example.universavideodownloader.ui.DownloadsScreen
import com.example.universavideodownloader.ui.HomeScreen
import com.example.universavideodownloader.ui.SettingsScreen
import com.example.universavideodownloader.utils.PreferenceManager

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle shared URL from other apps (e.g. browser or share sheet)
        handleIncomingIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                PreferenceManager.ThemeMode.DARK -> true
                PreferenceManager.ThemeMode.LIGHT -> false
                PreferenceManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainAppContainer(
                    viewModel = viewModel,
                    activity = this
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.setUrlInput(sharedText.trim())
                viewModel.analyzeUrl()
            }
        }
    }
}

enum class NavigationTab(val label: String) {
    HOME("Home"),
    DOWNLOADS("Downloads"),
    SETTINGS("Settings")
}

@Composable
fun MainAppContainer(
    viewModel: MainViewModel,
    activity: ComponentActivity
) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val resolutionResult by viewModel.resolutionResult.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val wifiOnly by viewModel.wifiOnly.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()

    // Handle back button on sub-tabs
    BackHandler(enabled = currentTab != NavigationTab.HOME) {
        currentTab = NavigationTab.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav")) {
                NavigationBarItem(
                    selected = currentTab == NavigationTab.HOME,
                    onClick = { currentTab = NavigationTab.HOME },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavigationTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_home")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.DOWNLOADS,
                    onClick = { currentTab = NavigationTab.DOWNLOADS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeDownloads.isNotEmpty()) {
                                    Badge { Text("${activeDownloads.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == NavigationTab.DOWNLOADS) Icons.Filled.Download else Icons.Outlined.Download,
                                contentDescription = "Downloads"
                            )
                        }
                    },
                    label = { Text("Downloads") },
                    modifier = Modifier.testTag("nav_downloads")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.SETTINGS,
                    onClick = { currentTab = NavigationTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavigationTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            NavigationTab.HOME -> {
                HomeScreen(
                    urlInput = urlInput,
                    onUrlChange = { viewModel.setUrlInput(it) },
                    onPasteClick = { viewModel.pasteFromClipboard() },
                    onClearClick = { viewModel.clearUrl() },
                    onAnalyzeClick = { viewModel.analyzeUrl() },
                    isAnalyzing = isAnalyzing,
                    resolutionResult = resolutionResult,
                    selectedFormat = selectedFormat,
                    onSelectFormat = { viewModel.selectFormat(it) },
                    isDownloading = isDownloading,
                    onStartDownload = {
                        viewModel.startDownload(onStarted = {
                            currentTab = NavigationTab.DOWNLOADS
                        })
                    },
                    activeDownloads = activeDownloads,
                    onCancelActiveDownload = { viewModel.cancelDownload(it) },
                    isPremium = isPremium,
                    onNavigateToDownloads = { currentTab = NavigationTab.DOWNLOADS },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            NavigationTab.DOWNLOADS -> {
                DownloadsScreen(
                    downloads = allDownloads,
                    onOpenClick = { viewModel.openDownload(activity, it) },
                    onShareClick = { viewModel.shareDownload(activity, it) },
                    onDeleteClick = { viewModel.deleteDownload(it) },
                    onCancelClick = { viewModel.cancelDownload(it) },
                    isPremium = isPremium,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            NavigationTab.SETTINGS -> {
                SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    isPremium = isPremium,
                    onBuyPremium = { viewModel.buyPremium(activity) },
                    onRestorePurchases = { viewModel.restorePurchases() },
                    wifiOnly = wifiOnly,
                    onWifiOnlyChange = { viewModel.setWifiOnly(it) },
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsEnabledChange = { viewModel.setNotificationsEnabled(it) },
                    onAboutClick = { showAboutDialog = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}
