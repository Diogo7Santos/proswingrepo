package com.example.proswing.data

import androidx.room.TypeConverter

class ScorecardConverters {
    @TypeConverter
    fun fromList(list: List<Int>): String = list.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<Int> =
        if (data.isEmpty()) emptyList()
        else data.split(",").map { it.toIntOrNull() ?: 0 }
}
