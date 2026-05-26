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

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _selectedStationA = MutableStateFlow<Station?>(null)
    val selectedStationA: StateFlow<Station?> = _selectedStationA.asStateFlow()

    private val _selectedStationB = MutableStateFlow<Station?>(null)
    val selectedStationB: StateFlow<Station?> = _selectedStationB.asStateFlow()

    private val _dataListA = MutableStateFlow<List<WeatherStats>>(emptyList())
    val dataListA: StateFlow<List<WeatherStats>> = _dataListA.asStateFlow()

    private val _dataListB = MutableStateFlow<List<WeatherStats>>(emptyList())
    val dataListB: StateFlow<List<WeatherStats>> = _dataListB.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadStations()
    }

    fun loadStations() {
        viewModelScope.launch {
            val list = repository.getAllStations()
            _stations.value = list
            if (list.size >= 2) {
                if (_selectedStationA.value == null) {
                    _selectedStationA.value = list[0]
                }
                if (_selectedStationB.value == null) {
                    _selectedStationB.value = list[1]
                }
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
     * 2地点の過去1ヶ月間などのデータを重ね合わせ比較するために
     * データベースからフロー経由でリアルタイム取得
     */
    fun compareData() {
        val stationA = _selectedStationA.value ?: return
        val stationB = _selectedStationB.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            // 比較用に2地点の全記録期間をカバーする日付範囲を決定
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

            // 2地点のデータをそれぞれ抽出して結合
            repository.getCompareData(stationA.stationId, stationB.stationId, startDate, endDate)
                .collect { list ->
                    // 地点Aと地点Bにデータを仕分け
                    _dataListA.value = list.filter { it.stationId == stationA.stationId }
                    _dataListB.value = list.filter { it.stationId == stationB.stationId }
                    _isLoading.value = false
                }
        }
    }
}
