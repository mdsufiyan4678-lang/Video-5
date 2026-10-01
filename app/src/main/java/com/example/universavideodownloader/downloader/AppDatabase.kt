package com.example.universavideodownloader.downloader

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus?): String {
        return status?.name ?: DownloadStatus.PREPARING.name
    }

    @TypeConverter
    fun toStatus(value: String?): DownloadStatus {
        return try {
            if (value != null) DownloadStatus.valueOf(value) else DownloadStatus.PREPARING
        } catch (_: Exception) {
            DownloadStatus.PREPARING
        }
    }
}

@Database(entities = [DownloadItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "universal_video_downloader.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
