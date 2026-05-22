package com.example.amedasarchive.presentation.singularity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amedasarchive.domain.model.SingularityResult
import com.example.amedasarchive.domain.model.Station
import com.example.amedasarchive.domain.repository.WeatherRepository
import com.example.amedasarchive.domain.usecase.AnalyzeSingularityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SingularityViewModel(
    private val repository: WeatherRepository,
    private val analyzeSingularityUseCase: AnalyzeSingularityUseCase
) : ViewModel() {

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    private val _singularityList = MutableStateFlow<List<SingularityResult>>(emptyList())
    val singularityList: StateFlow<List<SingularityResult>> = _singularityList.asStateFlow()

    private val _selectedType = MutableStateFlow(AnalyzeSingularityUseCase.SingularityType.SUNNY)
    val selectedType: StateFlow<AnalyzeSingularityUseCase.SingularityType> = _selectedType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val list = repository.getAllStations()
            _stations.value = list
            if (list.isNotEmpty()) {
                _selectedStation.value = list.first()
                analyzeSingularities()
            }
        }
    }

    fun selectStation(station: Station) {
        _selectedStation.value = station
        analyzeSingularities()
    }

    fun selectType(type: AnalyzeSingularityUseCase.SingularityType) {
        _selectedType.value = type
        analyzeSingularities()
    }

    /**
     * 特異日スキャン分析を実行
     */
    fun analyzeSingularities() {
        val station = _selectedStation.value ?: return
        val type = _selectedType.value
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // DB内にデータがあるかチェック
            val range = repository.getMinMaxDate(station.stationId)
            if (range.first == null) {
                _error.value = "※DBにデータがありません。同期管理からデータをダウンロードしてください。"
                _singularityList.value = emptyList()
                _isLoading.value = false
                return@launch
            }

            try {
                // Top 5 の特異日を算出
                val list = analyzeSingularityUseCase.execute(station.stationId, type, limit = 5)
                _singularityList.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "統計解析中にエラーが発生しました。"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
