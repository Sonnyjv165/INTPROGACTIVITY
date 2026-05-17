package com.example.intprogactivity.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.example.intprogactivity.domain.model.FlightOffer
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isValidPassword(): Boolean =
    length >= 8 && any { it.isUpperCase() } && any { it.isDigit() }

fun String.toLocalDateTime(): LocalDateTime? = try {
    ZonedDateTime.parse(this).toLocalDateTime()
} catch (_: Exception) {
    try {
        LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    } catch (_: Exception) {
        null
    }
}

fun String.formatDisplayTime(): String {
    val dt = toLocalDateTime() ?: return this
    return dt.format(DateTimeFormatter.ofPattern("HH:mm"))
}

fun String.formatDisplayDate(): String {
    val dt = toLocalDateTime() ?: return this
    return dt.format(DateTimeFormatter.ofPattern("d MMM, EEE", Locale.ENGLISH))
}

fun String.formatDisplayDateTime(): String {
    val dt = toLocalDateTime() ?: return this
    return dt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH))
}

fun String.isoDurationToReadable(): String {
    val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?")
    val match = regex.find(this) ?: return this
    val hours = match.groupValues[1].toIntOrNull() ?: 0
    val minutes = match.groupValues[2].toIntOrNull() ?: 0
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

fun Double.formatPrice(currency: String = "PHP"): String =
    String.format(Locale.getDefault(), "%s %,.0f", currency, this)

fun String.toDouble(): Double = try {
    java.lang.Double.parseDouble(this)
} catch (_: Exception) {
    0.0
}

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun Fragment.hideKeyboard() {
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(view?.windowToken, 0)
}

fun FlightOffer.isDepartingWithin2Hours(): Boolean {
    val firstDeparture = itineraries
        .firstOrNull()
        ?.segments
        ?.firstOrNull()
        ?.departure
        ?.at
        ?.toLocalDateTime() ?: return false
    val hoursUntilDeparture = ChronoUnit.HOURS.between(LocalDateTime.now(), firstDeparture)
    return hoursUntilDeparture < Constants.MIN_BOOKING_HOURS_ADVANCE
}

fun FlightOffer.containsRyanair(): Boolean =
    validatingAirlineCodes.contains(Constants.RYANAIR_IATA_CODE)

fun Long.toDisplayDate(): String {
    val dt = LocalDateTime.ofEpochSecond(this / 1000, 0, java.time.ZoneOffset.UTC)
    return dt.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
}
