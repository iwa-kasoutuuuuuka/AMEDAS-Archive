package com.example.amedasarchive.domain.model

/**
 * 特異日スキャン分析の結果を表すドメインデータモデル
 */
data class SingularityResult(
    val month: Int,
    val day: Int,
    val rainProbability: Double,    // 降水確率 (日降水量1.0mm以上の日割合 %)
    val sunProbability: Double,     // 晴天確率 (日照時間3.0時間以上の日割合 %)
    val avgTemp: Double,            // 平均気温 (℃)
    val description: String         // 天候特徴 ("晴天特異日"、"雨天特異日"など)
)
