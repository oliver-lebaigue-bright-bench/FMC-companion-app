package tools.fmc.companion

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import kotlin.math.roundToInt

// Design tokens and components, after ios/App/Style.swift: workout-companion is the base (
// grouped sections, filled number badges), Zeron adds capsule buttons and status colors.

enum class AppTheme {
    Classic,
    Material3;

    companion object {
        fun from(saved: String?) = entries.firstOrNull { it.name == saved } ?: Classic
    }
}

/** True while the loud Material 3 theme is on; components read it for discrete choices. */
val LocalExpressive = compositionLocalOf { false }

/** 0 = classic, 1 = Material 3, animated between them. Components use it to morph their own details. */
val LocalThemeProgress = compositionLocalOf { 0f }

private val light = lightColorScheme(
    primary = Color(0xFFFFC700), onPrimary = Color(0xFF1C1C1C), // fmc.tools --color-accent / --color-on-accent
    background = Color(0xFFF2F2F7), onBackground = Color(0xFF18181B),
    surface = Color.White, onSurface = Color(0xFF18181B), surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6B6B70), secondaryContainer = Color(0xFFE9E9EE), onSecondaryContainer = Color(0xFF18181B),
    surfaceContainer = Color.White, error = Color(0xFFDC2626),
)
private val dark = darkColorScheme(
    primary = Color(0xFFFFC700), onPrimary = Color(0xFF131313),
    background = Color.Black, onBackground = Color(0xFFECECEC),
    surface = Color(0xFF1C1C1E), onSurface = Color(0xFFECECEC), surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF9A9AA0), secondaryContainer = Color(0xFF2C2C2E), onSecondaryContainer = Color(0xFFECECEC),
    surfaceContainer = Color(0xFF1C1C1E), error = Color(0xFFF87171),
)

// Material 3 Expressive: wallpaper colour when the phone supplies it (Android 12+, so every
// install here), with a loud violet fallback for devices that return nothing useful.
private val expressiveLight = lightColorScheme(
    primary = Color(0xFF8A4CE0), onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF341168),
    secondary = Color(0xFF625B71), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1E192B),
    tertiary = Color(0xFF9C4146), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8DE), onTertiaryContainer = Color(0xFF400D13),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454F),
    surfaceContainer = Color(0xFFF3EDF7), surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9), outline = Color(0xFF79747E),
    error = Color(0xFFB3261E),
)
private val expressiveDark = darkColorScheme(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC), onSecondary = Color(0xFF332941),
    secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8), onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48), onTertiaryContainer = Color(0xFFFFD8DE),
    background = Color(0xFF141218), onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218), onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F), onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF211F26), surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B), outline = Color(0xFF938F99),
    error = Color(0xFFF2B8B5),
)

private val classicTypography = Typography()
/** Expressive type: much larger headlines, tighter tracking, heavier titles. */
private val expressiveTypography = Typography().copy(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
)
/** Linear interpolation helpers, so the whole theme can move on one animated 0…1 progress. */
internal fun mixDp(from: Dp, to: Dp, progress: Float) = (from.value + (to.value - from.value) * progress).dp

internal fun mixColor(from: Color, to: Color, progress: Float) = Color(
    alpha = from.alpha + (to.alpha - from.alpha) * progress,
    red = from.red + (to.red - from.red) * progress,
    green = from.green + (to.green - from.green) * progress,
    blue = from.blue + (to.blue - from.blue) * progress,
)

private fun mixUnit(from: TextUnit, to: TextUnit, progress: Float) =
    if (from == TextUnit.Unspecified || to == TextUnit.Unspecified) from
    else (from.value + (to.value - from.value) * progress).sp

private fun mixText(from: TextStyle, to: TextStyle, progress: Float): TextStyle {
    val fromWeight = from.fontWeight?.weight ?: 400
    val toWeight = to.fontWeight?.weight ?: fromWeight
    return from.copy(
        fontSize = mixUnit(from.fontSize, to.fontSize, progress),
        lineHeight = mixUnit(from.lineHeight, to.lineHeight, progress),
        letterSpacing = mixUnit(from.letterSpacing, to.letterSpacing, progress),
        fontWeight = FontWeight((fromWeight + (toWeight - fromWeight) * progress)
            .roundToInt().coerceIn(1, 1000)),
    )
}

