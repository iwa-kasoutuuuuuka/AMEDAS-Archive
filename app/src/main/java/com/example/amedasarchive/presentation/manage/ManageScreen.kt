package com.example.amedasarchive.presentation.manage

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

@Composable
fun ManageScreen(
    viewModel: ManageViewModel,
    modifier: Modifier = Modifier
) {
    val storageList by viewModel.storageList.collectAsState()
    val totalSizeMB by viewModel.totalSizeMB.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val syncProgressText by viewModel.syncProgressText.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val scrollState = rememberScrollState()

    // テスト用の都道府県リスト
    val prefsList = listOf("北海道", "東京都", "大阪府", "福岡県", "沖縄県")

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
                            MaterialTheme.colorScheme.error
                        )
                    )
                )
                .padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "データ蓄積・容量管理",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ストレージ概要ダッシュボード
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💾 端末内DB使用状況",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${String.format("%.2f", totalSizeMB)} MB",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "総データ件数: ${storageList.sumOf { it.recordCount }} 件",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        // 同期進捗インジケーター（WorkManager実行時のみ美しく表示）
        if (isSyncing || syncProgress > 0f) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔄 同期進行中...",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // プログレスバーの描画
                    LinearProgressIndicator(
                        progress = syncProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncProgressText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 都道府県別の同期・クリーンアップ操作リスト
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "地域別の同期とデータクリーンアップ",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                prefsList.forEach { pref ->
                    val usage = storageList.find { it.prefecture == pref }
                    val records = usage?.recordCount ?: 0L
                    val sizeKB = usage?.estimatedSizeKB ?: 0L

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = pref, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "蓄積: ${records}件 (${String.format("%.1f", sizeKB / 1024.0)}MB)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Row {
                            // 同期ボタン
                            Button(
                                onClick = { viewModel.startSyncForPrefecture(pref) },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(text = "同期", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            // 削除（クリーンアップ）ボタン
                            OutlinedButton(
                                onClick = { viewModel.deletePrefectureData(pref) },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(text = "クリア", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 免責バナー
        ComplianceBanner(modifier = Modifier.padding(top = 16.dp))
    }
}
