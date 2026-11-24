package xyz.bi7khi.inventree.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Field-style palette, inspired by the industrial-editorial language of
 * near-future sci-fi interfaces. No official artwork, logos or fonts are used.
 *
 * The accent is a signal, not a background wash: most of the screen stays in
 * warm paper / carbon black, while one saturated colour marks the active thing.
 */

val SignalYellow = Color(0xFFF2E900)
val SignalYellowDim = Color(0xFFD8CF00)
val SignalYellowSoft = Color(0x33F2E900)
val SignalInk = Color(0xFF121400)

val IndustrialCyan = Color(0xFF19D3D3)
val IndustrialCyanDim = Color(0xFF007A7A)
val IndustrialCyanSoft = Color(0x2619D3D3)

val EmberRed = Color(0xFFFF4B32)
val EmberRedDim = Color(0xFFB32414)

val FieldPaper = Color(0xFFE9E7DD)
val FieldPaperLight = Color(0xFFF4F2E9)
val FieldPaperDeep = Color(0xFFDCD9CE)
val FieldInk = Color(0xFF11130F)
val FieldCarbon = Color(0xFF0A0C0A)
val FieldCarbonRaised = Color(0xFF141813)
val FieldLine = Color(0xFFBFC0B5)
val FieldLineDark = Color(0xFF343A32)

// Names kept for existing call sites; values now follow the field palette.
val BrandGreen = SignalYellowDim
val BrandGreenDark = FieldCarbon
val BrandMint = IndustrialCyan
val BrandMintSoft = Color(0xFFC9F5EF)

val LightPrimary = Color(0xFF595200)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = SignalYellow
val LightOnPrimaryContainer = SignalInk
val LightSecondary = Color(0xFF006666)
val LightSecondaryContainer = Color(0xFFC9F3EE)
val LightTertiary = EmberRedDim
val LightTertiaryContainer = Color(0xFFFFDAD1)
val LightBackground = FieldPaper
val LightSurface = FieldPaperLight
val LightSurfaceVariant = FieldPaperDeep
val LightOutline = Color(0xFF74776D)
val LightError = Color(0xFFBA1A1A)

val DarkPrimary = SignalYellow
val DarkOnPrimary = SignalInk
val DarkPrimaryContainer = Color(0xFF3D3900)
val DarkOnPrimaryContainer = Color(0xFFFFF77A)
val DarkSecondary = IndustrialCyan
val DarkSecondaryContainer = Color(0xFF003D3D)
val DarkTertiary = Color(0xFFFF8A72)
val DarkTertiaryContainer = Color(0xFF5A180B)
val DarkBackground = FieldCarbon
val DarkSurface = FieldCarbonRaised
val DarkSurfaceVariant = Color(0xFF20251F)
val DarkOutline = Color(0xFF62685E)
val DarkError = Color(0xFFFFB4AB)

val Success = Color(0xFF1F7A4D)
val Warning = Color(0xFFC47A00)
val Danger = Color(0xFFBA1A1A)
val SoftBlue = Color(0xFF326E8C)
