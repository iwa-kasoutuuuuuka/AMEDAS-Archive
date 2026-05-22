package com.example.amedasarchive.domain.repository

import com.example.amedasarchive.domain.model.SingularityResult
import com.example.amedasarchive.domain.model.Station
import com.example.amedasarchive.domain.model.WeatherStats
import kotlinx.coroutines.flow.Flow

/**
 * 気象データおよび地点データを扱うリポジトリのインターフェース
 * クリーンアーキテクチャに基づき、ドメイン層に配置してDIPを適用します。
 */
interface WeatherRepository {
    
    // --- 地点（マスター）データ関連 ---
    suspend fun getAllStations(): List<Station>
    suspend fun getStationsByPrefecture(prefecture: String): List<Station>
    suspend fun getPrefectures(): List<String>
    suspend fun insertStations(stations: List<Station>)

    // --- 気象データ同期関連 ---
    /**
     * 該当観測所のデータを同期。ローカルDBの最新日付を自動確認し、
     * 未取得期間の差分データのみを気象庁から取得します。
     */
    suspend fun syncStationData(stationId: String): Boolean

    // --- 統計計算・検索関連 ---
    /**
     * 特定の観測所における、特定の日（MM-DD）の過去数十年の気象統計を算出。
     */
    suspend fun getSingularityStats(stationId: String, targetMonthDay: String): WeatherStats?

    /**
     * 指定された地点のDB内の最小（最古）日付と最大（最新）日付のペアを返却（期間バリデーション用）。
     */
    suspend fun getMinMaxDate(stationId: String): Pair<String?, String?>

    /**
     * 2地点の同一期間のデータを同時に取得して比較するためのフロー
     */
    fun getCompareData(stationIdA: String, stationIdB: String, startDate: String, endDate: String): Flow<List<WeatherStats>>

    // --- 容量管理・クリーンアップ関連 ---
    /**
     * 都道府県ごとの保存済みレコード数と推定容量（DBサイズに換算）のリストを取得。
     */
    suspend fun getStorageUsage(): List<PrefectureStorageUsage>

    /**
     * 特定地点のローカルデータを全削除（容量解放用）
     */
    suspend fun deleteStationData(stationId: String)
}

/**
 * 都道府県ごとのストレージ使用状況を表すモデル
 */
data class PrefectureStorageUsage(
    val prefecture: String,
    val recordCount: Long,
    val estimatedSizeKB: Long
)
