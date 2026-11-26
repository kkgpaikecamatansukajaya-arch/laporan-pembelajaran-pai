package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
  brightBgColor: Color = BackgroundLight,
  content: @Composable () -> Unit,
) {
  val brightColorScheme =
    lightColorScheme(
      primary = GreenPrimary,
      secondary = GoldAccent,
      tertiary = GreenSecondary,
      background = brightBgColor,
      surface = SurfaceLight,
      surfaceVariant = SurfaceVariantLight,
      onPrimary = Color.White,
      onSecondary = Color.White,
      onTertiary = Color.White,
      onBackground = TextDark,
      onSurface = TextDark,
      onSurfaceVariant = TextSecondaryDark
    )

  MaterialTheme(colorScheme = brightColorScheme, typography = Typography, content = content)
}
