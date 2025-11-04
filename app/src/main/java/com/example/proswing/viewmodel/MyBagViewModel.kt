package com.example.proswing.viewmodel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.proswing.data.AppDatabase
import com.example.proswing.data.GolfClubEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyBagViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).golfClubDao()

    val clubs = dao.getAllClubs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyList()
    )

    fun addClub(type: String, variant: String?, brand: String, model: String) {
        viewModelScope.launch {
            dao.insertClub(
                GolfClubEntity(
                    type = type,
                    variant = variant,
                    brand = brand,
                    model = model
                )
            )
        }
    }

    fun updateYardages(clubId: Int, carry: Int?, total: Int?) {
        viewModelScope.launch {
            dao.updateYardages(clubId, carry, total)
        }
    }

    fun deleteClub(club: GolfClubEntity) {
        viewModelScope.launch {
            dao.deleteClub(club)
        }
    }
}
