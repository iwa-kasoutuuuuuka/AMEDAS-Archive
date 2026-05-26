package com.example.amedasarchive.presentation.singularity

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
import androidx.compose.ui.draw.clip
import com.example.amedasarchive.domain.usecase.AnalyzeSingularityUseCase
import com.example.amedasarchive.presentation.components.ComplianceBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingularityScreen(
    viewModel: SingularityViewModel,
    modifier: Modifier = Modifier
) {
    val stations by viewModel.stations.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val singularityList by viewModel.singularityList.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
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
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "年間 特異日ランキング抽出",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 観測所選択
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "観測所を選択",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = selectedStation?.name ?: "選択肢なし",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("対象アメダス") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        stations.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    viewModel.selectStation(s)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // タイプ切り替えタブ（晴れ特異日 vs 雨特異日）
        TabRow(
            selectedTabIndex = if (selectedType == AnalyzeSingularityUseCase.SingularityType.SUNNY) 0 else 1,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedType == AnalyzeSingularityUseCase.SingularityType.SUNNY,
                onClick = { viewModel.selectType(AnalyzeSingularityUseCase.SingularityType.SUNNY) },
                text = { Text("☀ 晴天特異日 Top 5", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedType == AnalyzeSingularityUseCase.SingularityType.RAINY,
                onClick = { viewModel.selectType(AnalyzeSingularityUseCase.SingularityType.RAINY) },
                text = { Text("☔ 雨天特異日 Top 5", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 解析状態表示
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
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
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // ランキングリストの描画
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                singularityList.forEachIndexed { index, result ->
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD700) // ゴールド
                        1 -> Color(0xFFC0C0C0) // シルバー
                        2 -> Color(0xFFCD7F32) // ブロンズ
                        else -> Color.Gray
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 順位丸バッジ
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(rankColor, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))

                                // 日付表示
                                Text(
                                    text = "${result.month}月 ${result.day}日",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 確率データ
                            Column(horizontalAlignment = Alignment.End) {
                                if (selectedType == AnalyzeSingularityUseCase.SingularityType.SUNNY) {
                                    Text(
                                        text = "晴天率: ${String.format("%.1f", result.sunProbability)}%",
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "降水率: ${String.format("%.1f", result.rainProbability)}%",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                } else {
                                    Text(
                                        text = "降水率: ${String.format("%.1f", result.rainProbability)}%",
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFE91E63),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "晴天率: ${String.format("%.1f", result.sunProbability)}%",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 免責バナー
        ComplianceBanner(modifier = Modifier.padding(top = 16.dp))
    }
}
