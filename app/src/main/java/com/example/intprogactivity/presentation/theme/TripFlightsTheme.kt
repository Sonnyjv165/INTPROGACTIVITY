package com.example.intprogactivity.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandPrimary = Color(0xFF006CE4)
val BrandPrimaryDark = Color(0xFF0052B0)
val BrandPrimaryContainer = Color(0xFFD6E8FF)
val CtaOrange = Color(0xFFFF6B00)
val CtaOrangeLight = Color(0xFFFFF0E6)
val AppBackground = Color(0xFFF0F2F5)
val AppSurface = Color(0xFFFFFFFF)
val AppSurfaceVariant = Color(0xFFF5F7FA)
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF6B7280)
val TextHint = Color(0xFF9CA3AF)
val AppSuccess = Color(0xFF10B981)
val AppWarning = Color(0xFFF59E0B)
val AppError = Color(0xFFEF4444)
val Divider = Color(0xFFE5E7EB)
val Border = Color(0xFFD1D5DB)

val TierSilver = Color(0xFF9CA3AF)
val TierGold = Color(0xFFF59E0B)
val TierPlatinum = Color(0xFF60A5FA)
val TierDiamond = Color(0xFFA78BFA)
val TierDiamondPlus = Color(0xFFF472B6)
val TierBlackDiamond = Color(0xFFFFFFFF)
val TierBlackDiamondBg = Color(0xFF1F2937)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandPrimaryDark,
    secondary = CtaOrange,
    onSecondary = Color.White,
    secondaryContainer = CtaOrangeLight,
    onSecondaryContainer = Color(0xFF7A3200),
    background = AppBackground,
    onBackground = TextPrimary,
    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = AppError,
    onError = Color.White,
    outline = Border,
    outlineVariant = Divider,
)

@Composable
fun TripFlightsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
