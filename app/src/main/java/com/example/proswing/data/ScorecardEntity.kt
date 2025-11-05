package com.example.proswing.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "scorecard_table")
@TypeConverters(ScorecardConverters::class)
data class ScorecardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val totalScore: Int,
    val totalPar: Int,
    val netScore: Int,
    val toPar: Int,
    val holes: List<Int>, // list of stroke counts
    val handicap: Int
)
