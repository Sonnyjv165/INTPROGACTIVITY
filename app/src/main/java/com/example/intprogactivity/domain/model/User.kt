package com.example.intprogactivity.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String? = null,
    val photoUrl: String? = null,
    val membershipTier: MembershipTier = MembershipTier.SILVER,
    val tripCoins: Int = 0,
    val totalBookings: Int = 0,
    val totalSpend: Double = 0.0,
    val tierExpiryDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MembershipTier {
    GUEST, SILVER, GOLD, PLATINUM, DIAMOND, DIAMOND_PLUS, BLACK_DIAMOND;

    fun coinMultiplier(): Double = when (this) {
        GUEST -> 0.0
        SILVER -> 1.0
        GOLD -> 1.2
        PLATINUM -> 1.5
        DIAMOND -> 2.0
        DIAMOND_PLUS, BLACK_DIAMOND -> 2.5
    }

    fun displayName(): String = when (this) {
        GUEST -> "Guest"
        SILVER -> "Silver Member"
        GOLD -> "Gold Member"
        PLATINUM -> "Platinum Member"
        DIAMOND -> "Diamond Member"
        DIAMOND_PLUS -> "Diamond+ Member"
        BLACK_DIAMOND -> "Black Diamond"
    }

    fun colorRes(): Int = when (this) {
        GUEST -> com.example.intprogactivity.R.color.tier_silver
        SILVER -> com.example.intprogactivity.R.color.tier_silver
        GOLD -> com.example.intprogactivity.R.color.tier_gold
        PLATINUM -> com.example.intprogactivity.R.color.tier_platinum
        DIAMOND -> com.example.intprogactivity.R.color.tier_diamond
        DIAMOND_PLUS -> com.example.intprogactivity.R.color.tier_diamond_plus
        BLACK_DIAMOND -> com.example.intprogactivity.R.color.tier_black_diamond
    }

    fun bgColorRes(): Int = when (this) {
        GUEST -> com.example.intprogactivity.R.color.tier_silver_bg
        SILVER -> com.example.intprogactivity.R.color.tier_silver_bg
        GOLD -> com.example.intprogactivity.R.color.tier_gold_bg
        PLATINUM -> com.example.intprogactivity.R.color.tier_platinum_bg
        DIAMOND -> com.example.intprogactivity.R.color.tier_diamond_bg
        DIAMOND_PLUS -> com.example.intprogactivity.R.color.tier_diamond_plus_bg
        BLACK_DIAMOND -> com.example.intprogactivity.R.color.tier_black_diamond_bg
    }

    companion object {
        fun fromString(value: String): MembershipTier =
            entries.find { it.name == value } ?: SILVER

        /**
         * Evaluates tier upgrade based on booking history.
         * BLACK_DIAMOND can only be granted by admin — never returned here.
         */
        fun evaluate(
            currentTier: MembershipTier,
            totalBookings: Int,
            bookingsInLastYear: Int,
            totalSpend: Double,
            spendInLastYear: Double
        ): MembershipTier {
            if (currentTier == BLACK_DIAMOND) return BLACK_DIAMOND
            return when {
                spendInLastYear >= 10_000.0 && currentTier == DIAMOND -> DIAMOND_PLUS
                bookingsInLastYear >= 8 && spendInLastYear >= 1_000.0 && currentTier == PLATINUM -> DIAMOND
                bookingsInLastYear >= 3 && currentTier == GOLD -> PLATINUM
                totalBookings >= 1 && currentTier == SILVER -> GOLD
                else -> currentTier
            }
        }
    }
}
