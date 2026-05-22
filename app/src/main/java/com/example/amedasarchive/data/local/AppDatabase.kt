package com.example.amedasarchive.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.amedasarchive.data.local.dao.StationDao
import com.example.amedasarchive.data.local.dao.SyncLogDao
import com.example.amedasarchive.data.local.dao.WeatherDao
import com.example.amedasarchive.data.local.entity.DailyWeatherEntity
import com.example.amedasarchive.data.local.entity.StationEntity
import com.example.amedasarchive.data.local.entity.SyncLogEntity

/**
 * アプリケーションのプライマリRoomデータベース
 */
@Database(
    entities = [
        StationEntity::class,
        DailyWeatherEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun stationDao(): StationDao
    abstract fun weatherDao(): WeatherDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * データベースのシングルトンインスタンスを取得
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amedas_archive_database"
                )
                .fallbackToDestructiveMigration() // 破壊的マイグレーション（開発・OSS用）
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
