package com.example.amedasarchive.presentation.compare

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
import com.example.amedasarchive.presentation.components.StatsChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: CompareViewModel,
    modifier: Modifier = Modifier
) {
    val stations by viewModel.stations.collectAsState()
    val stationA by viewModel.selectedStationA.collectAsState()
    val stationB by viewModel.selectedStationB.collectAsState()
    val dataA by viewModel.dataListA.collectAsState()
    val dataB by viewModel.dataListB.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var expandedA by remember { mutableStateOf(false) }
    var expandedB by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.loadStations()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
            .padding(bottom = 24.dp)
    ) {
        // ヘッダー
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF3F51B5),
                            Color(0xFFFF5722)
                        )
                    )
                )
                .padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "複数地域の重ね合わせ比較",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 地域選択エリア
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "比較する観測地点を指定",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // 地点A
                    ExposedDropdownMenuBox(
                        expanded = expandedA,
                        onExpandedChange = { expandedA = !expandedA },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = stationA?.name ?: "選択肢なし",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("地点 A (青)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedA) },
                            modifier = Modifier.menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedA,
                            onDismissRequest = { expandedA = false }
                        ) {
                            stations.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        viewModel.selectStationA(s)
                                        expandedA = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 地点B
                    ExposedDropdownMenuBox(
                        expanded = expandedB,
                        onExpandedChange = { expandedB = !expandedB },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = stationB?.name ?: "選択肢なし",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("地点 B (赤)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedB) },
                            modifier = Modifier.menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedB,
                            onDismissRequest = { expandedB = false }
                        ) {
                            stations.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        viewModel.selectStationB(s)
                                        expandedB = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 比較結果の視覚化 (グラフ)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (dataA.isNotEmpty() && dataB.isNotEmpty()) {
            StatsChart(
                dataA = dataA,
                dataB = dataB,
                labelA = stationA?.name ?: "地点A",
                labelB = stationB?.name ?: "地点B",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 統計対比値カード
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 各地点の気候代表値（期間平均）",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stationA?.name ?: "地点A",
                                color = Color(0xFF3F51B5),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("平均気温: ${String.format("%.1f", dataA.map { it.temperatureMean ?: 0.0 }.average())}℃")
                            Text("平均最高気温: ${String.format("%.1f", dataA.map { it.temperatureMax ?: 0.0 }.average())}℃")
                        }

                        Divider(
                            modifier = Modifier
                                .height(60.dp)
                                .width(1.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stationB?.name ?: "地点B",
                                color = Color(0xFFFF5722),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("平均気温: ${String.format("%.1f", dataB.map { it.temperatureMean ?: 0.0 }.average())}℃")
                            Text("平均最高気温: ${String.format("%.1f", dataB.map { it.temperatureMax ?: 0.0 }.average())}℃")
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "※十分な過去データがDBに蓄積されていません。同期管理画面から両地点のデータを同期してください。",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 免責バナー
        ComplianceBanner(modifier = Modifier.padding(top = 16.dp))
    }
}
