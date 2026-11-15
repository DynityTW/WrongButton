package tw.dynity.wrongbutton.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tw.dynity.wrongbutton.core.settings.LocalReduceMotion

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFFB3402F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF775651),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF6F5C2E),
    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFFFBF8),
    onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    surfaceContainer = Color(0xFFFCEAE7),
    surfaceContainerHigh = Color(0xFFF7E4E1),
    outline = Color(0xFF857371),
    error = Color(0xFFBA1A1A),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF690002),
    primaryContainer = Color(0xFF8F2A1C),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFE7BDB6),
    onSecondary = Color(0xFF442925),
    secondaryContainer = Color(0xFF5D3F3B),
    onSecondaryContainer = Color(0xFFFFDAD4),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DFDC),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF1DFDC),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BE),
    surfaceContainer = Color(0xFF271D1C),
    surfaceContainerHigh = Color(0xFF322827),
    outline = Color(0xFFA08C89),
    error = Color(0xFFFFB4AB),
)

/** 正確／錯誤標籤用的語意色（第 10 關）。資訊同時用文字與圖示傳達，顏色只是輔助。 */
object SemanticColors {
    val Correct = Color(0xFF2E7D32)
    val Wrong = Color(0xFFC62828)
}

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Black),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        bodyLarge = t.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
    )
}

val TaskTextStyle = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)

/**
 * Material 3 Expressive（MaterialExpressiveTheme、MotionScheme）在目前最新穩定版 material3 1.4.0
 * 裡是 internal，無法使用；要到 1.5.0 才會公開。等 1.5.0 穩定後，只需要改這個函式：
 * 換成 MaterialExpressiveTheme，並依 [reduceMotion] 選 MotionScheme.standard() / expressive()。
 */
@Composable
fun WrongButtonTheme(
    reduceMotion: Boolean,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
        ) {
            // 整個 App 包在 Surface 裡：沒有指定顏色的文字與圖示才會跟著主題（深色模式下是淺色字）。
            Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
                content()
            }
        }
    }
}
