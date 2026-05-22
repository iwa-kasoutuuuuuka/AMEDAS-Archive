package com.example.amedasarchive.data.local.dao

import androidx.room.*
import com.example.amedasarchive.data.local.entity.DailyWeatherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(weatherList: List<DailyWeatherEntity>)

    @Query("SELECT * FROM daily_weather WHERE stationId = :stationId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getWeatherFlow(stationId: String, startDate: String, endDate: String): Flow<List<DailyWeatherEntity>>

    @Query("SELECT * FROM daily_weather WHERE stationId = :stationId AND date = :date LIMIT 1")
    suspend fun getWeatherByDate(stationId: String, date: String): DailyWeatherEntity?

    /**
     * 【高速化クエリ】特定の地点における、過去数十年の「特定の日（MM-DD）」の平均値と降水確率を高速に一括集計。
     * SQLiteのstrftimeを活用し、大量の日付データから秒速で統計を算出します。
     */
    @Query("""
        SELECT 
            strftime('%m-%d', date) as dayOfMonth,
            COUNT(date) as totalYears,
            AVG(temperatureMean) as avgTempMean,
            AVG(temperatureMax) as avgTempMax,
            AVG(temperatureMin) as avgTempMin,
            AVG(precipitation) as avgPrecipitation,
            SUM(CASE WHEN precipitation >= 1.0 THEN 1 ELSE 0 END) * 100.0 / COUNT(date) as rainProbability,
            SUM(CASE WHEN sunshineHours >= 3.0 THEN 1 ELSE 0 END) * 100.0 / COUNT(date) as sunProbability
        FROM daily_weather
        WHERE stationId = :stationId AND strftime('%m-%d', date) = :targetMonthDay
        GROUP BY dayOfMonth
    """)
    suspend fun getSingularityStats(stationId: String, targetMonthDay: String): DbSingularityStats?

    /**
     * 2地点の同一期間のデータを同時に取得して比較するためのクエリ
     */
    @Query("""
        SELECT * FROM daily_weather 
        WHERE stationId IN (:stationIdA, :stationIdB) 
          AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """)
    fun getCompareData(stationIdA: String, stationIdB: String, startDate: String, endDate: String): Flow<List<DailyWeatherEntity>>

    /**
     * 特定の地点の気象データを一括削除（クリーンアップ用）
     */
    @Query("DELETE FROM daily_weather WHERE stationId = :stationId")
    suspend fun deleteByStationId(stationId: String)

    /**
     * 保存されているデータの最小（最古）日付と最大（最新）日付を取得（期間バリデーション用）
     */
    @Query("SELECT MIN(date) FROM daily_weather WHERE stationId = :stationId")
    suspend fun getMinDate(stationId: String): String?

    @Query("SELECT MAX(date) FROM daily_weather WHERE stationId = :stationId")
    suspend fun getMaxDate(stationId: String): String?

    /**
     * 【容量可視化】都道府県ごとの登録データ件数（行数）を取得
     */
    @Query("""
        SELECT s.prefecture, COUNT(d.date) as recordCount 
        FROM daily_weather d
        INNER JOIN stations s ON d.stationId = s.stationId
        GROUP BY s.prefecture
    """)
    suspend fun getRecordCountByPrefecture(): List<PrefectureRecordCount>
}

/**
 * 統計計算結果を一時的に受け取るデータトランスファーオブジェクト
 */
data class DbSingularityStats(
    val dayOfMonth: String,
    val totalYears: Int,
    val avgTempMean: Double?,
    val avgTempMax: Double?,
    val avgTempMin: Double?,
    val avgPrecipitation: Double?,
    val rainProbability: Double?, // 1.0mm以上の降水日割合 (%)
    val sunProbability: Double?   // 日照時間3.0時間以上の晴天日割合 (%)
)

/**
 * 都道府県ごとのレコード件数を表す構造体
 */
data class PrefectureRecordCount(
    val prefecture: String,
    val recordCount: Long
)
