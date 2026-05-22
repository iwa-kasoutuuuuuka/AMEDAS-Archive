package com.example.amedasarchive.data.local.dao

import androidx.room.*
import com.example.amedasarchive.data.local.entity.SyncLogEntity

@Dao
interface SyncLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(syncLog: SyncLogEntity)

    @Query("SELECT * FROM sync_logs WHERE stationId = :stationId LIMIT 1")
    suspend fun getSyncLog(stationId: String): SyncLogEntity?

    @Query("DELETE FROM sync_logs WHERE stationId = :stationId")
    suspend fun deleteSyncLog(stationId: String)
}