/** Material 3 Expressive's very round corners, interpolated from the standard M3 shapes. */
private fun mixShapes(progress: Float) = Shapes(
    extraSmall = RoundedCornerShape(mixDp(4.dp, 10.dp, progress)),
    small = RoundedCornerShape(mixDp(8.dp, 18.dp, progress)),
    medium = RoundedCornerShape(mixDp(16.dp, 28.dp, progress)),
    large = RoundedCornerShape(mixDp(28.dp, 36.dp, progress)),
    extraLarge = RoundedCornerShape(mixDp(48.dp, 52.dp, progress)),
)

private fun mixTypography(progress: Float) = classicTypography.copy(
    displayLarge = mixText(classicTypography.displayLarge, expressiveTypography.displayLarge, progress),
    displayMedium = mixText(classicTypography.displayMedium, expressiveTypography.displayMedium, progress),
    displaySmall = mixText(classicTypography.displaySmall, expressiveTypography.displaySmall, progress),
    headlineLarge = mixText(classicTypography.headlineLarge, expressiveTypography.headlineLarge, progress),
    headlineMedium = mixText(classicTypography.headlineMedium, expressiveTypography.headlineMedium, progress),
    headlineSmall = mixText(classicTypography.headlineSmall, expressiveTypography.headlineSmall, progress),
    titleLarge = mixText(classicTypography.titleLarge, expressiveTypography.titleLarge, progress),
    titleMedium = mixText(classicTypography.titleMedium, expressiveTypography.titleMedium, progress),
    titleSmall = mixText(classicTypography.titleSmall, expressiveTypography.titleSmall, progress),
    bodyLarge = mixText(classicTypography.bodyLarge, expressiveTypography.bodyLarge, progress),
    bodyMedium = mixText(classicTypography.bodyMedium, expressiveTypography.bodyMedium, progress),
    bodySmall = mixText(classicTypography.bodySmall, expressiveTypography.bodySmall, progress),
    labelLarge = mixText(classicTypography.labelLarge, expressiveTypography.labelLarge, progress),
    labelMedium = mixText(classicTypography.labelMedium, expressiveTypography.labelMedium, progress),
    labelSmall = mixText(classicTypography.labelSmall, expressiveTypography.labelSmall, progress),
)

/** Every role of the colour scheme moves together — no role snaps while its neighbours slide. */
private fun mixColors(a: ColorScheme, b: ColorScheme, p: Float) =
    a.copy(
        primary = mixColor(a.primary, b.primary, p),
        onPrimary = mixColor(a.onPrimary, b.onPrimary, p),
        primaryContainer = mixColor(a.primaryContainer, b.primaryContainer, p),
        onPrimaryContainer = mixColor(a.onPrimaryContainer, b.onPrimaryContainer, p),
        secondary = mixColor(a.secondary, b.secondary, p),
        onSecondary = mixColor(a.onSecondary, b.onSecondary, p),
        secondaryContainer = mixColor(a.secondaryContainer, b.secondaryContainer, p),
        onSecondaryContainer = mixColor(a.onSecondaryContainer, b.onSecondaryContainer, p),
        tertiary = mixColor(a.tertiary, b.tertiary, p),
        onTertiary = mixColor(a.onTertiary, b.onTertiary, p),
        tertiaryContainer = mixColor(a.tertiaryContainer, b.tertiaryContainer, p),
        onTertiaryContainer = mixColor(a.onTertiaryContainer, b.onTertiaryContainer, p),
        error = mixColor(a.error, b.error, p),
        onError = mixColor(a.onError, b.onError, p),
        errorContainer = mixColor(a.errorContainer, b.errorContainer, p),
        onErrorContainer = mixColor(a.onErrorContainer, b.onErrorContainer, p),
        background = mixColor(a.background, b.background, p),
        onBackground = mixColor(a.onBackground, b.onBackground, p),
        surface = mixColor(a.surface, b.surface, p),
        onSurface = mixColor(a.onSurface, b.onSurface, p),
        surfaceVariant = mixColor(a.surfaceVariant, b.surfaceVariant, p),
        onSurfaceVariant = mixColor(a.onSurfaceVariant, b.onSurfaceVariant, p),
        surfaceTint = mixColor(a.surfaceTint, b.surfaceTint, p),
        inverseSurface = mixColor(a.inverseSurface, b.inverseSurface, p),
        inverseOnSurface = mixColor(a.inverseOnSurface, b.inverseOnSurface, p),
        inversePrimary = mixColor(a.inversePrimary, b.inversePrimary, p),
        outline = mixColor(a.outline, b.outline, p),
        outlineVariant = mixColor(a.outlineVariant, b.outlineVariant, p),
        scrim = mixColor(a.scrim, b.scrim, p),
        surfaceContainerLowest = mixColor(a.surfaceContainerLowest, b.surfaceContainerLowest, p),
        surfaceContainerLow = mixColor(a.surfaceContainerLow, b.surfaceContainerLow, p),
        surfaceContainer = mixColor(a.surfaceContainer, b.surfaceContainer, p),
        surfaceContainerHigh = mixColor(a.surfaceContainerHigh, b.surfaceContainerHigh, p),
        surfaceContainerHighest = mixColor(a.surfaceContainerHighest, b.surfaceContainerHighest, p),
        surfaceBright = mixColor(a.surfaceBright, b.surfaceBright, p),
        surfaceDim = mixColor(a.surfaceDim, b.surfaceDim, p),
    )

