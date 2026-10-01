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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import com.example.universavideodownloader.ui.AboutDialog
import com.example.universavideodownloader.ui.SettingsScreen
import com.example.universavideodownloader.utils.PreferenceManager

class SettingsActivity : ComponentActivity() {

    private val app by lazy { application as VideoDownloaderApplication }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by app.preferenceManager.themeMode.collectAsStateWithLifecycle()
            val isPremium by app.preferenceManager.isPremium.collectAsStateWithLifecycle()
            val wifiOnly by app.preferenceManager.wifiOnly.collectAsStateWithLifecycle()
            val notifications by app.preferenceManager.notificationsEnabled.collectAsStateWithLifecycle()

            val isDarkTheme = when (themeMode) {
                PreferenceManager.ThemeMode.DARK -> true
                PreferenceManager.ThemeMode.LIGHT -> false
                PreferenceManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            var showAboutDialog by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Settings") },
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
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChange = { app.preferenceManager.setThemeMode(it) },
                        isPremium = isPremium,
                        onBuyPremium = { app.billingManager.launchPurchaseFlow(this) },
                        onRestorePurchases = { app.billingManager.restorePurchase { } },
                        wifiOnly = wifiOnly,
                        onWifiOnlyChange = { app.preferenceManager.setWifiOnly(it) },
                        notificationsEnabled = notifications,
                        onNotificationsEnabledChange = { app.preferenceManager.setNotificationsEnabled(it) },
                        onAboutClick = { showAboutDialog = true },
                        modifier = Modifier.padding(innerPadding)
                    )

                    if (showAboutDialog) {
                        AboutDialog(onDismiss = { showAboutDialog = false })
                    }
                }
            }
        }
    }
}
