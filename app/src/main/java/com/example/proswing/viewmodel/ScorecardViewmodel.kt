package com.example.proswing.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.proswing.data.AppDatabase
import com.example.proswing.data.ScorecardEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ✅ Define Hole type here so it's known to the ViewModel
data class Hole(
    val number: Int,
    val par: Int,
    val strokes: Int?
)

class ScorecardViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).scorecardDao()

    // Collect saved rounds as StateFlow
    val savedRounds = dao.getAllScorecards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Save a new round to the database
    fun saveRound(holes: List<Hole>, handicap: Int) {
        viewModelScope.launch {
            // Defensive check
            if (holes.isEmpty()) return@launch

            // Calculate totals
            val totalScore = holes.sumOf { it.strokes ?: it.par }
            val totalPar = holes.sumOf { it.par }
            val toPar = totalScore - totalPar
            val netScore = totalScore - handicap

            // Build entity
            val scorecard = ScorecardEntity(
                totalScore = totalScore,
                totalPar = totalPar,
                netScore = netScore,
                toPar = toPar,
                holes = holes.map { it.strokes ?: it.par },
                pars = holes.map { it.par },
                handicap = handicap
            )

            dao.insert(scorecard)
        }
    }
}
