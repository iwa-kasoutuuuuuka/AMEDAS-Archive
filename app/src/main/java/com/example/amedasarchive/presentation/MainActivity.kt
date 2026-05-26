package com.example.amedasarchive.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.amedasarchive.data.local.AppDatabase
import com.example.amedasarchive.data.repository.WeatherRepositoryImpl
import com.example.amedasarchive.domain.usecase.AnalyzeSingularityUseCase
import com.example.amedasarchive.domain.usecase.CalculateStatsUseCase
import com.example.amedasarchive.domain.usecase.ManageStorageUseCase
import com.example.amedasarchive.presentation.compare.CompareScreen
import com.example.amedasarchive.presentation.compare.CompareViewModel
import com.example.amedasarchive.presentation.home.HomeScreen
import com.example.amedasarchive.presentation.home.HomeViewModel
import com.example.amedasarchive.presentation.manage.ManageScreen
import com.example.amedasarchive.presentation.manage.ManageViewModel
import com.example.amedasarchive.presentation.singularity.SingularityScreen
import com.example.amedasarchive.presentation.singularity.SingularityViewModel
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.amedasarchive.domain.model.Station

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hiltなしでも即ビルド・起動できるよう、RoomDBと各レイヤーインスタンスを手動初期化 (ポータビリティ設計)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = WeatherRepositoryImpl(
            database.weatherDao(),
            database.stationDao(),
            database.syncLogDao()
        )

        // 初回起動時の全国アメダス完全マスタCSV (stations.csv) インポート処理 (DBシード)
        lifecycleScope.launch {
            try {
                val current = repository.getAllStations()
                if (current.isEmpty()) {
                    val stationsList = mutableListOf<Station>()
                    assets.open("stations.csv").bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            val tokens = line.split(",")
                            if (tokens.size >= 8) {
                                val station = Station(
                                    stationId = tokens[0].trim(),
                                    name = tokens[1].trim(),
                                    kana = tokens[2].trim(),
                                    prefecture = tokens[3].trim(),
                                    latitude = tokens[4].trim().toDoubleOrNull() ?: 0.0,
                                    longitude = tokens[5].trim().toDoubleOrNull() ?: 0.0,
                                    elevation = tokens[6].trim().toDoubleOrNull() ?: 0.0,
                                    stationType = tokens[7].trim()
                                )
                                stationsList.add(station)
                            }
                        }
                    }
                    if (stationsList.isNotEmpty()) {
                        repository.insertStations(stationsList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // UseCase群の生成
        val calculateStatsUseCase = CalculateStatsUseCase(repository)
        val analyzeSingularityUseCase = AnalyzeSingularityUseCase(repository)
        val manageStorageUseCase = ManageStorageUseCase(repository)

        // ViewModel群を手動ファクトリ形式でインスタンス化
        val homeViewModel = HomeViewModel(repository, calculateStatsUseCase)
        val compareViewModel = CompareViewModel(repository)
        val singularityViewModel = SingularityViewModel(repository, analyzeSingularityUseCase)
        val manageViewModel = ManageViewModel(applicationContext, repository, manageStorageUseCase)

        setContent {
            MaterialTheme {
                MainAppScreen(
                    homeViewModel = homeViewModel,
                    compareViewModel = compareViewModel,
                    singularityViewModel = singularityViewModel,
                    manageViewModel = manageViewModel
                )
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Home : Screen("home", "単一検索", "🏠")
    object Compare : Screen("compare", "地域比較", "📊")
    object Singularity : Screen("singularity", "特異日", "☀")
    object Manage : Screen("manage", "同期管理", "⚙")
}

@Composable
fun MainAppScreen(
    homeViewModel: HomeViewModel,
    compareViewModel: CompareViewModel,
    singularityViewModel: SingularityViewModel,
    manageViewModel: ManageViewModel
) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Compare,
        Screen.Singularity,
        Screen.Manage
    )

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Text(screen.icon, fontSize = 20.sp) },
                        label = { Text(screen.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = currentScreen.route == screen.route,
                        onClick = {
                            currentScreen = screen
                            navController.navigate(screen.route) {
                                // ナビゲーションスタックが肥大化しないように制御
                                popUpTo(navController.graph.startDestinationId) {
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
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = homeViewModel)
            }
            composable(Screen.Compare.route) {
                CompareScreen(viewModel = compareViewModel)
            }
            composable(Screen.Singularity.route) {
                SingularityScreen(viewModel = singularityViewModel)
            }
            composable(Screen.Manage.route) {
                ManageScreen(viewModel = manageViewModel)
            }
        }
    }
}
