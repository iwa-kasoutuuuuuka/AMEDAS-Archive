package com.example.amedasarchive.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * 観測地点ごとの日別気象実測データを保存するデータベースエンティティ
 * 複合主キー: 地点コード(stationId) と 日付(date)
 * 期間検索のパフォーマンス向上のため、複合インデックスおよび日付単体インデックスを付与
 */
@Entity(
    tableName = "daily_weather",
    primaryKeys = ["stationId", "date"],
    indices = [
        Index(value = ["stationId", "date"]),
        Index(value = ["date"]) // 全地域における同一日付比較用
    ]
)
data class DailyWeatherEntity(
    val stationId: String,          // 地点コード
    val date: String,               // 日付（フォーマット: YYYY-MM-DD）
    val temperatureMean: Float?,    // 平均気温 (℃)
    val temperatureMax: Float?,     // 最高気温 (℃)
    val temperatureMin: Float?,     // 最低気温 (℃)
    val precipitation: Float?,      // 日降水量 (mm)
    val sunshineHours: Float?,      // 日照時間 (h)
    val snowDepth: Int?,            // 最深積雪 (cm)
    val humidityMean: Float?,       // 平均湿度 (%)
    val windSpeedMean: Float?       // 平均風速 (m/s)
)
