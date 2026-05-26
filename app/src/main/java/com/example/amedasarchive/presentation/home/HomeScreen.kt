package com.example.amedasarchive.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amedasarchive.presentation.components.ComplianceBanner
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val prefectures by viewModel.prefectures.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val selectedPref by viewModel.selectedPrefecture.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val statsResult by viewModel.statsResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dateRangeText by viewModel.dateRangeText.collectAsState()
    val validationError by viewModel.validationError.collectAsState()

    var monthText by remember { mutableStateOf("7") }
    var dayText by remember { mutableStateOf("7") }
    var yearText by remember { mutableStateOf(LocalDate.now().year.toString()) }

    var prefExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(bottom = 24.dp)
    ) {
        // ヘッダーセクション
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            Column {
                Text(
                    text = "AMEDAS 過去統計アーカイブ",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "民間API完全排除・ローカル算出型統計エンジン",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }

        // 観測所選択エリア
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. 地域・観測所（アメダス）選択",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // 都道府県ドロップダウン
                    ExposedDropdownMenuBox(
                        expanded = prefExpanded,
                        onExpandedChange = { prefExpanded = !prefExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = selectedPref,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("都道府県") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prefExpanded) },
                            modifier = Modifier.menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = prefExpanded,
                            onDismissRequest = { prefExpanded = false }
                        ) {
                            prefectures.forEach { pref ->
                                DropdownMenuItem(
                                    text = { Text(pref) },
                                    onClick = {
                                        viewModel.selectPrefecture(pref)
                                        prefExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 観測所ドロップダウン
                    ExposedDropdownMenuBox(
                        expanded = stationExpanded,
                        onExpandedChange = { stationExpanded = !stationExpanded },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        TextField(
                            value = selectedStation?.name ?: "選択肢なし",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("観測所") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                            modifier = Modifier.menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = stationExpanded,
                            onDismissRequest = { stationExpanded = false }
                        ) {
                            stations.forEach { station ->
                                DropdownMenuItem(
                                    text = { Text(station.name) },
                                    onClick = {
                                        viewModel.selectStation(station)
                                        stationExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // 蓄積データ期間の可視化
                Text(
                    text = dateRangeText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 統計日付と期間指定エリア
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. 統計日及び検証年指定",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextField(
                        value = yearText,
                        onValueChange = { yearText = it },
                        label = { Text("検証対象年") },
                        modifier = Modifier.weight(1.2f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = monthText,
                        onValueChange = { monthText = it },
                        label = { Text("月") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = dayText,
                        onValueChange = { dayText = it },
                        label = { Text("日") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // バリデーションエラーメッセージ
                validationError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        val m = monthText.toIntOrNull() ?: 7
                        val d = dayText.toIntOrNull() ?: 7
                        val y = yearText.toIntOrNull() ?: LocalDate.now().year
                        viewModel.calculateStats(m, d, y)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(text = "過去統計データを算出する", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 統計結果表示エリア
        statsResult?.let { stats ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 ${stats.stationName} - ${stats.targetDateOrDay}の過去${stats.totalYears}年間統計",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 晴れ確率
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "晴れの確率", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = "${String.format("%.1f", stats.sunProbability ?: 0.0)}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 降水確率
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "降水確率", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = "${String.format("%.1f", stats.rainProbability ?: 0.0)}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE91E63)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 気温平均
                    Text(
                        text = "気温統計平均",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "平均気温: ${String.format("%.1f", stats.temperatureMean ?: 0.0)}℃")
                        Text(text = "最高気温: ${String.format("%.1f", stats.temperatureMax ?: 0.0)}℃")
                        Text(text = "最低気温: ${String.format("%.1f", stats.temperatureMin ?: 0.0)}℃")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // 法的遵守事項＆免責バナーを下部に常時表示
        ComplianceBanner(modifier = Modifier.padding(top = 16.dp))
    }
}
