package com.example.amedasarchive.presentation.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amedasarchive.domain.model.Station
import com.example.amedasarchive.domain.model.WeatherStats
import com.example.amedasarchive.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CompareViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    // 同期済みの都道府県リスト
    private val _activePrefectures = MutableStateFlow<List<String>>(emptyList())
    val activePrefectures: StateFlow<List<String>> = _activePrefectures.asStateFlow()

    // 地点Aの選択状態
    private val _selectedPrefectureA = MutableStateFlow("")
    val selectedPrefectureA: StateFlow<String> = _selectedPrefectureA.asStateFlow()

    private val _stationsA = MutableStateFlow<List<Station>>(emptyList())
    val stationsA: StateFlow<List<Station>> = _stationsA.asStateFlow()

    private val _selectedStationA = MutableStateFlow<Station?>(null)
    val selectedStationA: StateFlow<Station?> = _selectedStationA.asStateFlow()

    // 地点Bの選択状態
    private val _selectedPrefectureB = MutableStateFlow("")
    val selectedPrefectureB: StateFlow<String> = _selectedPrefectureB.asStateFlow()

    private val _stationsB = MutableStateFlow<List<Station>>(emptyList())
    val stationsB: StateFlow<List<Station>> = _stationsB.asStateFlow()

    private val _selectedStationB = MutableStateFlow<Station?>(null)
    val selectedStationB: StateFlow<Station?> = _selectedStationB.asStateFlow()

    // 比較データリスト
    private val _dataListA = MutableStateFlow<List<WeatherStats>>(emptyList())
    val dataListA: StateFlow<List<WeatherStats>> = _dataListA.asStateFlow()

    private val _dataListB = MutableStateFlow<List<WeatherStats>>(emptyList())
    val dataListB: StateFlow<List<WeatherStats>> = _dataListB.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadActiveData()
    }

    companion object {
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
     * データが蓄積されている都道府県および初期観測所を読み込む
     */
    fun loadActiveData() {
        viewModelScope.launch {
            _isLoading.value = true
            val prefs = repository.getActivePrefectures()
            val sortedPrefs = prefs.sortedBy { pref ->
                PREFECTURE_ORDER.indexOf(pref).let { index -> if (index == -1) 999 else index }
            }
            _activePrefectures.value = sortedPrefs

            if (sortedPrefs.isNotEmpty()) {
                val initPrefA = _selectedPrefectureA.value.ifEmpty { sortedPrefs.first() }
                val initPrefB = _selectedPrefectureB.value.ifEmpty { 
                    if (sortedPrefs.size >= 2) sortedPrefs[1] else sortedPrefs.first()
                }
                selectPrefectureA(initPrefA)
                selectPrefectureB(initPrefB)
            } else {
                _selectedPrefectureA.value = ""
                _selectedPrefectureB.value = ""
                _stationsA.value = emptyList()
                _stationsB.value = emptyList()
                _selectedStationA.value = null
                _selectedStationB.value = null
                _dataListA.value = emptyList()
                _dataListB.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun selectPrefectureA(pref: String) {
        _selectedPrefectureA.value = pref
        viewModelScope.launch {
            val list = repository.getActiveStationsByPrefecture(pref)
            _stationsA.value = list
            if (list.isNotEmpty()) {
                val current = _selectedStationA.value
                val matched = list.firstOrNull { it.stationId == current?.stationId }
                selectStationA(matched ?: list.first())
            } else {
                _selectedStationA.value = null
                compareData()
            }
        }
    }

    fun selectPrefectureB(pref: String) {
        _selectedPrefectureB.value = pref
        viewModelScope.launch {
            val list = repository.getActiveStationsByPrefecture(pref)
            _stationsB.value = list
            if (list.isNotEmpty()) {
                val current = _selectedStationB.value
                val matched = list.firstOrNull { it.stationId == current?.stationId }
                selectStationB(matched ?: list.first())
            } else {
                _selectedStationB.value = null
                compareData()
            }
        }
    }

    fun selectStationA(station: Station) {
        _selectedStationA.value = station
        compareData()
    }

    fun selectStationB(station: Station) {
        _selectedStationB.value = station
        compareData()
    }

    /**
     * 2地点のデータを重ね合わせ比較するためにデータベースからフロー経由でリアルタイム取得
     */
    fun compareData() {
        val stationA = _selectedStationA.value
        val stationB = _selectedStationB.value
        if (stationA == null || stationB == null) {
            _dataListA.value = emptyList()
            _dataListB.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val rangeA = repository.getMinMaxDate(stationA.stationId)
            val rangeB = repository.getMinMaxDate(stationB.stationId)

            val minA = rangeA.first
            val minB = rangeB.first
            val maxA = rangeA.second
            val maxB = rangeB.second

            val startDate = when {
                minA != null && minB != null -> if (minA < minB) minA else minB
                minA != null -> minA
                minB != null -> minB
                else -> "2023-01-01"
            }
            val endDate = when {
                maxA != null && maxB != null -> if (maxA > maxB) maxA else maxB
                maxA != null -> maxA
                maxB != null -> maxB
                else -> "2023-12-31"
            }

            repository.getCompareData(stationA.stationId, stationB.stationId, startDate, endDate)
                .collect { list ->
                    _dataListA.value = list.filter { it.stationId == stationA.stationId }
                    _dataListB.value = list.filter { it.stationId == stationB.stationId }
                    _isLoading.value = false
                }
        }
    }
}
