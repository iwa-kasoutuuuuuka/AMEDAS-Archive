package com.example.amedasarchive.presentation.manage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.amedasarchive.data.local.AppDatabase
import com.example.amedasarchive.data.remote.AmedasSyncWorker
import com.example.amedasarchive.domain.repository.PrefectureStorageUsage
import com.example.amedasarchive.domain.repository.WeatherRepository
import com.example.amedasarchive.domain.usecase.ManageStorageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ManageViewModel(
    private val context: Context,
    private val repository: WeatherRepository,
    private val manageStorageUseCase: ManageStorageUseCase
) : ViewModel() {

    private val _storageList = MutableStateFlow<List<PrefectureStorageUsage>>(emptyList())
    val storageList: StateFlow<List<PrefectureStorageUsage>> = _storageList.asStateFlow()

    private val _totalSizeMB = MutableStateFlow(0.0)
    val totalSizeMB: StateFlow<Double> = _totalSizeMB.asStateFlow()

    // WorkManager 同期進捗状態
    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _syncProgressText = MutableStateFlow("同期は実行されていません")
    val syncProgressText: StateFlow<String> = _syncProgressText.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadStorageInfo()
    }

    /**
     * 都道府県ごとのDB登録レコード数およびファイルサイズ測定
     */
    fun loadStorageInfo() {
        viewModelScope.launch {
            val usageList = manageStorageUseCase.getStorageUsageSummary()
            _storageList.value = usageList
            
            val total = manageStorageUseCase.getTotalStorageUsageMB()
            _totalSizeMB.value = total
        }
    }

    /**
     * 指定された都道府県に属するすべての観測所データを一括削除（クリーンアップ）
     */
    fun deletePrefectureData(prefecture: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            manageStorageUseCase.clearPrefectureData(prefecture)
            loadStorageInfo()
            _isSyncing.value = false
        }
    }

    /**
     * 【WorkManager同期開始】
     * 選択された都道府県のすべての観測所データの一括同期処理をバックグラウンド実行。
     * 同時にWorkInfoから詳細進捗を購読します。
     */
    fun startSyncForPrefecture(prefecture: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            
            // 該当日付の地点IDリストを取得
            val stations = repository.getStationsByPrefecture(prefecture)
            val stationIds = stations.map { it.stationId }.toTypedArray()

            if (stationIds.isEmpty()) {
                _syncProgressText.value = "エラー: 地点マスタがありません"
                _isSyncing.value = false
                return@launch
            }

            // WorkRequestの構成
            val syncWorkRequest = OneTimeWorkRequestBuilder<AmedasSyncWorker>()
                .setInputData(
                    workDataOf(AmedasSyncWorker.KEY_STATION_IDS to stationIds)
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED) // ネットワーク接続時のみ実行
                        .build()
                )
                .build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "amedas_sync_work_${prefecture}",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )

            // WorkInfoのライブ進捗監視（Flowで購読してメモリリーク・メインスレッド例外を完全回避）
            workManager.getWorkInfoByIdFlow(syncWorkRequest.id)
                .collect { workInfo ->
                    if (workInfo != null) {
                        when (workInfo.state) {
                            WorkInfo.State.RUNNING -> {
                                val current = workInfo.progress.getInt(AmedasSyncWorker.KEY_PROGRESS_CURRENT, 0)
                                val total = workInfo.progress.getInt(AmedasSyncWorker.KEY_PROGRESS_TOTAL, 0)
                                val name = workInfo.progress.getString(AmedasSyncWorker.KEY_CURRENT_STATION_NAME) ?: ""

                                if (total > 0) {
                                    val percent = current.toFloat() / total.toFloat()
                                    _syncProgress.value = percent
                                    _syncProgressText.value = "同期中: $prefecture ($name) ... $current / $total 地点完了"
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                _syncProgress.value = 1.0f
                                _syncProgressText.value = "同期完了: $prefecture のデータが蓄積されました"
                                _isSyncing.value = false
                                loadStorageInfo()
                                // 3秒後に進捗表示をクリアしてカードを非表示にする
                                viewModelScope.launch {
                                    kotlinx.coroutines.delay(3000)
                                    if (!_isSyncing.value) {
                                        _syncProgress.value = 0f
                                    }
                                }
                            }
                            WorkInfo.State.FAILED -> {
                                _syncProgressText.value = "同期失敗: エラーが発生しました"
                                _isSyncing.value = false
                                // 3秒後に進捗表示をクリアしてカードを非表示にする
                                viewModelScope.launch {
                                    kotlinx.coroutines.delay(3000)
                                    if (!_isSyncing.value) {
                                        _syncProgress.value = 0f
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
        }
    }
}
