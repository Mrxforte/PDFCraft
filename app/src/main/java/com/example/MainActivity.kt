package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.core.database.HistoryEntity
import com.example.core.database.SmallpdfDatabase
import com.example.core.model.PdfToolType
import com.example.ui.components.SmallpdfBottomBar
import com.example.ui.screens.*
import com.example.ui.theme.SmallpdfTheme
import java.io.File

sealed class ScreenDestination {
    object Home : ScreenDestination()
    object AllTools : ScreenDestination()
    object History : ScreenDestination()
    object Settings : ScreenDestination()
    data class Tool(val toolType: PdfToolType) : ScreenDestination()
    data class Viewer(val file: File) : ScreenDestination()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by remember { mutableStateOf(false) }

            SmallpdfTheme(darkTheme = darkTheme) {
                val context = LocalContext.current
                val dao = remember { SmallpdfDatabase.getInstance(context).historyDao() }
                val recentHistory by dao.getRecentHistory(5).collectAsState(initial = emptyList())

                var currentScreen by remember { mutableStateOf<ScreenDestination>(ScreenDestination.Home) }

                // Handle system back navigation
                BackHandler(enabled = currentScreen !is ScreenDestination.Home) {
                    currentScreen = ScreenDestination.Home
                }

                val currentRoute = when (currentScreen) {
                    ScreenDestination.Home -> "home"
                    ScreenDestination.AllTools -> "tools"
                    ScreenDestination.History -> "history"
                    ScreenDestination.Settings -> "settings"
                    else -> ""
                }

                val showBottomBar = currentScreen in listOf(
                    ScreenDestination.Home,
                    ScreenDestination.AllTools,
                    ScreenDestination.History,
                    ScreenDestination.Settings
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            SmallpdfBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    currentScreen = when (route) {
                                        "home" -> ScreenDestination.Home
                                        "tools" -> ScreenDestination.AllTools
                                        "history" -> ScreenDestination.History
                                        "settings" -> ScreenDestination.Settings
                                        else -> ScreenDestination.Home
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    val screenModifier = Modifier.padding(innerPadding)

                    when (val screen = currentScreen) {
                        ScreenDestination.Home -> {
                            HomeScreen(
                                onSelectTool = { tool ->
                                    currentScreen = ScreenDestination.Tool(tool)
                                },
                                onOpenHistoryItem = { item ->
                                    val f = File(item.filePath)
                                    if (f.exists()) {
                                        currentScreen = ScreenDestination.Viewer(f)
                                    }
                                },
                                onOpenPdf = { file ->
                                    currentScreen = ScreenDestination.Viewer(file)
                                },
                                recentHistory = recentHistory,
                                modifier = screenModifier
                            )
                        }

                        ScreenDestination.AllTools -> {
                            AllToolsScreen(
                                onSelectTool = { tool ->
                                    currentScreen = ScreenDestination.Tool(tool)
                                },
                                modifier = screenModifier
                            )
                        }

                        ScreenDestination.History -> {
                            HistoryScreen(
                                onViewPdf = { file ->
                                    currentScreen = ScreenDestination.Viewer(file)
                                },
                                modifier = screenModifier
                            )
                        }

                        ScreenDestination.Settings -> {
                            SettingsScreen(
                                darkTheme = darkTheme,
                                onToggleDarkTheme = { darkTheme = it },
                                modifier = screenModifier
                            )
                        }

                        is ScreenDestination.Tool -> {
                            when (screen.toolType) {
                                PdfToolType.MERGE -> {
                                    MergeScreen(
                                        onNavigateBack = { currentScreen = ScreenDestination.Home },
                                        onViewPdf = { file -> currentScreen = ScreenDestination.Viewer(file) },
                                        modifier = screenModifier
                                    )
                                }
                                PdfToolType.COMPRESS -> {
                                    CompressScreen(
                                        onNavigateBack = { currentScreen = ScreenDestination.Home },
                                        onViewPdf = { file -> currentScreen = ScreenDestination.Viewer(file) },
                                        modifier = screenModifier
                                    )
                                }
                                PdfToolType.WATERMARK_DELETE -> {
                                    WatermarkDeleterScreen(
                                        onNavigateBack = { currentScreen = ScreenDestination.Home },
                                        onViewPdf = { file -> currentScreen = ScreenDestination.Viewer(file) },
                                        modifier = screenModifier
                                    )
                                }
                                PdfToolType.DELETE_METADATA -> {
                                    MetadataCleanerScreen(
                                        onNavigateBack = { currentScreen = ScreenDestination.Home },
                                        onViewPdf = { file -> currentScreen = ScreenDestination.Viewer(file) },
                                        modifier = screenModifier
                                    )
                                }
                                PdfToolType.OCR -> {
                                    OcrScreen(
                                        onNavigateBack = { currentScreen = ScreenDestination.Home },
                                        modifier = screenModifier
                                    )
                                }
                                PdfToolType.SPLIT -> {
                                    SplitScreen(
                                        onNavigateBack = { currentScreen = ScreenDestination.Home },
                                        onViewPdf = { file -> currentScreen = ScreenDestination.Viewer(file) },
                                        modifier = screenModifier
                                    )
                                }
                                PdfToolType.SIGN -> {
                                    SignScreen(
                                        onNavigateBack = { currentScreen = ScreenDestination.Home },
                                        onViewPdf = { file -> currentScreen = ScreenDestination.Viewer(file) },
                                        modifier = screenModifier
                                    )
                                }
                                else -> {
                                    ToolWorkflowScreen(
                                        tool = screen.toolType,
                                        onNavigateBack = { currentScreen = ScreenDestination.Home },
                                        onViewPdf = { file -> currentScreen = ScreenDestination.Viewer(file) },
                                        modifier = screenModifier
                                    )
                                }
                            }
                        }

                        is ScreenDestination.Viewer -> {
                            PdfViewerScreen(
                                pdfFile = screen.file,
                                onNavigateBack = { currentScreen = ScreenDestination.Home },
                                modifier = screenModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