object Fmc {
    val success @Composable get() = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF15803D)
    val warning @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFACC15) else Color(0xFFA16207)
    val danger @Composable get() = MaterialTheme.colorScheme.error
    val secondary @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun FmcTheme(theme: AppTheme = AppTheme.Classic, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val expressive = theme == AppTheme.Material3
    // Both endpoints stay fixed during the morph: switching either way animates the same 0…1 value.
    val classicColors = if (darkTheme) dark else light
    val expressiveColors = remember(darkTheme) {
        runCatching {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }.getOrElse { if (darkTheme) expressiveDark else expressiveLight }
    }
    val progress by animateFloatAsState(if (expressive) 1f else 0f, Motion.theme(), label = "themeMorph")
    CompositionLocalProvider(LocalExpressive provides expressive, LocalThemeProgress provides progress) {
        MaterialTheme(
            colorScheme = mixColors(classicColors, expressiveColors, progress),
            typography = mixTypography(progress),
            shapes = mixShapes(progress),
            content = content,
        )
    }
}

/** A titled group of rows on a rounded card, like an iOS inset grouped section. */
@Composable
fun Section(title: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val progress = LocalThemeProgress.current
    val c = MaterialTheme.colorScheme
    val inset = mixDp(16.dp, 18.dp, progress)
    Column(Modifier.padding(horizontal = 16.dp, vertical = mixDp(8.dp, 10.dp, progress))) {
        title?.let {
            Text(if (progress > 0.5f) it else it.uppercase(),
                style = if (progress > 0.5f) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                color = Fmc.secondary,
                modifier = Modifier.padding(start = inset, bottom = mixDp(6.dp, 10.dp, progress)))
        }
        Surface(
            shape = RoundedCornerShape(mixDp(12.dp, 36.dp, progress)),
            color = mixColor(c.surface, c.surfaceContainer, progress),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = inset, vertical = mixDp(10.dp, 16.dp, progress)),
                verticalArrangement = Arrangement.spacedBy(mixDp(10.dp, 14.dp, progress)), content = content)
        }
        footer?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Fmc.secondary,
                modifier = Modifier.padding(start = inset, end = inset, top = mixDp(6.dp, 8.dp, progress)))
        }
    }
}

/** A filled rounded square with a symbol or a number in it: the watch tile (64) and the day badge (44). */
@Composable
fun Badge(side: Dp = 44.dp, filled: Boolean = true, content: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    val progress = LocalThemeProgress.current
    Surface(
        modifier = Modifier.size(side),
        shape = RoundedCornerShape(mixDp(side * 0.31f, 28.dp, progress)),
        color = if (!filled) c.surfaceVariant else mixColor(c.primary, c.primaryContainer, progress),
        contentColor = if (!filled) c.onSurfaceVariant else mixColor(c.onPrimary, c.onPrimaryContainer, progress),
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}

/** A big number with its label under it. (workout-companion Metric) */
@Composable
fun Metric(value: String, unit: String, modifier: Modifier = Modifier) = Column(modifier) {
    Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Text(unit, style = MaterialTheme.typography.bodyMedium, color = Fmc.secondary)
}

/** Zeron's primary button: a full-width capsule in the accent (fmc.tools .kind__primary). */
@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val progress = LocalThemeProgress.current
    Button(onClick, Modifier.fillMaxWidth(), enabled = enabled,
        shape = RoundedCornerShape(mixDp(20.dp, 28.dp, progress)),
        contentPadding = PaddingValues(
            horizontal = mixDp(20.dp, 24.dp, progress), vertical = mixDp(15.dp, 18.dp, progress))) {
        Text(text, fontWeight = if (progress > 0.5f) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = (17f + progress).sp)
    }
}

