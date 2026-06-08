package dev.peterbot.auranotes.ui.theme

import androidx.compose.ui.graphics.Color

// --- Aura Notes brand palette -------------------------------------------------
// The four blues from the project's colour scheme. Also used to colour-code
// categories (see CategoryUi) and the favorite star.
val BrandBlueDark = Color(0xFF2C5EAD)
val BrandBlue = Color(0xFF1591DC)
val BrandBlueLight = Color(0xFF4BB8FA)
val BrandBluePale = Color(0xFFC4E2F5)

// --- Light theme --------------------------------------------------------------
val LightPrimary = BrandBlue
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = BrandBluePale
val LightOnPrimaryContainer = Color(0xFF06324A)
val LightSecondary = BrandBlueDark
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD7E3FF)
val LightOnSecondaryContainer = Color(0xFF001A41)
val LightTertiary = BrandBlueLight
val LightOnTertiary = Color(0xFF00344C)
val LightTertiaryContainer = Color(0xFFCDE7FF)
val LightOnTertiaryContainer = Color(0xFF001E2E)
val LightBackground = Color(0xFFF7FAFD)
val LightOnBackground = Color(0xFF191C1E)
val LightSurface = Color(0xFFF7FAFD)
val LightOnSurface = Color(0xFF191C1E)
val LightSurfaceVariant = Color(0xFFDDE3EA)
val LightOnSurfaceVariant = Color(0xFF41484D)
val LightOutline = Color(0xFF71787E)
val LightOutlineVariant = Color(0xFFC1C7CE)

/** Soft light-blue fill for note cards, so they lift off the near-white page. */
val CardLight = Color(0xFFE7F2FC)

// --- Dark theme ---------------------------------------------------------------
val DarkPrimary = BrandBlueLight
val DarkOnPrimary = Color(0xFF003351)
val DarkPrimaryContainer = BrandBlueDark
val DarkOnPrimaryContainer = BrandBluePale
val DarkSecondary = Color(0xFFA9C7FF)
val DarkOnSecondary = Color(0xFF002E69)
val DarkSecondaryContainer = Color(0xFF1B4493)
val DarkOnSecondaryContainer = Color(0xFFD7E3FF)
val DarkTertiary = Color(0xFF8DCFFF)
val DarkOnTertiary = Color(0xFF00344C)
val DarkTertiaryContainer = Color(0xFF1591DC)
val DarkOnTertiaryContainer = Color(0xFFCDE7FF)
val DarkBackground = Color(0xFF101417)
val DarkOnBackground = Color(0xFFE2E2E6)
val DarkSurface = Color(0xFF101417)
val DarkOnSurface = Color(0xFFE2E2E6)
val DarkSurfaceVariant = Color(0xFF41484D)
val DarkOnSurfaceVariant = Color(0xFFC1C7CE)
val DarkOutline = Color(0xFF8B9198)
val DarkOutlineVariant = Color(0xFF41484D)
