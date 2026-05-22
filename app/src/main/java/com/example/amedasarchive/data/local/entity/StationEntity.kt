package com.example.amedasarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 気象観測地点（アメダス）を表すデータベースエンティティ
 */
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey 
    val stationId: String,       // 気象庁の地点コード（例: "47662" 東京）
    val name: String,             // 地点名（例: "東京"）
    val kana: String,             // カナ表記（例: "トウキョウ"）
    val prefecture: String,       // 都道府県名（例: "東京都"）
    val latitude: Double,         // 緯度
    val longitude: Double,        // 経度
    val elevation: Double,        // 標高 (m)
    val stationType: String       // 観測区分（官署、アメダスなど）
)
