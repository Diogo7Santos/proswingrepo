package com.example.proswing.ui.screens

enum class CourseElement(val label: String) {
    TEE_BOX("Tee Box"),
    FAIRWAY("Fairway"),
    ROUGH("Rough"),
    BUNKER("Bunker"),
    FRINGE("Fringe"),
    GREEN("Green")
}

enum class WindType(val label: String) {
    NONE("No Wind"),
    HEADWIND("Headwind"),
    TAILWIND("Tailwind"),
    CROSSWIND("Crosswind")
}

enum class ElevationType(val label: String) {
    FLAT("Flat"),
    UPHILL("Uphill"),
    DOWNHILL("Downhill")
}

enum class LieType(val label: String) {
    NORMAL("Normal"),
    HEAVY_ROUGH("Heavy Rough"),
    DIVOT("Divot"),
    BALL_ABOVE_FEET("Ball Above Feet"),
    BALL_BELOW_FEET("Ball Below Feet")
}

data class SimpleClub(
    val id: Int,
    val displayName: String,
    val type: String,
    val carryDistance: Int,
    val totalDistance: Int?
)

data class ClubRecommendationResult(
    val recommendedClub: String,
    val alternativeClub: String? = null,
    val effectiveDistance: Int,
    val matchedCarryDistance: Int,
    val reason: String
)