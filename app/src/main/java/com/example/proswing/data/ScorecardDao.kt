package com.example.proswing.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScorecardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scorecard: ScorecardEntity)

    @Query("SELECT * FROM scorecard_table ORDER BY id DESC")
    fun getAllScorecards(): Flow<List<ScorecardEntity>>

    @Delete
    suspend fun delete(scorecard: ScorecardEntity)
}
