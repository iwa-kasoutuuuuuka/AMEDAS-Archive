package com.example.amedasarchive.data.remote

import com.example.amedasarchive.data.local.entity.DailyWeatherEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * 気象庁が提供する過去気象データCSVのストリームパーサー
 * メモリ負荷を抑えるため、ファイル全体を一括ロードせず、ストリームから1行ずつ読み込みます。
 */
class AmedasCsvParser {

    /**
     * CSVデータを解析し、Room用のDailyWeatherEntityのリストに変換します。
     * @param inputStream CSVファイルのインプットストリーム
     * @param stationId 解析対象の観測所ID
     */
    fun parse(inputStream: InputStream, stationId: String): List<DailyWeatherEntity> {
        val weatherList = mutableListOf<DailyWeatherEntity>()
        // 気象庁のデータは標準的に Shift_JIS（Windows-31J / MS932）でエンコードされています
        val reader = BufferedReader(InputStreamReader(inputStream, Charset.forName("Shift_JIS")))

        try {
            var line: String?
            var isDataHeaderReached = false
            var dataStartIndex = 0
            var lineCount = 0

            while (reader.readLine().also { line = it } != null) {
                lineCount++
                val currentLine = line ?: break

                // 気象庁CSV特有の挙動に対応: 
                // 「ダウンロードされたCSVファイル」は通常、ヘッダーに複数の不要な説明行、項目名行、品質パラメータ行を含みます。
                // 「年月日」あるいは「日付」というキーワードを含む行が項目定義ヘッダーです。
                if (!isDataHeaderReached) {
                    if (currentLine.contains("年月日") || currentLine.contains("日付")) {
                        isDataHeaderReached = true
                        // ヘッダー直下の品質情報行や均質番号行（通常は項目ヘッダーの2行下までがメタデータ）をスキップするため、
                        // データ読み込みの開始行数を設定
                        dataStartIndex = lineCount + 2 
                    }
                    continue
                }

                // データ開始インデックスに到達するまではスキップ
                if (lineCount <= dataStartIndex) {
                    continue
                }

                // 空白行や不完全な行をスキップ
                val tokens = currentLine.split(",")
                if (tokens.size < 4) continue 

                // 1列目は日付（例: "2023/10/01" または "\"2023/10/01\""）
                val rawDate = tokens[0].trim().replace("\"", "")
                val formattedDate = normalizeDate(rawDate) ?: continue

                // CSV列の取得（気象庁ダウンロードの設定により順序が変わるため、基本構成として定義）
                // 2列目以降に平均気温、最高気温、最低気温、降水量、日照時間が入っていると仮定してマッピング
                val tempMean = tokens.getOrNull(1)?.trim()?.replace("\"", "")?.toFloatOrNull()
                val tempMax = tokens.getOrNull(2)?.trim()?.replace("\"", "")?.toFloatOrNull()
                val tempMin = tokens.getOrNull(3)?.trim()?.replace("\"", "")?.toFloatOrNull()
                val precipitation = tokens.getOrNull(4)?.trim()?.replace("\"", "")?.toFloatOrNull()
                val sunshine = tokens.getOrNull(5)?.trim()?.replace("\"", "")?.toFloatOrNull()

                weatherList.add(
                    DailyWeatherEntity(
                        stationId = stationId,
                        date = formattedDate,
                        temperatureMean = tempMean,
                        temperatureMax = tempMax,
                        temperatureMin = tempMin,
                        precipitation = precipitation,
                        sunshineHours = sunshine,
                        snowDepth = null,
                        humidityMean = null,
                        windSpeedMean = null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                reader.close()
            } catch (e: Exception) {
                // クローズ時の例外は無視
            }
        }
        return weatherList
    }

    /**
     * 様々な日付表記（"2023/10/1", "2023-10-01"など）を Room格納フォーマット "YYYY-MM-DD" に変換
     */
    private fun normalizeDate(rawDate: String): String? {
        val cleaned = rawDate.replace("/", "-")
        val parts = cleaned.split("-")
        if (parts.size != 3) return null
        
        val year = parts[0]
        val month = parts[1].padStart(2, '0')
        val day = parts[2].padStart(2, '0')
        
        return "$year-$month-$day"
    }
}
