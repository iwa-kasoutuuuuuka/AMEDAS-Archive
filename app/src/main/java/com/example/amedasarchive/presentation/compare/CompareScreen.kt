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
    val activePrefectures by viewModel.activePrefectures.collectAsState()
    val selectedPrefA by viewModel.selectedPrefectureA.collectAsState()
    val selectedPrefB by viewModel.selectedPrefectureB.collectAsState()
    val stationsA by viewModel.stationsA.collectAsState()
    val stationsB by viewModel.stationsB.collectAsState()
    val stationA by viewModel.selectedStationA.collectAsState()
    val stationB by viewModel.selectedStationB.collectAsState()
    val dataA by viewModel.dataListA.collectAsState()
    val dataB by viewModel.dataListB.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var prefExpandedA by remember { mutableStateOf(false) }
    var stationExpandedA by remember { mutableStateOf(false) }
    var prefExpandedB by remember { mutableStateOf(false) }
    var stationExpandedB by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.loadActiveData()
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
                Spacer(modifier = Modifier.height(16.dp))

                // 地点A (青) 選択セクション
                Text(
                    text = "🔵 比較地点 A (青色プロット)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3F51B5)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    // 都道府県A
                    ExposedDropdownMenuBox(
                        expanded = prefExpandedA,
                        onExpandedChange = { prefExpandedA = !prefExpandedA },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = selectedPrefA.ifEmpty { "選択肢なし" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("都道府県 A") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prefExpandedA) },
                            modifier = Modifier.menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = prefExpandedA,
                            onDismissRequest = { prefExpandedA = false }
                        ) {
                            activePrefectures.forEach { pref ->
                                DropdownMenuItem(
                                    text = { Text(pref) },
                                    onClick = {
                                        viewModel.selectPrefectureA(pref)
                                        prefExpandedA = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 観測所A
                    ExposedDropdownMenuBox(
                        expanded = stationExpandedA,
                        onExpandedChange = { stationExpandedA = !stationExpandedA },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        TextField(
                            value = stationA?.name ?: "選択肢なし",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("観測所 A") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpandedA) },
                            modifier = Modifier.menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = stationExpandedA,
                            onDismissRequest = { stationExpandedA = false }
                        ) {
                            stationsA.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        viewModel.selectStationA(s)
                                        stationExpandedA = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // 地点B (赤) 選択セクション
                Text(
                    text = "🔴 比較地点 B (赤色プロット)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5722)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    // 都道府県B
                    ExposedDropdownMenuBox(
                        expanded = prefExpandedB,
                        onExpandedChange = { prefExpandedB = !prefExpandedB },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = selectedPrefB.ifEmpty { "選択肢なし" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("都道府県 B") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prefExpandedB) },
                            modifier = Modifier.menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = prefExpandedB,
                            onDismissRequest = { prefExpandedB = false }
                        ) {
                            activePrefectures.forEach { pref ->
                                DropdownMenuItem(
                                    text = { Text(pref) },
                                    onClick = {
                                        viewModel.selectPrefectureB(pref)
                                        prefExpandedB = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 観測所B
                    ExposedDropdownMenuBox(
                        expanded = stationExpandedB,
                        onExpandedChange = { stationExpandedB = !stationExpandedB },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        TextField(
                            value = stationB?.name ?: "選択肢なし",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("観測所 B") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpandedB) },
                            modifier = Modifier.menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = stationExpandedB,
                            onDismissRequest = { stationExpandedB = false }
                        ) {
                            stationsB.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        viewModel.selectStationB(s)
                                        stationExpandedB = false
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
