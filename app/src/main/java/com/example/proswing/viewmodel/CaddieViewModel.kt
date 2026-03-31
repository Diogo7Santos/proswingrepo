package com.example.proswing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proswing.data.GolfClubDao
import com.example.proswing.ui.screens.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class CaddieViewModel(
    private val golfClubDao: GolfClubDao
) : ViewModel() {

    private val _clubUiState = MutableStateFlow<List<SimpleClub>>(emptyList())
    val clubUiState: StateFlow<List<SimpleClub>> = _clubUiState.asStateFlow()

    private val _recommendation = MutableStateFlow<ClubRecommendationResult?>(null)
    val recommendation: StateFlow<ClubRecommendationResult?> = _recommendation.asStateFlow()

    init {
        observeClubs()
    }

    private fun observeClubs() {
        viewModelScope.launch {
            golfClubDao.getAllClubs().collectLatest { clubs ->
                _clubUiState.value = clubs.mapNotNull { entity ->
                    val carry = entity.carryDistance?.toInt()
                    if (carry == null || carry <= 0) {
                        null
                    } else {
                        SimpleClub(
                            id = entity.id,
                            displayName = buildDisplayName(entity.type, entity.variant),
                            type = entity.type,
                            carryDistance = carry,
                            totalDistance = entity.totalDistance?.toInt()
                        )
                    }
                }.sortedByDescending { it.carryDistance }
            }
        }
    }

    fun generateRecommendation(
        distanceToPin: Int,
        courseElement: CourseElement,
        windType: WindType,
        windStrength: Int,
        elevationType: ElevationType,
        lieType: LieType
    ) {
        val allClubs = _clubUiState.value

        if (distanceToPin <= 0 || allClubs.isEmpty()) {
            _recommendation.value = null
            return
        }

        val effectiveDistance = calculateEffectiveDistance(
            baseDistance = distanceToPin,
            windType = windType,
            windStrength = windStrength,
            elevationType = elevationType,
            lieType = lieType
        )

        val filteredClubs = filterClubsForElement(allClubs, courseElement, effectiveDistance)

        if (filteredClubs.isEmpty()) {
            _recommendation.value = ClubRecommendationResult(
                recommendedClub = "No suitable club",
                effectiveDistance = effectiveDistance,
                matchedCarryDistance = 0,
                reason = "No saved clubs match this course element."
            )
            return
        }

        val sortedByDifference = filteredClubs.sortedBy { abs(it.carryDistance - effectiveDistance) }

        val best = sortedByDifference.first()
        val alternative = sortedByDifference.getOrNull(1)

        _recommendation.value = ClubRecommendationResult(
            recommendedClub = best.displayName,
            alternativeClub = alternative?.displayName,
            effectiveDistance = effectiveDistance,
            matchedCarryDistance = best.carryDistance,
            reason = buildReason(
                baseDistance = distanceToPin,
                effectiveDistance = effectiveDistance,
                courseElement = courseElement,
                bestClub = best,
                windType = windType,
                elevationType = elevationType,
                lieType = lieType
            )
        )
    }

    private fun calculateEffectiveDistance(
        baseDistance: Int,
        windType: WindType,
        windStrength: Int,
        elevationType: ElevationType,
        lieType: LieType
    ): Int {
        var adjusted = baseDistance

        when (windType) {
            WindType.HEADWIND -> adjusted += windStrength
            WindType.TAILWIND -> adjusted -= windStrength
            WindType.CROSSWIND -> adjusted += 0
            WindType.NONE -> {}
        }

        when (elevationType) {
            ElevationType.UPHILL -> adjusted += 8
            ElevationType.DOWNHILL -> adjusted -= 8
            ElevationType.FLAT -> {}
        }

        when (lieType) {
            LieType.HEAVY_ROUGH -> adjusted += 6
            LieType.DIVOT -> adjusted += 4
            LieType.BALL_BELOW_FEET -> adjusted += 3
            LieType.BALL_ABOVE_FEET -> adjusted -= 2
            LieType.NORMAL -> {}
        }

        return adjusted.coerceAtLeast(1)
    }

    private fun filterClubsForElement(
        clubs: List<SimpleClub>,
        element: CourseElement,
        effectiveDistance: Int
    ): List<SimpleClub> {
        return when (element) {
            CourseElement.GREEN -> {
                clubs.filter { isPutter(it) }
            }

            CourseElement.FRINGE -> {
                if (effectiveDistance <= 25) {
                    clubs.filter { isPutter(it) || isWedge(it) }
                } else {
                    clubs.filter { isWedge(it) || isShortIron(it) }
                }
            }

            CourseElement.BUNKER -> {
                when {
                    effectiveDistance <= 40 -> clubs.filter { isLobWedge(it) || isSandWedge(it) || isGapWedge(it) }
                    effectiveDistance <= 90 -> clubs.filter { isSandWedge(it) || isGapWedge(it) || isPitchingWedge(it) }
                    else -> clubs.filter { isPitchingWedge(it) || isNineIron(it) || isEightIron(it) }
                }
            }

            CourseElement.ROUGH -> {
                clubs.filterNot { isPutter(it) }
            }

            CourseElement.FAIRWAY -> {
                clubs.filterNot { isPutter(it) }
            }

            CourseElement.TEE_BOX -> {
                clubs.filterNot { isPutter(it) }
            }
        }
    }

    private fun buildReason(
        baseDistance: Int,
        effectiveDistance: Int,
        courseElement: CourseElement,
        bestClub: SimpleClub,
        windType: WindType,
        elevationType: ElevationType,
        lieType: LieType
    ): String {
        return "Base distance $baseDistance yds adjusted to $effectiveDistance yds " +
                "for ${windType.label.lowercase()}, ${elevationType.label.lowercase()}, and ${lieType.label.lowercase()}. " +
                "${bestClub.displayName} is the closest match from your saved carry distances for a ${courseElement.label.lowercase()} shot."
    }

    private fun buildDisplayName(type: String, variant: String?): String {
        return if (variant.isNullOrBlank()) type else "$type ${variant.trim()}"
    }

    private fun isPutter(club: SimpleClub): Boolean =
        club.type.contains("putter", ignoreCase = true)

    private fun isWedge(club: SimpleClub): Boolean =
        club.type.contains("wedge", ignoreCase = true) ||
                club.type.equals("PW", ignoreCase = true) ||
                club.type.equals("GW", ignoreCase = true) ||
                club.type.equals("SW", ignoreCase = true) ||
                club.type.equals("LW", ignoreCase = true)

    private fun isLobWedge(club: SimpleClub): Boolean =
        club.type.equals("LW", ignoreCase = true) ||
                club.displayName.contains("lob", ignoreCase = true)

    private fun isSandWedge(club: SimpleClub): Boolean =
        club.type.equals("SW", ignoreCase = true) ||
                club.displayName.contains("sand", ignoreCase = true)

    private fun isGapWedge(club: SimpleClub): Boolean =
        club.type.equals("GW", ignoreCase = true) ||
                club.displayName.contains("gap", ignoreCase = true)

    private fun isPitchingWedge(club: SimpleClub): Boolean =
        club.type.equals("PW", ignoreCase = true) ||
                club.displayName.contains("pitch", ignoreCase = true)

    private fun isNineIron(club: SimpleClub): Boolean =
        club.type.equals("9I", ignoreCase = true) ||
                club.displayName.contains("9", ignoreCase = true)

    private fun isEightIron(club: SimpleClub): Boolean =
        club.type.equals("8I", ignoreCase = true) ||
                club.displayName.contains("8", ignoreCase = true)

    private fun isShortIron(club: SimpleClub): Boolean =
        club.type.equals("8I", ignoreCase = true) ||
                club.type.equals("9I", ignoreCase = true) ||
                isPitchingWedge(club)
}

class CaddieViewModelFactory(
    private val golfClubDao: GolfClubDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaddieViewModel::class.java)) {
            return CaddieViewModel(golfClubDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}