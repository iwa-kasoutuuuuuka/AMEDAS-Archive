package com.example.amedasarchive.domain.usecase

import com.example.amedasarchive.domain.repository.WeatherRepository

/**
 * 特定の観測所の気象データを同期（差分インポート）するユースケース
 */
class SyncStationUseCase(
    private val repository: WeatherRepository
) {
    suspend fun execute(stationId: String, years: Int = 10): Boolean {
        return repository.syncStationData(stationId, years)
    }
}
