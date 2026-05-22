package com.example.amedasarchive.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 気象業務法および政府標準利用規約に適合するための免責・出典バナー
 * アプリのすべての統計表示画面の下部などに配置します。
 */
@Composable
fun ComplianceBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "【免責事項及びデータ出典について】",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "・本アプリに掲載されている降水確率、晴天率、平均気温等の統計情報は、すべて気象庁が公表した過去の観測実測データを元にローカル計算したものです。未来の予測（天気予報業務）は一切行っていません。気象業務法における『予報業務許可』の対象外となります。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 15.sp,
                fontSize = 11.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "・本アプリで使用している一次データは、政府標準利用規約（第2.0版）に基づき、出典を明記のうえ利用しております。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 15.sp,
                fontSize = 11.sp
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "データ出典: 気象庁ホームページ (https://www.jma.go.jp/)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        }
    }
}
