package com.example.amedasarchive.data.local.dao

import androidx.room.*
import com.example.amedasarchive.data.local.entity.StationEntity

@Dao
interface StationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stations: List<StationEntity>)

    @Query("SELECT * FROM stations ORDER BY prefecture, name ASC")
    suspend fun getAllStations(): List<StationEntity>

    @Query("SELECT * FROM stations WHERE prefecture = :prefecture ORDER BY name ASC")
    suspend fun getStationsByPrefecture(prefecture: String): List<StationEntity>

    @Query("SELECT DISTINCT prefecture FROM stations ORDER BY prefecture ASC")
    suspend fun getAllPrefectures(): List<String>

    @Query("SELECT * FROM stations WHERE stationId = :stationId LIMIT 1")
    suspend fun getStationById(stationId: String): StationEntity?
    
    @Query("DELETE FROM stations")
    suspend fun clearAll()
}
