package com.example.netisolate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    tertiary = PrimaryColor, // Reuse primary for tertiary to keep it minimal
    background = BackgroundColor,
    surface = SurfaceColor,
    onPrimary = OnPrimaryColor,
    onSecondary = OnPrimaryColor,
    onTertiary = OnPrimaryColor,
    onBackground = OnBackgroundColor,
    onSurface = OnSurfaceColor,
    error = ErrorColor
)

// Define sharp shapes
val CyberShapes = Shapes(
    small = CutCornerShape(0.dp),
    medium = CutCornerShape(4.dp), 
    large = CutCornerShape(8.dp)
)

@Composable
fun NetIsolateTheme(
    // Force Dark Theme
    darkTheme: Boolean = true, 
    // Disable Dynamic Color to enforce our aesthetic
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CyberShapes, // Apply sharp corners
        content = content
    )
}