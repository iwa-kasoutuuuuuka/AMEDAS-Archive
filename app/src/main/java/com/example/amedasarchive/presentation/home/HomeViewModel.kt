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
     * 代表的な観測地点マスタを初期データとして登録し、都道府県一覧を取得
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // OSS動作確認用に代表的な観測マスタデータが空なら自動挿入（47都道府県完全網羅、各県3箇所ずつに大幅拡張）
            val currentStations = repository.getAllStations()
            if (currentStations.isEmpty()) {
                val defaultStations = listOf(
                    // 北海道
                    Station("47412", "札幌", "サッポロ", "北海道", 43.06, 141.32, 17.0, "官署"),
                    Station("47407", "旭川", "アサヒカワ", "北海道", 43.77, 142.37, 112.0, "官署"),
                    Station("47430", "函館", "ハコダテ", "北海道", 41.82, 140.75, 34.0, "官署"),
                    // 青森県
                    Station("47401", "青森", "アオモリ", "青森県", 40.82, 140.76, 3.0, "官署"),
                    Station("47405", "むつ", "ムツ", "青森県", 41.28, 141.21, 4.0, "官署"),
                    Station("47409", "八戸", "ハチノヘ", "青森県", 40.51, 141.50, 27.0, "官署"),
                    // 岩手県
                    Station("47584", "盛岡", "モリオカ", "岩手県", 39.70, 141.17, 155.0, "官署"),
                    Station("47585", "宮古", "ミヤコ", "岩手県", 39.64, 141.97, 39.0, "官署"),
                    Station("47510", "大船渡", "オオフナト", "岩手県", 39.02, 141.74, 43.0, "官署"),
                    // 宮城県
                    Station("47590", "仙台", "センダイ", "宮城県", 38.26, 140.89, 39.0, "官署"),
                    Station("47597", "石巻", "イシノマキ", "宮城県", 38.43, 141.30, 42.0, "官署"),
                    Station("44296", "気仙沼", "ケセンヌマ", "宮城県", 38.90, 141.58, 4.0, "アメダス"),
                    // 秋田県
                    Station("47582", "秋田", "アキタ", "秋田県", 39.72, 140.10, 6.0, "官署"),
                    Station("44021", "能代", "ノシロ", "秋田県", 40.21, 140.03, 4.0, "アメダス"),
                    Station("44131", "横手", "ヨコテ", "秋田県", 39.31, 140.56, 73.0, "アメダス"),
                    // 山形県
                    Station("47592", "山形", "ヤマガタ", "山形県", 38.25, 140.34, 153.0, "官署"),
                    Station("47587", "酒田", "サカタ", "山形県", 38.92, 139.84, 3.0, "官署"),
                    Station("47588", "新庄", "シンジョウ", "山形県", 38.76, 140.30, 126.0, "官署"),
                    // 福島県
                    Station("47595", "福島", "フクシマ", "福島県", 37.75, 140.47, 67.0, "官署"),
                    Station("47570", "若松", "ワカマツ", "福島県", 37.50, 139.93, 212.0, "官署"),
                    Station("47598", "小名浜", "オナハマ", "福島県", 36.94, 140.90, 3.0, "官署"),
                    // 茨城県
                    Station("47629", "水戸", "ミト", "茨城県", 36.38, 140.47, 29.0, "官署"),
                    Station("47646", "つくば", "ツクバ", "茨城県", 36.06, 140.13, 25.0, "官署"),
                    Station("44056", "古河", "コガ", "茨城県", 36.19, 139.70, 17.0, "アメダス"),
                    // 栃木県
                    Station("47615", "宇都宮", "ウツノミヤ", "栃木県", 36.55, 139.88, 119.0, "官署"),
                    Station("47690", "日光", "ニッコウ", "栃木県", 36.75, 139.60, 1292.0, "官署"),
                    Station("44186", "小山", "オヤマ", "栃木県", 36.31, 139.82, 35.0, "アメダス"),
                    // 群馬県
                    Station("47624", "前橋", "マエバシ", "群馬県", 36.39, 139.06, 112.0, "官署"),
                    Station("44106", "桐生", "キリュウ", "群馬県", 36.41, 139.33, 123.0, "アメダス"),
                    Station("44136", "館林", "タテバヤシ", "群馬県", 36.24, 139.54, 21.0, "アメダス"),
                    // 埼玉県
                    Station("47626", "熊谷", "クマガヤ", "埼玉県", 36.15, 139.38, 30.0, "官署"),
                    Station("44131", "さいたま", "サイタマ", "埼玉県", 35.88, 139.59, 8.0, "アメダス"),
                    Station("47622", "秩父", "チチブ", "埼玉県", 35.99, 139.08, 237.0, "官署"),
                    // 千葉県
                    Station("47682", "千葉", "チバ", "千葉県", 35.61, 140.10, 3.0, "官署"),
                    Station("47648", "銚子", "チョウシ", "千葉県", 35.73, 140.85, 19.0, "官署"),
                    Station("47672", "館山", "タテヤマ", "千葉県", 34.99, 139.86, 6.0, "官署"),
                    // 東京都
                    Station("47662", "東京", "トウキョウ", "東京都", 35.69, 139.75, 25.0, "官署"),
                    Station("44116", "八王子", "ハチオウジ", "東京都", 35.66, 139.32, 112.0, "アメダス"),
                    Station("47675", "大島", "オオシマ", "東京都", 34.78, 139.39, 76.0, "官署"),
                    // 神奈川県
                    Station("47670", "横浜", "ヨコハマ", "神奈川県", 35.44, 139.65, 39.0, "官署"),
                    Station("44356", "小田原", "オダワラ", "神奈川県", 35.25, 139.15, 14.0, "アメダス"),
                    Station("44211", "相模原", "サガミハラ", "神奈川県", 35.57, 139.35, 115.0, "アメダス"),
                    // 新潟県
                    Station("47604", "新潟", "ニイガタ", "新潟県", 37.92, 139.05, 4.0, "官署"),
                    Station("44201", "長岡", "ナガオカ", "新潟県", 37.43, 138.83, 31.0, "アメダス"),
                    Station("47616", "高田", "タカダ", "新潟県", 37.11, 138.25, 13.0, "官署"),
                    // 富山県
                    Station("47607", "富山", "トヤマ", "富山県", 36.70, 137.20, 9.0, "官署"),
                    Station("47606", "伏木", "フシキ", "富山県", 36.79, 137.05, 14.0, "官署"),
                    Station("44026", "魚津", "ウオヅ", "富山県", 36.81, 137.40, 7.0, "アメダス"),
                    // 石川県
                    Station("47605", "金沢", "カナザワ", "石川県", 36.59, 136.63, 27.0, "官署"),
                    Station("47600", "輪島", "ワジマ", "石川県", 37.39, 136.90, 7.0, "官署"),
                    Station("44131", "小松", "コマツ", "石川県", 36.40, 136.42, 6.0, "アメダス"),
                    // 福井県
                    Station("47610", "福井", "フクイ", "福井県", 36.06, 136.22, 9.0, "官署"),
                    Station("47640", "敦賀", "ツルガ", "福井県", 35.65, 136.06, 4.0, "官署"),
                    Station("44081", "大野", "オオノ", "福井県", 35.98, 136.50, 185.0, "アメダス"),
                    // 山梨県
                    Station("47638", "甲府", "コウフ", "山梨県", 35.67, 138.57, 273.0, "官署"),
                    Station("47641", "河口湖", "カワグチコ", "山梨県", 35.50, 138.76, 860.0, "官署"),
                    Station("44111", "大月", "オオツキ", "山梨県", 35.62, 138.94, 357.0, "アメダス"),
                    // 長野県
                    Station("47612", "長野", "ナガノ", "長野県", 36.66, 138.19, 418.0, "官署"),
                    Station("47618", "松本", "マツモト", "長野県", 36.25, 137.97, 610.0, "官署"),
                    Station("47622", "軽井沢", "カルイザワ", "長野県", 36.35, 138.60, 999.0, "官署"),
                    // 岐阜県
                    Station("47632", "岐阜", "ギフ", "岐阜県", 35.40, 136.76, 13.0, "官署"),
                    Station("47617", "高山", "タカヤマ", "岐阜県", 36.16, 137.25, 560.0, "官署"),
                    Station("44321", "多治見", "タジミ", "岐阜県", 35.34, 137.11, 85.0, "アメダス"),
                    // 静岡県
                    Station("47656", "静岡", "シズオカ", "静岡県", 34.98, 138.40, 14.0, "官署"),
                    Station("47654", "浜松", "ハママツ", "静岡県", 34.71, 137.71, 47.0, "官署"),
                    Station("47657", "三島", "ミシマ", "静岡県", 35.12, 138.91, 21.0, "官署"),
                    // 愛知県
                    Station("47636", "名古屋", "ナゴヤ", "愛知県", 35.17, 136.97, 51.0, "官署"),
                    Station("47668", "豊橋", "トヨハシ", "愛知県", 34.76, 137.38, 3.0, "官署"),
                    Station("44261", "岡崎", "オカザキ", "愛知県", 34.93, 137.17, 30.0, "アメダス"),
                    // 三重県
                    Station("47651", "津", "ツ", "三重県", 34.73, 136.51, 3.0, "官署"),
                    Station("47663", "尾鷲", "オワセ", "三重県", 34.07, 136.19, 17.0, "官署"),
                    Station("44111", "四日市", "ヨッカイチ", "三重県", 34.97, 136.63, 6.0, "アメダス"),
                    // 滋賀県
                    Station("47637", "彦根", "ヒコネ", "滋賀県", 35.27, 136.26, 87.0, "官署"),
                    Station("44131", "大津", "オオツ", "滋賀県", 34.99, 135.91, 86.0, "アメダス"),
                    Station("44256", "信楽", "シガラキ", "滋賀県", 34.91, 136.05, 290.0, "アメダス"),
                    // 京都府
                    Station("47759", "京都", "キョウト", "京都府", 35.01, 135.74, 41.0, "官署"),
                    Station("47750", "舞鶴", "マイヅル", "京都府", 35.45, 135.32, 6.0, "官署"),
                    Station("44111", "福知山", "フクチヤマ", "京都府", 35.30, 135.13, 24.0, "アメダス"),
                    // 大阪府
                    Station("47772", "大阪", "オオサカ", "大阪府", 34.68, 135.52, 23.0, "官署"),
                    Station("44216", "堺", "サカイ", "大阪府", 34.56, 135.47, 4.0, "アメダス"),
                    Station("44101", "豊中", "トヨナカ", "大阪府", 34.79, 135.45, 20.0, "アメダス"),
                    // 兵庫県
                    Station("47770", "神戸", "コウベ", "兵庫県", 34.69, 135.21, 5.0, "官署"),
                    Station("47769", "姫路", "ヒメジ", "兵庫県", 34.85, 134.67, 38.0, "官署"),
                    Station("47747", "豊岡", "トヨオカ", "兵庫県", 35.54, 134.82, 4.0, "官署"),
                    // 奈良県
                    Station("47780", "奈良", "ナラ", "奈良県", 34.68, 135.83, 104.0, "官署"),
                    Station("44216", "吉野", "ヨシノ", "奈良県", 34.40, 135.86, 175.0, "アメダス"),
                    Station("44246", "五條", "ゴジョウ", "奈良県", 34.34, 135.69, 110.0, "アメダス"),
                    // 和歌山県
                    Station("47777", "和歌山", "ワカヤマ", "和歌山県", 34.23, 135.17, 14.0, "官署"),
                    Station("47778", "潮岬", "シオノミサキ", "和歌山県", 33.45, 135.76, 68.0, "官署"),
                    Station("44281", "田辺", "タナベ", "和歌山県", 33.74, 135.39, 12.0, "アメダス"),
                    // 鳥取県
                    Station("47746", "鳥取", "トットリ", "鳥取県", 35.53, 134.22, 7.0, "官署"),
                    Station("47744", "米子", "ヨナゴ", "鳥取県", 35.43, 133.35, 7.0, "官署"),
                    Station("47740", "境", "サカイ", "鳥取県", 35.54, 133.23, 2.0, "官署"),
                    // 島根県
                    Station("47741", "松江", "マツエ", "島根県", 35.45, 133.07, 17.0, "官署"),
                    Station("47755", "浜田", "ハマダ", "島根県", 34.90, 132.08, 20.0, "官署"),
                    Station("47742", "西郷", "サイゴウ", "島根県", 36.20, 133.33, 27.0, "官署"),
                    // 岡山県
                    Station("47768", "岡山", "オカヤマ", "岡山県", 34.66, 133.92, 5.0, "官署"),
                    Station("47756", "津山", "ツヤマ", "岡山県", 35.07, 134.02, 140.0, "官署"),
                    Station("44226", "笠岡", "カサオカ", "岡山県", 34.50, 133.50, 4.0, "アメダス"),
                    // 広島県
                    Station("47765", "広島", "ヒロシマ", "広島県", 34.40, 132.46, 4.0, "官署"),
                    Station("47767", "呉", "クレ", "広島県", 34.25, 132.55, 4.0, "官署"),
                    Station("47771", "福山", "フクヤマ", "広島県", 34.49, 133.37, 3.0, "官署"),
                    // 山口県
                    Station("47762", "下関", "シモノセキ", "山口県", 33.96, 130.93, 2.0, "官署"),
                    Station("47761", "山口", "ヤマグチ", "山口県", 34.16, 131.46, 13.0, "官署"),
                    Station("47757", "萩", "ハギ", "山口県", 34.41, 131.40, 4.0, "官署"),
                    // 徳島県
                    Station("47895", "徳島", "トクシマ", "徳島県", 34.07, 134.56, 2.0, "官署"),
                    Station("44056", "美馬", "ミマ", "徳島県", 34.06, 134.16, 85.0, "アメダス"),
                    Station("44111", "阿波池田", "アワイケダ", "徳島県", 34.03, 133.80, 89.0, "アメダス"),
                    // 香川県
                    Station("47891", "高松", "タカマツ", "香川県", 34.32, 134.05, 9.0, "官署"),
                    Station("47892", "多度津", "タドツ", "香川県", 34.27, 133.76, 5.0, "官署"),
                    Station("44131", "内海", "ウツミ", "香川県", 34.48, 134.30, 5.0, "アメダス"),
                    // 愛媛県
                    Station("47887", "松山", "マツヤマ", "愛媛県", 33.84, 132.79, 32.0, "官署"),
                    Station("47897", "宇和島", "ウワジマ", "愛媛県", 33.22, 132.56, 4.0, "官署"),
                    Station("44111", "新居浜", "ニイハマ", "愛媛県", 33.96, 133.28, 19.0, "アメダス"),
                    // 高知県
                    Station("47893", "高知", "コウチ", "高知県", 33.56, 133.53, 2.0, "官署"),
                    Station("47899", "室戸岬", "ムロトミサキ", "高知県", 33.25, 134.18, 185.0, "官署"),
                    Station("47898", "清水", "シミズ", "高知県", 32.78, 132.96, 31.0, "官署"),
                    // 福岡県
                    Station("47807", "福岡", "フクオカ", "福岡県", 33.58, 130.37, 14.0, "官署"),
                    Station("47809", "飯塚", "イイヅカ", "福岡県", 33.64, 130.69, 38.0, "官署"),
                    Station("44211", "久留米", "クルメ", "福岡県", 33.32, 130.50, 11.0, "アメダス"),
                    // 佐賀県
                    Station("47813", "佐賀", "サガ", "佐賀県", 33.26, 130.30, 6.0, "官署"),
                    Station("44051", "唐津", "カラツ", "佐賀県", 33.45, 129.97, 4.0, "アメダス"),
                    Station("44111", "伊万里", "イマリ", "佐賀県", 33.27, 129.83, 10.0, "アメダス"),
                    // 長崎県
                    Station("47817", "長崎", "ナガサキ", "長崎県", 32.74, 129.87, 27.0, "官署"),
                    Station("47812", "佐世保", "サセボ", "長崎県", 33.16, 129.72, 4.0, "官署"),
                    Station("47800", "厳原", "イズハラ", "長崎県", 34.20, 129.29, 6.0, "官署"),
                    // 熊本県
                    Station("47819", "熊本", "クマモト", "熊本県", 32.81, 130.71, 38.0, "官署"),
                    Station("47821", "阿蘇山", "アソサン", "熊本県", 32.88, 131.04, 1142.0, "官署"),
                    Station("47822", "人吉", "ヒトヨシ", "熊本県", 32.21, 130.76, 107.0, "官署"),
                    // 大分県
                    Station("47815", "大分", "オオイタ", "大分県", 33.24, 131.62, 5.0, "官署"),
                    Station("47824", "日田", "ヒタ", "大分県", 33.32, 130.94, 83.0, "官署"),
                    Station("44051", "中津", "ナカツ", "大分県", 33.60, 131.18, 5.0, "アメダス"),
                    // 宮崎県
                    Station("47830", "宮崎", "ミヤザキ", "宮崎県", 31.91, 131.42, 7.0, "官署"),
                    Station("47829", "延岡", "ノベオカ", "宮崎県", 32.58, 131.67, 37.0, "官署"),
                    Station("47831", "都城", "ミヤコノジョウ", "宮崎県", 31.72, 131.06, 154.0, "官署"),
                    // 鹿児島県
                    Station("47827", "鹿児島", "カゴシマ", "鹿児島県", 31.56, 130.55, 4.0, "官署"),
                    Station("47909", "名瀬", "ナゼ", "鹿児島県", 28.38, 129.50, 3.0, "官署"),
                    Station("47837", "種子島", "タネガシマ", "鹿児島県", 30.73, 130.98, 17.0, "官署"),
                    // 沖縄県
                    Station("47936", "那覇", "ナハ", "沖縄県", 26.20, 127.68, 8.0, "官署"),
                    Station("47918", "石垣島", "イシガキジマ", "沖縄県", 24.34, 124.16, 6.0, "官署"),
                    Station("47927", "宮古島", "ミヤコジマ", "沖縄県", 24.79, 125.28, 40.0, "官署")
                )
                repository.insertStations(defaultStations)
            }
            
            val prefs = repository.getActivePrefectures()
            val sortedPrefs = prefs.sortedBy { PREFECTURE_ORDER.indexOf(it).let { index -> if (index == -1) 999 else index } }
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
