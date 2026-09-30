package com.example.ganggreen.theme

import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.shape.CutCornerShape

import androidx.compose.ui.unit.dp
import android.os.Build

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ganggreen.data.AppTheme

private val HomeTheme = darkColorScheme(
    primary = JetsGreenLight,
    secondary = JetsGreen,
    tertiary = StatusRed,
    background = DarkGreenBackground,
    surface = DarkGreenSurface,
    surfaceVariant = DarkGreenSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)


private val AlternateTheme = darkColorScheme(
    primary = JetsGreenLight,
    secondary = JetsGreen,
    tertiary = StatusRed,
    background = Color.Black,
    surface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFF2D2D2D),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray
)

private val ClassicTheme = darkColorScheme(

    primary = Color(0xFF00A859),

    secondary = Color(0xFF008040),

    tertiary = StatusRed,

    background = Color(0xFF0C2B1B),

    surface = Color(0xFF0A2215),

    surfaceVariant = Color(0xFF133E27),

    onPrimary = Color.White,

    onSecondary = Color.White,

    onBackground = Color.White,

    onSurface = Color.White,

    onSurfaceVariant = Color.LightGray

)

private val AflTheme = darkColorScheme(
    primary = Color(0xFFC5B358), // Old Gold
    secondary = Color(0xFFB1A04F),
    tertiary = StatusRed,
    background = Color(0xFF002244), // Titans Navy
    surface = Color(0xFF003060),
    surfaceVariant = Color(0xFF004080),
    onPrimary = Color(0xFF002244),
    onSecondary = Color(0xFF002244),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray
)

private val RivalryTheme = darkColorScheme(
    primary = JetsGreenLight, 
    secondary = Color(0xFFA0AAB2), 
    tertiary = Color(0xFF63666A), 
    background = Color(0xFF111111), 
    surface = Color(0xFF1E1E1E), 
    surfaceVariant = Color(0xFF2A2A2A),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray
)

@Composable
fun GangGreenTheme(
    appTheme: AppTheme = AppTheme.HOME,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (appTheme) {
        AppTheme.HOME -> HomeTheme
        AppTheme.ALTERNATE -> AlternateTheme
        AppTheme.CLASSIC -> ClassicTheme
        AppTheme.AFL -> AflTheme
        AppTheme.RIVALRY -> RivalryTheme
        AppTheme.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isSystemInDarkTheme()) HomeTheme else ClassicTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SportsTypography,
        shapes = SportsShapes,
        content = content
    )
}

val SportsTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black),
    displayMedium = Typography().displayMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black),
    displaySmall = Typography().displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold),
    titleLarge = Typography().titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    titleSmall = Typography().titleSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = Typography().bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = Typography().labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    labelMedium = Typography().labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    labelSmall = Typography().labelSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
)

val SportsShapes = Shapes(
    small = CutCornerShape(4.dp),
    medium = CutCornerShape(8.dp),
    large = CutCornerShape(12.dp)
)
