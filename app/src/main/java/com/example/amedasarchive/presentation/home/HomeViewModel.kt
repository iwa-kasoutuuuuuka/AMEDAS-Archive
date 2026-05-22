package com.example.amedasarchive.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amedasarchive.domain.model.Station
import com.example.amedasarchive.domain.model.WeatherStats
import com.example.amedasarchive.domain.repository.WeatherRepository
import com.example.amedasarchive.domain.usecase.CalculateStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: WeatherRepository,
    private val calculateStatsUseCase: CalculateStatsUseCase
) : ViewModel() {

    // UI状態
    private val _prefectures = MutableStateFlow<List<String>>(emptyList())
    val prefectures: StateFlow<List<String>> = _prefectures.asStateFlow()

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _selectedPrefecture = MutableStateFlow("")
    val selectedPrefecture: StateFlow<String> = _selectedPrefecture.asStateFlow()

    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    private val _statsResult = MutableStateFlow<WeatherStats?>(null)
    val statsResult: StateFlow<WeatherStats?> = _statsResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 期間設定とバリデーション情報
    private val _dateRangeText = MutableStateFlow("最古: -- ~ 最新: --")
    val dateRangeText: StateFlow<String> = _dateRangeText.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    init {
        loadInitialData()
    }

    /**
     * 代表的な観測地点マスタを初期データとして登録し、都道府県一覧を取得
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // OSS動作確認用に代表的な観測マスタデータが空なら自動挿入
            val currentStations = repository.getAllStations()
            if (currentStations.isEmpty()) {
                val defaultStations = listOf(
                    Station("47412", "札幌", "サッポロ", "北海道", 43.06, 141.32, 17.0, "官署"),
                    Station("47662", "東京", "トウキョウ", "東京都", 35.69, 139.75, 25.0, "官署"),
                    Station("47772", "大阪", "オオサカ", "大阪府", 34.68, 135.52, 23.0, "官署"),
                    Station("47807", "福岡", "フクオカ", "福岡県", 33.58, 130.37, 14.0, "官署"),
                    Station("47936", "那覇", "ナハ", "沖縄県", 26.20, 127.68, 8.0, "官署")
                )
                repository.insertStations(defaultStations)
            }
            
            val prefs = repository.getPrefectures()
            _prefectures.value = prefs
            if (prefs.isNotEmpty()) {
                selectPrefecture(prefs.first())
            }
            _isLoading.value = false
        }
    }

    fun selectPrefecture(pref: String) {
        _selectedPrefecture.value = pref
        viewModelScope.launch {
            val list = repository.getStationsByPrefecture(pref)
            _stations.value = list
            if (list.isNotEmpty()) {
                selectStation(list.first())
            } else {
                _selectedStation.value = null
            }
        }
    }

    fun selectStation(station: Station) {
        _selectedStation.value = station
        viewModelScope.launch {
            // DB内の格納最大・最小期間を自動検知
            val range = repository.getMinMaxDate(station.stationId)
            val minDate = range.first ?: "未取得"
            val maxDate = range.second ?: "未取得"
            _dateRangeText.value = "DBデータ期間: $minDate ~ $maxDate"
        }
    }

    /**
     * 指定日付の統計計算を実行。
     * 同時に指定年がDB期間内であるかのバリデーションを実行。
     */
    fun calculateStats(month: Int, day: Int, targetYear: Int) {
        val station = _selectedStation.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _validationError.value = null

            // DBから格納済み期間の上限・下限を取得
            val range = repository.getMinMaxDate(station.stationId)
            val minYear = range.first?.split("-")?.firstOrNull()?.toIntOrNull()
            val maxYear = range.second?.split("-")?.firstOrNull()?.toIntOrNull()

            // 期間バリデーション処理
            if (minYear == null || maxYear == null) {
                _validationError.value = "※ローカルDBにデータがありません。同期管理からダウンロードしてください。"
                _statsResult.value = null
                _isLoading.value = false
                return@launch
            }

            if (targetYear < minYear || targetYear > maxYear) {
                _validationError.value = "※エラー: 入力された年($targetYear)はDBの保存範囲外です。範囲: ${minYear}年〜${maxYear}年"
                _statsResult.value = null
                _isLoading.value = false
                return@launch
            }

            // 特異日（同月同日）の集計計算を取得
            val result = calculateStatsUseCase.execute(station.stationId, month, day)
            _statsResult.value = result
            _isLoading.value = false
        }
    }
}
