package com.example.proswing.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GolfClubDao {

    // Get all clubs as a Flow (live updates)
    @Query("SELECT * FROM golf_clubs")
    fun getAllClubs(): Flow<List<GolfClubEntity>>

    // Insert a new club (replace if same ID)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClub(club: GolfClubEntity)

    // Delete a club
    @Delete
    suspend fun deleteClub(club: GolfClubEntity)

    // Update yardages for a specific club
    @Query("UPDATE golf_clubs SET carryDistance = :carry, totalDistance = :total WHERE id = :clubId")
    suspend fun updateYardages(clubId: Int, carry: Int?, total: Int?)
}
