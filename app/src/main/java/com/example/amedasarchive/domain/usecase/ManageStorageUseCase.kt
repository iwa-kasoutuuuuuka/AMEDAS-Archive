package com.example.amedasarchive.domain.usecase

import com.example.amedasarchive.domain.repository.PrefectureStorageUsage
import com.example.amedasarchive.domain.repository.WeatherRepository

/**
 * データベースのストレージ容量可視化とクリーンアップ（データ一括削除）を制御するユースケース
 */
class ManageStorageUseCase(
    private val repository: WeatherRepository
) {
    /**
     * 各都道府県ごとの保存済み気象データ件数と推定容量（KB）を取得。
     */
    suspend fun getStorageUsageSummary(): List<PrefectureStorageUsage> {
        return repository.getStorageUsage()
    }

    /**
     * データベース全体の総レコード件数および総推定サイズ（MB表記）を取得。
     */
    suspend fun getTotalStorageUsageMB(): Double {
        val list = repository.getStorageUsage()
        val totalKB = list.sumOf { it.estimatedSizeKB }
        return totalKB / 1024.0
    }

    /**
     * 特定の都道府県配下にあるすべての観測所データを削除してストレージを解放。
     */
    suspend fun clearPrefectureData(prefecture: String) {
        val stations = repository.getStationsByPrefecture(prefecture)
        stations.forEach { station ->
            repository.deleteStationData(station.stationId)
        }
    }

    /**
     * 特定の単一観測所のデータを削除してストレージを解放。
     */
    suspend fun clearStationData(stationId: String) {
        repository.deleteStationData(stationId)
    }
}
