package com.hexis.bi.ui.theme

import androidx.compose.ui.graphics.Color

// === Figma Color Styles ===

val Black = Color(0xFF060611)

// Blue (100 = vivid primary, higher = darker)
val Blue100 = Color(0xFF005AFF)   // vivid primary — buttons, active states, highlights

val Blue200 = Color(0xFF001D99)   // deeper blue — strong backgrounds
val Blue300 = Color(0xFF0030FF)   // darkest blue

val BlueFadedIndicator300 = Color(0xFF4B6BFB)
val BlueFadedIndicator200 = Color(0xFFA8C5FF)
val BlueFadedIndicator100 = Color(0xFFD1E2FF)

// Gray (100 = darkest, 600 = lightest)
val Gray100 = Color(0xFF64748B)
val Gray200 = Color(0xFFAEB4BB)
val Gray300 = Color(0xFFF8F8F8)
val Gray400 = Color(0xFFEFF0F6)
val Gray500 = Color(0xFFEDF1F3)
val Gray600 = Color(0xFFF0F0F0)

val GrayText = Color(0xFF686868)

/** Muted card surface (scan history rows, error callouts); light-only — consider theme tokens for dark mode later. */
val HistoryCardBackground = Color(0xFFF3F3F3)
val Bg = Color(0xFFF7F7FA)
val White = Color(0xFFFFFFFF)

val ShadowColor = Color(0x33A7A7A7)

// Red
val Red100 = Color(0xFFFF3A3A)   // vivid red — errors, delete actions

//val Red200 = Color(0xFFFF7070)   // muted red
val Red300 = Color(0xFFE62020)   // deep red — destructive action buttons

// Lime
val Lime100 = Color(0xFFF2FF91)  // slightly muted lime
val Lime200 = Color(0xFFE8FE3F)  // bright lime — highlights, active tabs

// Other
val LightBlue = Color(0xFFDDE9F5) // light blue — secondary accents
val LightGradientBlue = Color(0xFF75A4FF)
val LightBlueBackground = Color(0xFFE6E6E6)

val Green = Color(0xFF0E8716)
val Yellow = Color(0xFFF2B705)

// Subtitle/description text (blue-purple muted — onSurfaceVariant)
val SubtitleBlue = Color(0xFF7272A8)

// Shadow
val ShadowGray = Color(0xFFC8C8C8)
val GridLineGray = Color(0x5EB5B5B5)
val GridLineLightGray = Color(0xFFEFF0F3)

val BodyToggleTrackBorder = Color(0x29F6F6F6)
val BodyToggleSelectedChipFill = Color(0x33FFFFFF)
val BodyToggleSelectedLabel = Color(0xFFF6F6F6)
val BodyToggleUnselectedLabel = Color(0xFFE7E7E7)

val DarkPrimaryButtonGradientTop = Color(0xA31DC4B3)
val DarkPrimaryButtonGradientBottom = Color(0xA30B2020)
val DarkPrimaryButtonDisabledFill = Color(0x66AEB4BB)

val DialogBackdrop = Color(0x40424242)
val DialogWindowBackground = Color(0xFF1A1A1A)

val GlassTrackFill = Color(0x0D090909)
val GlassSelectionFill = Color(0x4D090909)
val GlassRimHighlight = Color(0xFFE6FFFC)

val DarkSliderLabel = Color(0xFFF6F6F6)
val DarkSliderActiveTrack = Color(0xFF1DC4B3)
val DarkSliderInactiveTrack = Color(0x38797979)

/** Transparent edge stop (`rgba(189, 190, 192, 0)`) for gradient dividers. */
val GradientDividerEdge = Color(0x00BDBEC0)

/** Center band `#EFF0F3` (same value as [GridLineLightGray]). */
val GradientDividerCenter = GridLineLightGray
