package tools.fmc.companion

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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image

// Design tokens and components, after ios/App/Style.swift: workout-companion is the base (
// grouped sections, filled number badges), Zeron adds capsule buttons and status colors.

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

object Fmc {
    val success @Composable get() = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF15803D)
    val warning @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFACC15) else Color(0xFFA16207)
    val danger @Composable get() = MaterialTheme.colorScheme.error
    val secondary @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun FmcTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) dark else light, content = content)

/** A titled group of rows on a rounded card, like an iOS inset grouped section. */
@Composable
fun Section(title: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        title?.let {
            Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = Fmc.secondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        }
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
        footer?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Fmc.secondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
        }
    }
}

/** A filled rounded square with a symbol or a number in it: the watch tile (64) and the day badge (44). */
@Composable
fun Badge(side: Dp = 44.dp, filled: Boolean = true, content: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(side), shape = RoundedCornerShape(side * 0.31f),
        color = if (filled) c.primary else c.surfaceVariant, contentColor = if (filled) c.onPrimary else c.onSurfaceVariant,
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}

/** A big number with its label under it. (workout-companion Metric) */
@Composable
fun Metric(value: String, unit: String, modifier: Modifier = Modifier) = Column(modifier) {
    Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Text(unit, style = MaterialTheme.typography.bodyMedium, color = Fmc.secondary)
}

private val capsule = PaddingValues(horizontal = 20.dp, vertical = 15.dp)

/** Zeron's primary button: a full-width capsule in the accent (fmc.tools .kind__primary). */
@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) =
    Button(onClick, Modifier.fillMaxWidth(), enabled = enabled, contentPadding = capsule) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }

/** Zeron's secondary button: a quiet capsule under the primary one. */
@Composable
fun SecondaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) =
    FilledTonalButton(onClick, Modifier.fillMaxWidth(), enabled = enabled, contentPadding = capsule,
        colors = ButtonDefaults.filledTonalButtonColors()) { Text(text, fontWeight = FontWeight.Medium, fontSize = 17.sp) }

// decoded pictures in memory, by their size in bytes; the files themselves on disk (ImageCache)
private val images = object : LruCache<String, ImageBitmap>(32 shl 20) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

/** The watch's 466×466 round panel. */
@Composable
fun FacePreview(url: String?, modifier: Modifier = Modifier) {
    val bitmap = rememberImage(url)
    val alpha by animateFloatAsState(
        targetValue = if (bitmap != null) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "imageAlpha"
    )
    Box(modifier.aspectRatio(1f).clip(CircleShape).background(Color.Black)) {
        bitmap?.let { Image(it, null, Modifier.matchParentSize().graphicsLayer { this.alpha = alpha }, contentScale = ContentScale.Fit) }
    }
}

/** A picture from the web, from disk after the first time; null while loading, on failure or for a format Android can't decode. */
@Composable
fun rememberImage(url: String?): ImageBitmap? {
    // keyed by url: the same call site showing another picture starts over
    var bitmap by remember(url) { mutableStateOf(url?.let(images::get)) }
    val context = LocalContext.current
    LaunchedEffect(url) {
        if (bitmap == null && url != null) {
            ImageCache.init(context.cacheDir)
            bitmap = runCatching {
                val raw = ImageCache.bytes(url)
                withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(raw, 0, raw.size).asImageBitmap() }
                    .also { images.put(url, it) }
            }.getOrNull()
        }
    }
    return bitmap
}
