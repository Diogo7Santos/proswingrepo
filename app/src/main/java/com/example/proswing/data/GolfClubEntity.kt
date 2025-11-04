package com.example.proswing.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "golf_clubs")
data class GolfClubEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val variant: String?,
    val brand: String,
    val model: String,
    val carryDistance: Float? = null,   // <-- changed from Int? to Float?
    val totalDistance: Float? = null    // <-- changed from Int? to Float?
)

