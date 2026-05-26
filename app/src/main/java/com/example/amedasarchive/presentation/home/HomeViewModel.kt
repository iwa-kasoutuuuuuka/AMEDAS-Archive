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

    companion object {
        /** 都道府県の地理的表示順序（北から南） */
        private val PREFECTURE_ORDER = listOf(
            "北海道", "青森県", "岩手県", "宮城県", "秋田県", "山形県", "福島県",
            "茨城県", "栃木県", "群馬県", "埼玉県", "千葉県", "東京都", "神奈川県",
            "新潟県", "富山県", "石川県", "福井県", "山梨県", "長野県", "岐阜県",
            "静岡県", "愛知県", "三重県", "滋賀県", "京都府", "大阪府", "兵庫県",
            "奈良県", "和歌山県", "鳥取県", "島根県", "岡山県", "広島県", "山口県",
            "徳島県", "香川県", "愛媛県", "高知県", "福岡県", "佐賀県", "長崎県",
            "熊本県", "大分県", "宮崎県", "鹿児島県", "沖縄県"
        )
    }

    /**
     * 初期データの読み込み。
     * 同期済みデータが存在する都道府県のみをアクティブフィルタで取得・表示する。
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true

            val prefs = repository.getActivePrefectures()
            val sortedPrefs = prefs.sortedBy { pref ->
                PREFECTURE_ORDER.indexOf(pref).let { index -> if (index == -1) 999 else index }
            }
            _prefectures.value = sortedPrefs
            if (sortedPrefs.isNotEmpty()) {
                selectPrefecture(sortedPrefs.first())
            } else {
                _prefectures.value = emptyList()
                _stations.value = emptyList()
                _selectedStation.value = null
            }
            _isLoading.value = false
        }
    }

    fun selectPrefecture(pref: String) {
        _selectedPrefecture.value = pref
        viewModelScope.launch {
            val list = repository.getActiveStationsByPrefecture(pref)
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
