package com.example.amedasarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 各観測地点のデータ同期履歴を管理するエンティティ
 * 差分ダウンロードの実装に使用
 */
@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey
    val stationId: String,          // 地点コード
    val lastSyncedDate: String,     // 最終同期済みの日付（フォーマット: YYYY-MM-DD）
    val updatedAt: Long             // 同期処理が実行されたタイムスタンプ（ミリ秒）
)
