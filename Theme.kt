package com.nova.assistant.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Typography ────────────────────────────────────────
// Using system fonts; swap with custom font files in res/font/
val NovaFontFamily = FontFamily.Default

val NovaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NovaFontFamily,
        fontWeight = FontWeight.W300,
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp,
        color = NovaTextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = NovaFontFamily,
        fontWeight = FontWeight.W300,
        fontSize = 45.sp,
        color = NovaTextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = NovaFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
        color = NovaTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = NovaFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 24.sp,
        color = NovaTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = NovaFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        letterSpacing = 0.5.sp,
        color = NovaTextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = NovaFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = NovaTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = NovaFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = NovaTextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = NovaFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = NovaTextMuted
    )
)

// ── Dark Color Scheme ─────────────────────────────────
private val NovaDarkColorScheme = darkColorScheme(
    primary = NovaCyan,
    onPrimary = NovaBlack,
    primaryContainer = NovaCyanDim,
    onPrimaryContainer = NovaTextPrimary,
    secondary = NovaViolet,
    onSecondary = NovaTextPrimary,
    background = NovaBlack,
    onBackground = NovaTextPrimary,
    surface = NovaDarkSurface,
    onSurface = NovaTextPrimary,
    surfaceVariant = NovaCard,
    onSurfaceVariant = NovaTextSecondary,
    outline = NovaTextMuted,
    error = NovaRed,
    onError = NovaTextPrimary
)

@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NovaDarkColorScheme,
        typography = NovaTypography,
        content = content
    )
}
