package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.EnhanceScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.PixelBoostTheme
import com.example.ui.viewmodel.MainViewModel

enum class MainTab {
    ENHANCE,
    HISTORY,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val darkTheme = when (settings.darkMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            PixelBoostTheme(darkTheme = darkTheme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(MainTab.ENHANCE) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isProcessing by viewModel.isProcessing.collectAsState()
    val enhancementResult by viewModel.enhancementResult.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val serverFallbackPrompt by viewModel.serverFallbackPrompt.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isProcessing) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.ENHANCE,
                        onClick = { selectedTab = MainTab.ENHANCE },
                        icon = {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Enhance",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Enhance", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF041E28),
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.HISTORY,
                        onClick = { selectedTab = MainTab.HISTORY },
                        icon = {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "History",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("History", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF041E28),
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.SETTINGS,
                        onClick = { selectedTab = MainTab.SETTINGS },
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF041E28),
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (selectedTab) {
            MainTab.ENHANCE -> {
                when {
                    isProcessing -> {
                        ProcessingScreen(viewModel = viewModel, modifier = modifier)
                    }
                    enhancementResult != null -> {
                        ResultScreen(
                            viewModel = viewModel,
                            onEnhanceAnother = { viewModel.resetForNewEnhancement() },
                            modifier = modifier
                        )
                    }
                    selectedImageUri != null -> {
                        EnhanceScreen(
                            viewModel = viewModel,
                            onStartEnhance = { /* State updates automatically */ },
                            modifier = modifier
                        )
                    }
                    else -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onImageSelected = { /* selectedImageUri will trigger EnhanceScreen */ },
                            onNavigateToHistory = { selectedTab = MainTab.HISTORY },
                            onNavigateToSettings = { selectedTab = MainTab.SETTINGS },
                            modifier = modifier
                        )
                    }
                }
            }
            MainTab.HISTORY -> {
                HistoryScreen(
                    viewModel = viewModel,
                    onOpenProject = { selectedTab = MainTab.ENHANCE },
                    modifier = modifier
                )
            }
            MainTab.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = modifier
                )
            }
        }
    }

    // Server Fallback Dialog
    if (serverFallbackPrompt != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFallbackPrompt() },
            title = {
                Text(
                    text = "AI Server Unavailable",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Could not connect to the remote AI super-resolution server:\n\n${serverFallbackPrompt}\n\nWould you like to process this image locally on your device with neural upscaling?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startEnhancement(forceLocalFallback = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color(0xFF041E28))
                ) {
                    Text("Use Basic Upscale", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissFallbackPrompt() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