/** Zeron's secondary button: a quiet capsule under the primary one. */
@Composable
fun SecondaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val progress = LocalThemeProgress.current
    FilledTonalButton(onClick, Modifier.fillMaxWidth(), enabled = enabled,
        shape = RoundedCornerShape(mixDp(20.dp, 28.dp, progress)),
        contentPadding = PaddingValues(
            horizontal = mixDp(20.dp, 24.dp, progress), vertical = mixDp(15.dp, 18.dp, progress)),
        colors = ButtonDefaults.filledTonalButtonColors()) {
        Text(text, fontWeight = if (progress > 0.5f) FontWeight.Bold else FontWeight.Medium,
            fontSize = (17f + progress).sp)
    }
}

// Decoded pictures in memory, by their size in bytes; the files themselves on disk (ImageCache). The key carries the
// target size: a 24 dp avatar must not keep a 1000 px decode alive, nor reuse one meant for a 240 dp preview.
private val images = object : LruCache<String, ImageBitmap>(24 shl 20) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

private fun imageKey(url: String, size: Dp): String = if (size == Dp.Unspecified) url else "$url|${size.value.toInt()}"

/**
 * Decode down to [maxPx] instead of at the source resolution. Two passes over the bytes, but the second one allocates a
 * bitmap a fraction of the size — the difference between a smooth grid and GC churn on every scroll.
 */
private fun decode(raw: ByteArray, maxPx: Int): Bitmap? {
    if (maxPx <= 0) return BitmapFactory.decodeByteArray(raw, 0, raw.size)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample / 2 >= maxPx) sample *= 2
    return BitmapFactory.decodeByteArray(raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

/** The watch's 466×466 round panel. */
@Composable
fun FacePreview(url: String?, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val bitmap = rememberImage(url, size)
    // animateFloatAsState starts at the target: a cached picture appears at once, a downloaded one
    // fades and rises in. No flash of black on screens we have already visited.
    val alpha by animateFloatAsState(if (bitmap != null) 1f else 0f, Motion.quick(), label = "imageAlpha")
    val scale by animateFloatAsState(if (bitmap != null) 1f else 0.96f, Motion.enter(), label = "imageScale")
    Box(modifier.aspectRatio(1f).clip(CircleShape).background(Color.Black)) {
        bitmap?.let { Image(it, null, Modifier.matchParentSize().graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
        }, contentScale = ContentScale.Fit) }
    }
}

/**
 * A picture from the web, from disk after the first time; null while loading, on failure or for a format Android can't
 * decode. [size] is the size it will be drawn at: decoded once at roughly that resolution, then cached at that size.
 */
@Composable
fun rememberImage(url: String?, size: Dp = Dp.Unspecified): ImageBitmap? {
    val density = LocalDensity.current
    val key = url?.let { imageKey(it, size) }
    // keyed by url and size: the same call site showing another picture starts over
    var bitmap by remember(key) { mutableStateOf(key?.let(images::get)) }
    val context = LocalContext.current
    LaunchedEffect(key) {
        if (bitmap == null && key != null && url != null) {
            ImageCache.init(context.cacheDir)
            val maxPx = if (size == Dp.Unspecified) 0 else with(density) { size.toPx() }.roundToInt()
            bitmap = runCatching {
                val raw = ImageCache.bytes(url)
                withContext(Dispatchers.Default) { decode(raw, maxPx)?.asImageBitmap() }
                    .also { if (it != null) images.put(key, it) }
            }.getOrNull()
        }
    }
    return bitmap
}
