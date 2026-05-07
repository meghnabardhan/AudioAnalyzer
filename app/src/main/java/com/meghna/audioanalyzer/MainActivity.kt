package com.meghna.audioanalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.meghna.audioanalyzer.AudioViewModel
import com.meghna.audioanalyzer.screens.FFTVisualizerScreen
import com.meghna.audioanalyzer.screens.FocusStateScreen
import com.meghna.audioanalyzer.screens.RoutingGraphScreen
import com.meghna.audioanalyzer.screens.StreamDashboardScreen
import com.meghna.audioanalyzer.ui.theme.AudioAnalyzerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioAnalyzerTheme {
                val navController = rememberNavController()
		val viewModel: AudioViewModel = hiltViewModel()

                // Bottom navigation items
                val items = listOf(
                    Triple("dashboard", "Streams", Icons.Filled.MusicNote),
                    Triple("focus", "Focus", Icons.Filled.Analytics),
                    Triple("routing", "Routing", Icons.Filled.Router),
                    Triple("fft", "FFT", Icons.Filled.GraphicEq)
                )

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            items.forEach { (route, label, icon) ->
                                NavigationBarItem(
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    selected = currentRoute == route,
                                    onClick = {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") { StreamDashboardScreen(viewModel = viewModel) }
                        composable("focus") { FocusStateScreen(viewModel = viewModel) }
                        composable("routing") { RoutingGraphScreen(viewModel = viewModel) }
                        composable("fft") { FFTVisualizerScreen() }
                    }
                }
            }
        }
    }
}