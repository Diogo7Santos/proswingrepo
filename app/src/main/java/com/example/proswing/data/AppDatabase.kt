package com.example.proswing.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [GolfClubEntity::class, ScorecardEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(ScorecardConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun golfClubDao(): GolfClubDao
    abstract fun scorecardDao(): ScorecardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "proswing_db"
                )
                    // Automatically rebuilds DB if schema changes
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
