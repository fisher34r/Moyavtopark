package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode(val title: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

enum class AppColorStyle(
    val title: String,
    val subtitle: String,
    val primaryColor: Color
) {
    AGRO_GREEN(
        title = "Агро-зелёный",
        subtitle = "Фирменный стиль перевозок зерна",
        primaryColor = Color(0xFF1B5E20)
    ),
    TRANSPORT_BLUE(
        title = "Транспортный синий",
        subtitle = "Классический реестр и логистика",
        primaryColor = Color(0xFF155383)
    ),
    AMBER_WHEAT(
        title = "Золотой колос",
        subtitle = "Тёплый янтарный урожай",
        primaryColor = Color(0xFFB45309)
    ),
    DARK_GRAPHITE(
        title = "Тёмный графит",
        subtitle = "Строгий индустриальный автопарк",
        primaryColor = Color(0xFF334155)
    )
}

// 1. Агро-зелёный
private val AgroGreenLight = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF024022),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0284C7),
    background = Color(0xFFF6F8F6),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9F0EC),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1)
)

private val AgroGreenDark = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF0F3822),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF633F00),
    onSecondaryContainer = Color(0xFFFFDEAC),
    tertiary = Color(0xFF38BDF8),
    background = Color(0xFF0F1713),
    surface = Color(0xFF16231D),
    surfaceVariant = Color(0xFF1E2F28),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF4B6358)
)

// 2. Транспортный синий (в точности как в референсе реестра)
private val TransportBlueLight = lightColorScheme(
    primary = Color(0xFF155383),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0D3759),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF10B981),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF2F6),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1)
)

private val TransportBlueDark = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF34D399),
    background = Color(0xFF0A0F1D),
    surface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

// 3. Золотой колос (Янтарь)
private val AmberWheatLight = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF15803D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF166534),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFFCFBF8),
    surface = Color.White,
    surfaceVariant = Color(0xFFF7F3E9),
    onSurface = Color(0xFF1C1917),
    onSurfaceVariant = Color(0xFF44403C),
    outline = Color(0xFFD6D3D1)
)

private val AmberWheatDark = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF166534),
    onSecondaryContainer = Color(0xFFBBF7D0),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFF14120E),
    surface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFF292524),
    onSurface = Color(0xFFF5F5F4),
    onSurfaceVariant = Color(0xFFD6D3D1),
    outline = Color(0xFF57534E)
)

// 4. Тёмный графит
private val DarkGraphiteLight = lightColorScheme(
    primary = Color(0xFF334155),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF10B981),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val DarkGraphiteDark = darkColorScheme(
    primary = Color(0xFF94A3B8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0C4A6E),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF34D399),
    background = Color(0xFF0B0F19),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1F2937),
    onSurface = Color(0xFFF9FAFB),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF4B5563)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    colorStyle: String = "AGRO_GREEN",
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemInDark
    }

    val style = try {
        AppColorStyle.valueOf(colorStyle)
    } catch (_: Exception) {
        AppColorStyle.AGRO_GREEN
    }

    val colorScheme: ColorScheme = when (style) {
        AppColorStyle.AGRO_GREEN -> if (isDark) AgroGreenDark else AgroGreenLight
        AppColorStyle.TRANSPORT_BLUE -> if (isDark) TransportBlueDark else TransportBlueLight
        AppColorStyle.AMBER_WHEAT -> if (isDark) AmberWheatDark else AmberWheatLight
        AppColorStyle.DARK_GRAPHITE -> if (isDark) DarkGraphiteDark else DarkGraphiteLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
