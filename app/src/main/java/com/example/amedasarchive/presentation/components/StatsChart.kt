package com.example.amedasarchive.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amedasarchive.domain.model.WeatherStats

/**
 * 2つの異なる観測所の気象推移を重ね合わせて描画するカスタムComposeキャンバスグラフ
 */
@Composable
fun StatsChart(
    dataA: List<WeatherStats>,
    dataB: List<WeatherStats>,
    labelA: String,
    labelB: String,
    modifier: Modifier = Modifier
) {
    val colorA = Color(0xFF3F51B5) // 青系 (地点A)
    val colorB = Color(0xFFFF5722) // オレンジ系 (地点B)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // 凡例表示部
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).background(colorA, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = labelA, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Box(modifier = Modifier.size(12.dp).background(colorB, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = labelB, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // キャンバスによる描画
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val padding = 30f

                // 描画可能領域
                val chartWidth = width - (padding * 2)
                val chartHeight = height - (padding * 2)

                // 気温などの変動範囲 (仮に-10℃から40℃とする)
                val minY = -10f
                val maxY = 40f
                val rangeY = maxY - minY

                // Y軸の補助線グリッド (0℃, 15℃, 30℃)
                val gridLines = listOf(0f, 15f, 30f)
                gridLines.forEach { temp ->
                    val y = padding + chartHeight - (((temp - minY) / rangeY) * chartHeight)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 2f
                    )
                }

                // 2地点のポイントパスの組み立てと描画
                fun drawDataPath(data: List<WeatherStats>, color: Color) {
                    if (data.size < 2) return
                    val path = Path()
                    val stepX = chartWidth / (data.size - 1)

                    data.forEachIndexed { index, stats ->
                        val temp = (stats.temperatureMean ?: 0.0).toFloat()
                        // 描画ピクセル座標の計算
                        val x = padding + (index * stepX)
                        val y = padding + chartHeight - (((temp - minY) / rangeY) * chartHeight)

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 4f)
                    )
                }

                // 地点Aの推移を描画
                drawDataPath(dataA, colorA)
                // 地点Bの推移を描画
                drawDataPath(dataB, colorB)
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "開始日", fontSize = 10.sp, color = Color.Gray)
            Text(text = "期間内推移 (気温)", fontSize = 10.sp, color = Color.Gray)
            Text(text = "終了日", fontSize = 10.sp, color = Color.Gray)
        }
    }
}
