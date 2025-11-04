package com.example.proswing.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GolfClubDao {

    @Query("SELECT * FROM golf_clubs")
    fun getAllClubs(): Flow<List<GolfClubEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClub(club: GolfClubEntity)

    @Delete
    suspend fun deleteClub(club: GolfClubEntity)

    @Query("UPDATE golf_clubs SET carryDistance = :carry, totalDistance = :total WHERE id = :clubId")
    suspend fun updateYardages(clubId: Int, carry: Float?, total: Float?)  // <-- use Float? here
}
