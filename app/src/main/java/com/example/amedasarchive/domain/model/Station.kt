package com.example.amedasarchive.domain.model

/**
 * 観測地点（アメダス）を表すドメインモデル
 */
data class Station(
    val stationId: String,
    val name: String,
    val kana: String,
    val prefecture: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val stationType: String
)
