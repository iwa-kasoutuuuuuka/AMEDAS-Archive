package com.example.amedasarchive.domain.model

/**
 * 特定の気象統計結果を表すドメインデータモデル
 */
data class WeatherStats(
    val stationId: String,
    val stationName: String,
    val targetDateOrDay: String,     // 日付 ("YYYY-MM-DD") または 特定の日 ("MM-DD")
    val totalYears: Int,             // 集計対象となった年数
    val temperatureMean: Double?,    // 平均気温 (℃)
    val temperatureMax: Double?,     // 最高気温 (℃)
    val temperatureMin: Double?,     // 最低気温 (℃)
    val precipitationMean: Double?,  // 平均降水量 (mm)
    val rainProbability: Double?,    // 降水確率 (日降水量1.0mm以上の日割合 %)
    val sunProbability: Double?      // 晴天確率 (日照時間3.0時間以上の日割合 %)
)
