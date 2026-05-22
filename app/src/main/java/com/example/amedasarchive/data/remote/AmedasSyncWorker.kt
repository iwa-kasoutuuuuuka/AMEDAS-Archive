package com.example.amedasarchive.data.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.amedasarchive.data.local.AppDatabase
import com.example.amedasarchive.data.repository.WeatherRepositoryImpl
import kotlinx.coroutines.delay

/**
 * 複数地点の気象データをバックグラウンドで一括ダウンロードし、ローカルDBに格納するWorkManager用のWorker。
 * アプリケーションが閉じられてもタスクの実行を継続します。
 */
class AmedasSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_STATION_IDS = "station_ids"
        const val KEY_PROGRESS_CURRENT = "progress_current"
        const val KEY_PROGRESS_TOTAL = "progress_total"
        const val KEY_CURRENT_STATION_NAME = "current_station_name"
    }

    override suspend fun doWork(): Result {
        // 同期対象となる観測所IDのリストを取得
        val stationIds = inputData.getStringArray(KEY_STATION_IDS) ?: return Result.failure()
        val total = stationIds.size
        if (total == 0) return Result.success()

        // Room DBから直接レポジトリインスタンスを作成して安全にアクセス
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = WeatherRepositoryImpl(db.weatherDao(), db.stationDao(), db.syncLogDao())

        try {
            for (index in 0 until total) {
                val stationId = stationIds[index]
                val currentProgress = index + 1

                // 観測所詳細の取得
                val station = db.stationDao().getStationById(stationId)
                val stationName = station?.name ?: "コード: $stationId"

                // 1. UIへ通知するためにWorkManagerのProgressを更新
                setProgress(
                    workDataOf(
                        KEY_PROGRESS_CURRENT to currentProgress,
                        KEY_PROGRESS_TOTAL to total,
                        KEY_CURRENT_STATION_NAME to stationName
                    )
                )

                // 2. 該当地点の同期処理（レポジトリ経由、内部で差分検知を実施）を実行
                repository.syncStationData(stationId)

                // 3. 【超重要】気象庁のサーバーに短時間で大量のリクエストを送信して
                // 負荷をかけないよう、リクエスト間に必ず 2000ms のウェイトを設ける
                delay(2000)
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // 一時的なネットワークエラー等の場合は、再試行キューに入れる
            return Result.retry()
        }
    }
}
