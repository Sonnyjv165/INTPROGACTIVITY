package com.example.intprogactivity.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    // Split name fields — matches web Firestore schema
    val firstName: String = "",
    val lastName: String = "",
    val middleInitial: String = "",
    val suffix: String = "",
    val displayName: String = "",       // "firstName lastName" — kept for convenience + cross-platform
    val phone: String? = null,
    val photoUrl: String? = null,
    val nationality: String? = null,
    val dob: String? = null,            // "yyyy-MM-dd" — matches web field name "dob"
    val role: String = "user",          // matches web field
    val status: String = "ACTIVE",      // matches web field
    val providerId: String? = null,     // matches web field (null for email auth)
    val membershipTier: MembershipTier = MembershipTier.SILVER,
    val loyaltyPoints: Int = 0,         // matches web field "loyaltyPoints" (was tripCoins)
    val totalBookings: Int = 0,
    val confirmedBookings: Int = 0,   // only CONFIRMED bookings — used for tier evaluation
    val totalSpend: Double = 0.0,
    val tierExpiryDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Convenience: returns "First Last" or displayName, whichever is populated. */
    fun fullName(): String = displayName.ifBlank { "$firstName $lastName".trim() }
}

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

    fun discountPercent(): Double = when (this) {
        GUEST, SILVER -> 0.0
        GOLD -> 0.05
        PLATINUM -> 0.10
        DIAMOND -> 0.15
        DIAMOND_PLUS -> 0.20
        BLACK_DIAMOND -> 0.25
    }

    fun discountLabel(): String = when (this) {
        GUEST, SILVER -> "No discount"
        GOLD -> "5% off flights"
        PLATINUM -> "10% off flights"
        DIAMOND -> "15% off flights"
        DIAMOND_PLUS -> "20% off flights"
        BLACK_DIAMOND -> "25% off flights"
    }

    fun benefits(): List<String> = when (this) {
        GUEST -> listOf("Register to start earning Trip Coins")
        SILVER -> listOf(
            "Earn 1 coin per ₱20 spent",
            "Standard booking access",
            "Coins never expire"
        )
        GOLD -> listOf(
            "5% discount on all flights",
            "1.2× Trip Coin multiplier",
            "Priority email support"
        )
        PLATINUM -> listOf(
            "10% discount on all flights",
            "1.5× Trip Coin multiplier",
            "Priority phone support",
            "Free standard seat selection"
        )
        DIAMOND -> listOf(
            "15% discount on all flights",
            "2.0× Trip Coin multiplier",
            "Free seat selection",
            "Airport lounge access",
            "Dedicated support line"
        )
        DIAMOND_PLUS -> listOf(
            "20% discount on all flights",
            "2.5× Trip Coin multiplier",
            "Complimentary seat upgrade",
            "Lounge + guest access",
            "Personal travel agent"
        )
        BLACK_DIAMOND -> listOf(
            "25% discount on all flights",
            "2.5× Trip Coin multiplier",
            "First class upgrade priority",
            "Unlimited lounge access",
            "24/7 dedicated concierge"
        )
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
            confirmedBookings: Int,
            bookingsInLastYear: Int,
            totalSpend: Double,
            spendInLastYear: Double
        ): MembershipTier {
            if (currentTier == BLACK_DIAMOND) return BLACK_DIAMOND
            return when {
                spendInLastYear >= 2_000_000.0 && currentTier == DIAMOND -> DIAMOND_PLUS
                bookingsInLastYear >= 25 && spendInLastYear >= 500_000.0 && currentTier == PLATINUM -> DIAMOND
                bookingsInLastYear >= 10 && spendInLastYear >= 50_000.0 && currentTier == GOLD -> PLATINUM
                confirmedBookings >= 5 && currentTier == SILVER -> GOLD
                else -> currentTier
            }
        }
    }
}
