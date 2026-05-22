package com.example.amedasarchive.domain.usecase

import com.example.amedasarchive.domain.model.WeatherStats
import com.example.amedasarchive.domain.repository.WeatherRepository

/**
 * 特定の観測所・日付（MM-DD）における過去の気候統計を算出するユースケース
 */
class CalculateStatsUseCase(
    private val repository: WeatherRepository
) {
    /**
     * @param stationId 地点コード
     * @param month 月（1〜12）
     * @param day 日（1〜31）
     */
    suspend fun execute(stationId: String, month: Int, day: Int): WeatherStats? {
        val targetMonthDay = String.format("%02d-%02d", month, day)
        
        // リポジトリから高速計算されたDB統計値を取得
        return repository.getSingularityStats(stationId, targetMonthDay)
    }

    /**
     * 指定された地点の利用可能なデータ期間（最古年〜最新年）を取得します。
     * 期間選択時のバリデーションに使用。
     */
    suspend fun getAvailableYearsRange(stationId: String): Pair<Int?, Int?> {
        val minMax = repository.getMinMaxDate(stationId)
        val minYear = minMax.first?.split("-")?.firstOrNull()?.toIntOrNull()
        val maxYear = minMax.second?.split("-")?.firstOrNull()?.toIntOrNull()
        return Pair(minYear, maxYear)
    }
}
