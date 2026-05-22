package com.example.amedasarchive.domain.usecase

import com.example.amedasarchive.domain.model.SingularityResult
import com.example.amedasarchive.domain.repository.WeatherRepository

/**
 * 過去10年以上のデータから「特異日」（最も晴れやすい日、最も雨が降りやすい日）を
 * スキャンしてランキング化するユースケース。
 */
class AnalyzeSingularityUseCase(
    private val repository: WeatherRepository
) {
    enum class SingularityType {
        SUNNY, // 晴天特異日
        RAINY  // 雨天特異日
    }

    /**
     * 指定観測所における特異日のランキングを取得
     * @param limit 取得件数（例: Top 5）
     */
    suspend fun execute(
        stationId: String,
        type: SingularityType,
        limit: Int = 5
    ): List<SingularityResult> {
        val allDaysResults = mutableListOf<SingularityResult>()

        // 1年365日（うるう年は除外）をループ処理して各日の統計を取得
        for (month in 1..12) {
            val maxDays = when (month) {
                2 -> 28
                4, 6, 9, 11 -> 30
                else -> 31
            }
            for (day in 1..maxDays) {
                val monthDayStr = String.format("%02d-%02d", month, day)
                val stats = repository.getSingularityStats(stationId, monthDayStr) ?: continue

                val sunProb = stats.sunProbability ?: 0.0
                val rainProb = stats.rainProbability ?: 0.0
                val avgTemp = stats.temperatureMean ?: 0.0

                // 特異日判定のための結果を格納
                val description = if (sunProb > 70.0) "晴天特異日" else if (rainProb > 40.0) "雨天特異日" else "通常日"
                
                allDaysResults.add(
                    SingularityResult(
                        month = month,
                        day = day,
                        rainProbability = rainProb,
                        sunProbability = sunProb,
                        avgTemp = avgTemp,
                        description = description
                    )
                )
            }
        }

        // タイプに応じてソートして上位を指定件数分返却
        return when (type) {
            SingularityType.SUNNY -> {
                // 晴れ確率が高い順、同じなら降水確率が低い順
                allDaysResults.sortedWith(
                    compareByDescending<SingularityResult> { it.sunProbability }
                        .thenBy { it.rainProbability }
                ).take(limit)
            }
            SingularityType.RAINY -> {
                // 降水確率が高い順、同じなら晴れ確率が低い順
                allDaysResults.sortedWith(
                    compareByDescending<SingularityResult> { it.rainProbability }
                        .thenBy { it.sunProbability }
                ).take(limit)
            }
        }
    }
}
