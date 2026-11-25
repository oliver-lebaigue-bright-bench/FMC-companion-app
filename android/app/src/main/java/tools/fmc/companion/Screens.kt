package tools.fmc.companion

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.mikepenz.markdown.m3.Markdown
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import android.Manifest
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import tools.fmc.companion.wire.UploadError
import tools.fmc.companion.wire.WireError
import tools.fmc.companion.wire.dialId
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalConfiguration
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.material3.Surface
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import tools.fmc.companion.wire.FaceVerdict
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

/** Where the app is: a tab, and a face or the sign-in page on top of it. */
class Nav {
    var tab by mutableStateOf(0)
    /** The catalog's pages over its grid: face ids, "user:<id>" for an author. */
    val pages = mutableStateListOf<String>()
    var face: String?
        get() = pages.lastOrNull()
        set(v) { if (v == null) pages.removeLastOrNull() else pages.add(v) }
    var signingIn by mutableStateOf(false)
    /** A watch app's page, over Apps or straight from its ask notification. */
    /** App pages, newest last: "store:<name>" is the store page, a bare name the app's settings. */
    private val apps = androidx.compose.runtime.mutableStateListOf<String>()
    var app: String?
        get() = apps.lastOrNull()
        set(v) { if (v == null) apps.removeLastOrNull() else apps.add(v) }
    var developer by mutableStateOf(false)
    var firmware by mutableStateOf(false)
    var rescue by mutableStateOf(false)
    var settings by mutableStateOf(false)

    /** Back to the tab's root. */
    fun root() { pages.clear(); signingIn = false; apps.clear(); developer = false; firmware = false; rescue = false; settings = false }
}

/**
 * A snapshot of a destination. AnimatedContent must render the screen a transition is leaving
 * from; reading Nav's mutable fields inside its content would show the new screen twice instead.
 * [level] is the depth in the navigation stack: deeper goes forward, shallower goes back.
 */
private sealed interface Route {
    val key: String
    val level: Int

    data object Catalog : Route {
        override val key = "catalog"
        override val level = 0
    }
    data object Apps : Route {
        override val key = "apps"
        override val level = 0
    }
    data object Watch : Route {
        override val key = "watch"
        override val level = 0
    }
    data object SignIn : Route {
        override val key = "signin"
        override val level = 2
    }
    data class App(val name: String) : Route {
        override val key = "app:$name"
        override val level = if (name.startsWith("store:")) 1 else 2
    }
    data object Developer : Route {
        override val key = "developer"
        override val level = 2
    }
    data object Firmware : Route {
        override val key = "firmware"
        override val level = 2
    }
    data object Rescue : Route {
        override val key = "rescue"
        override val level = 2
    }
    data object Settings : Route {
        override val key = "settings"
        override val level = 1
    }
    data class Author(val id: String) : Route {
        override val key = "author:$id"
        override val level = 2
    }
    data class Face(val id: String) : Route {
        override val key = "face:$id"
        override val level = 1
    }
}

private fun Nav.route(): Route = when {
    signingIn -> Route.SignIn
    app != null -> Route.App(app!!)
    developer -> Route.Developer
    firmware -> Route.Firmware
    rescue -> Route.Rescue
    settings -> Route.Settings
    face?.startsWith("user:") == true -> Route.Author(face!!.removePrefix("user:"))
    face != null -> Route.Face(face!!)
    tab == 0 -> Route.Catalog
    tab == 2 -> Route.Apps
    else -> Route.Watch
}

/** Forward slides in from the right, back reverses it, tab switches only cross-fade. */
private fun screenTransition(initial: Route, target: Route): ContentTransform = when {
    target.level > initial.level ->
        (slideInHorizontally(Motion.enter()) { it / 6 } + fadeIn(Motion.enter())) togetherWith
            (slideOutHorizontally(Motion.exit()) { -it / 12 } + fadeOut(Motion.exit()))
    target.level < initial.level ->
        (slideInHorizontally(Motion.enter()) { -it / 12 } + fadeIn(Motion.enter())) togetherWith
            (slideOutHorizontally(Motion.exit()) { it / 6 } + fadeOut(Motion.exit()))
    else -> fadeIn(Motion.quick()) togetherWith fadeOut(Motion.quick())
}

/** One screen, from its snapshot rather than from whatever Nav holds mid-transition. */
@Composable
private fun Screen(model: AppModel, nav: Nav, route: Route) {
    when (route) {
        Route.SignIn -> SignInScreen(model) { nav.signingIn = false }
        is Route.App -> AppScreen(model, route.name, { nav.app = it }) { nav.app = null }
        Route.Developer -> DeveloperScreen { nav.developer = false }
        Route.Firmware -> FirmwareScreen(model) { nav.firmware = false }
        Route.Rescue -> RescueScreen(model.store) { nav.rescue = false }
        Route.Settings -> SettingsScreen(model, nav, { nav.signingIn = true }) { nav.settings = false }
        is Route.Author -> AuthorScreen(model, route.id, { nav.face = "user:$it" }) { nav.face = null }
        is Route.Face -> FaceScreen(model, route.id, { nav.face = "user:$it" }) { nav.face = null }
        Route.Catalog -> CatalogScreen(model) { nav.face = it }
        Route.Apps -> AppsScreen(model) { nav.app = it }
        Route.Watch -> WatchScreen(model, nav)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(model: AppModel, nav: Nav) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val themeProgress = LocalThemeProgress.current
    BackHandler(nav.face != null || nav.signingIn || nav.app != null || nav.developer || nav.firmware || nav.rescue || nav.settings) {
        when {
            nav.signingIn -> nav.signingIn = false
            nav.app != null -> nav.app = null
            nav.developer -> nav.developer = false
            nav.firmware -> nav.firmware = false
            nav.rescue -> nav.rescue = false
            nav.settings -> nav.settings = false
            else -> nav.face = null
        }
    }
    Scaffold(
        containerColor = mixColor(MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceContainerLowest, themeProgress),
        bottomBar = {
            val expressive = LocalExpressive.current
            val navColors = if (expressive) NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) else NavigationBarItemDefaults.colors()
            NavigationBar(containerColor = mixColor(MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceContainer, themeProgress)) {
                NavigationBarItem(nav.tab == 0, { nav.tab = 0; nav.root() },
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.watch_faces)) },
                    colors = navColors)
                val pending = liveGrants().pending().size
                NavigationBarItem(nav.tab == 2, { nav.tab = 2; nav.root() }, label = { Text(stringResource(R.string.apps)) },
                    icon = {
                        BadgedBox({ if (pending > 0) androidx.compose.material3.Badge { Text("$pending") } }) {
                            Icon(painterResource(R.drawable.ic_apps), null, Modifier.size(24.dp))
                        }
                    }, colors = navColors)
                NavigationBarItem(nav.tab == 1, { nav.tab = 1; nav.root() },
                    icon = { Icon(painterResource(R.drawable.ic_watch), null, Modifier.size(24.dp)) },
                    label = { Text(stringResource(R.string.watch)) }, colors = navColors)
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            val route = nav.route()
            AnimatedContent(
                targetState = route,
                transitionSpec = { screenTransition(initialState, targetState) },
                modifier = Modifier.fillMaxSize(),
                label = "ScreenTransition"
            ) { target ->
                saveableStateHolder.SaveableStateProvider(target.key) {
                    // Opaque: without this the two screens overlap mid-slide and their text crosses over.
                    val background = mixColor(MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLowest, themeProgress)
                    Box(Modifier.fillMaxSize().background(background)) {
                        Screen(model, nav, target)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Bar(title: String, back: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    val progress = LocalThemeProgress.current
    val c = MaterialTheme.colorScheme
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge,
            fontWeight = if (progress > 0.5f) FontWeight.Bold else FontWeight.SemiBold) },
        navigationIcon = { back?.let { IconButton(it) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } } },
        actions = actions,
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = mixColor(c.background, c.surfaceContainer, progress),
            titleContentColor = mixColor(c.onBackground, c.onSurface, progress),
        ),
    )
}

/** Asks for Bluetooth once, then runs `action`. */
@Composable
fun withBluetooth(action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val perms = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) action()
    }
    return {
        if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) action()
        else ask.launch(perms)
    }
}

// Catalog

/** The FMC catalog and, signed in, the user's own faces: a grid of round previews, search by name or author. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(model: AppModel, open: (String) -> Unit) {
    // 0 all, 1 liked, 2 mine
    var shelf by rememberSaveable { mutableIntStateOf(0) }
    // fmc_frontend market.svelte SORT_OPTIONS (Top creators, a shelf of rails, left out)
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var sorting by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val signedIn = model.user != null
    val showMine = signedIn && shelf == 2
    val q = query.trim()
    val wanted = CatalogQuery(if (signedIn) Shelf.entries[shelf] else Shelf.All, CatalogSort.entries[sort], q, model.user?.id)
    val grid = rememberLazyGridState()
    LaunchedEffect(wanted) {
        // typing: the server is asked once the user pauses, not per letter
        val searchChanged = wanted.search != model.query.search
        val queryChanged = wanted != model.query
        if (searchChanged) delay(350)
        model.show(wanted)
        if (queryChanged) grid.scrollToItem(0)
    }
    val shown = model.faces
    // the next page when the last cells come into view
    val nearEnd by remember { derivedStateOf {
        val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        CatalogQuery.nearEnd(last - 1, model.faces.size) // item 0 is the header
    } }
    LaunchedEffect(nearEnd, shown.size) { if (nearEnd) model.more() }
    Column {
        Bar(if (showMine) stringResource(R.string.my_watch_faces) else if (signedIn && shelf == 1) stringResource(R.string.liked) else stringResource(R.string.watch_faces)) {
            Box {
                IconButton({ sorting = true }) { Icon(painterResource(R.drawable.ic_sort), stringResource(R.string.sort)) }
                DropdownMenu(sorting, { sorting = false }) {
                    listOf(stringResource(R.string.sort_newest), stringResource(R.string.sort_popular), stringResource(R.string.sort_most_downloaded)).forEachIndexed { i, label ->
                        DropdownMenuItem({ Text(label) }, { sort = i; sorting = false },
                            trailingIcon = { if (i == sort) Icon(Icons.Default.Check, stringResource(R.string.selected)) })
                    }
                }
            }
        }
        PullToRefreshBox(refreshing, { scope.launch { refreshing = true; model.refresh(); refreshing = false } }) {
            LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), state = grid,
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (model.user != null) {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                listOf(stringResource(R.string.shelf_all), stringResource(R.string.liked), stringResource(R.string.shelf_mine)).forEachIndexed { i, label ->
                                    SegmentedButton(shelf == i, { shelf = i },
                                        SegmentedButtonDefaults.itemShape(i, 3)) { Text(label) }
                                }
                            }
                        }
                        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                            placeholder = { Text(stringResource(R.string.search_placeholder)) }, leadingIcon = { Icon(Icons.Default.Search, null) })
                    }
                }
                if (shown.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val empty = when {
                            model.catalogError != null -> stringResource(R.string.catalog_error, model.catalogError.orEmpty())
                            model.catalogLoading -> stringResource(R.string.loading)
                            q.isNotEmpty() -> stringResource(R.string.nothing_found)
                            showMine -> stringResource(R.string.no_watch_faces_yet)
                            signedIn && shelf == 1 -> stringResource(R.string.no_liked_watch_faces)
                            else -> stringResource(R.string.loading)
                        }
                        Text(empty, Modifier.padding(top = 80.dp), color = Fmc.secondary, textAlign = TextAlign.Center)
                    }
                }
                items(shown, key = { it.id }) { face ->
                    FaceCard(model, face, Modifier.animateItem()) { open(face.id) }
                }
                if (model.catalogLoading && !model.catalogEnd && shown.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

/** As fmc.tools' watchface-card: the dial, its name, the author, then downloads, likes and the type. */
@Composable
fun FaceCard(model: AppModel, face: Watchface, modifier: Modifier = Modifier, open: () -> Unit) {
    val progress = LocalThemeProgress.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = Motion.press(),
        label = "cardScale"
    )
    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, onClick = open),
        shape = RoundedCornerShape(mixDp(18.dp, 36.dp, progress)),
        color = mixColor(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceContainerLow, progress)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FacePreview(face.thumbURL, Modifier.fillMaxWidth().padding(bottom = 2.dp), size = 160.dp)
            Text(face.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!face.published) Text(stringResource(R.string.draft), style = MaterialTheme.typography.bodySmall, color = Fmc.secondary)
            else face.author?.let { author ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Avatar(face.authorAvatar, author, 18.dp)
                    Text(author, style = MaterialTheme.typography.bodySmall, color = Fmc.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("↓ ${compact(face.downloads)}", style = MaterialTheme.typography.bodySmall, color = Fmc.secondary)
                LikeButton(model, face)
                Spacer(Modifier.weight(1f))
                if (face.type.isNotEmpty()) Text(face.type, Modifier.clip(CircleShape).background(Fmc.secondary.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** A creator's page, as fmc.tools /user/<id>: avatar, name, totals, then their published faces. */
@Composable
fun AuthorScreen(model: AppModel, id: String, open: (String) -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    var faces by remember(id) { mutableStateOf<List<Watchface>>(emptyList()) }
    LaunchedEffect(id) { runCatching { model.author(id) }.onSuccess { faces = it } }
    val first = faces.firstOrNull()
    val grid = rememberLazyGridState()
    Column {
        Bar(first?.author ?: "", back) {
            IconButton({ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$FMC/user/$id"))) }) {
                Icon(painterResource(R.drawable.ic_open_in_new), stringResource(R.string.open_on_fmc))
            }
        }
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), state = grid, contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Avatar(first?.authorAvatar, first?.author ?: "?", 80.dp)
                    Text(first?.author ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    val downloads = faces.sumOf { it.downloads }
                    val likes = faces.sumOf { model.likeCount(it) }
                    Text(pluralStringResource(R.plurals.author_faces, faces.size, "${faces.size}") + " · " +
                        pluralStringResource(R.plurals.author_downloads, downloads, compact(downloads)) + " · " +
                        pluralStringResource(R.plurals.author_likes, likes, compact(likes)), color = Fmc.secondary)
                }
            }
            items(faces, key = { it.id }) { face ->
                FaceCard(model, face, Modifier.animateItem()) { open(face.id) }
            }
        }
    }
}

/** The heart and the count; a tap likes or unlikes when signed in. */
@Composable
fun LikeButton(model: AppModel, face: Watchface) {
    val scope = rememberCoroutineScope()
    val liked = model.liked(face)
    val count = model.likeCount(face)
    // A one-off pop on change, not a permanent 10% larger heart for as long as it stays liked.
    val scale = remember(face.id) { Animatable(1f) }
    var lastLiked by remember(face.id) { mutableStateOf(liked) }
    LaunchedEffect(liked) {
        if (liked == lastLiked) return@LaunchedEffect
        lastLiked = liked
        if (liked) {
            scale.animateTo(1.3f, Motion.pop())
            scale.animateTo(1f, Motion.settle())
        } else {
            scale.animateTo(0.82f, Motion.pop())
            scale.animateTo(1f, Motion.settle())
        }
    }
    val fill by animateFloatAsState(if (liked) 1f else 0f, Motion.quick(), label = "heartFill")
    val tint by animateColorAsState(
        targetValue = if (liked) Color(0xFFE5484D) else Fmc.secondary,
        animationSpec = Motion.quick(),
        label = "heartColor"
    )
    Row(Modifier.clip(CircleShape).clickable(enabled = model.user != null) { scope.launch { model.toggleLike(face) } },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.size(15.dp).graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }) {
            Icon(Icons.Default.FavoriteBorder, null, Modifier.matchParentSize(), tint = tint.copy(alpha = 1f - fill))
            Icon(Icons.Default.Favorite, null, Modifier.matchParentSize().graphicsLayer { alpha = fill }, tint = tint)
        }
        AnimatedContent(count, transitionSpec = {
            (fadeIn(Motion.quick()) + slideInVertically { it / 3 }) togetherWith
                (fadeOut(Motion.quick()) + slideOutVertically { -it / 3 })
        }, label = "likeCount") { n ->
            Text(compact(n), style = MaterialTheme.typography.bodySmall, color = Fmc.secondary)
        }
    }
}

/** A round avatar, else the name's first letter. */
@Composable
fun Avatar(url: String?, name: String, side: Dp) {
    val bitmap = rememberImage(url, side)
    Surface(Modifier.size(side), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
        if (bitmap != null) Image(bitmap, null, Modifier.size(side), contentScale = ContentScale.Crop)
        else Box(contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), fontSize = (side.value * 0.5f).sp, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/** 1234 → "1.2K", as fmc.tools' compact(). */
fun compact(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).replace(".0M", "M")
    n >= 1_000 -> "%.1fK".format(n / 1_000.0).replace(".0K", "K")
    else -> "$n"
}

/** A face on the watch as Nothing X and fmc.tools show it: its round preview and name; the active one ringed yellow. */
/** A face's round picture: its preview, else the black panel with its number. */
@Composable
fun DialPicture(model: AppModel, slot: DialSlot, modifier: Modifier = Modifier, size: Dp = 88.dp) {
    val url = model.dials[slot.id]?.thumbURL ?: slot.preview
    if (url != null) FacePreview(url, modifier, size)
    else Box(modifier.aspectRatio(1f).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
        // no picture anywhere for stock and Nothing X faces: the watch's place for it
        Text("${slot.index + 1}",
            color = mixColor(Color(0xFFFFC700), MaterialTheme.colorScheme.primary, LocalThemeProgress.current),
            fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * The CMF Watch Pro 2 render fmc.tools frames dials with (fmc_frontend watch-case.svelte), [content] on its glass. Laid
 * out as the bezel alone ([side] across): the case and strap hang outside it and take no space. The render is square,
 * the bezel 51.75% of it, centred a hair under the middle; the panel 94% of the bezel.
 */
@Composable
fun WatchFrame(side: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier.size(side), contentAlignment = Alignment.Center) {
        val render = side / 0.5175f
        Image(painterResource(R.drawable.watch_frame), null,
            Modifier.wrapContentSize(unbounded = true).requiredSize(render).offset(y = -render * 0.00175f))
        Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize(0.94f).clip(CircleShape)) { content() }
        }
    }

@Composable
fun DialTile(model: AppModel, slot: DialSlot, modifier: Modifier = Modifier, active: Boolean = false) {
    val face = model.dials[slot.id]
    Column(modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val ring = if (active) Modifier.border(2.5.dp,
            mixColor(Color(0xFFFFC700), MaterialTheme.colorScheme.primary, LocalThemeProgress.current), CircleShape)
        else Modifier
        Box(ring.padding(3.dp)) { DialPicture(model, slot, Modifier.fillMaxWidth()) }
        Text(face?.name ?: slot.label, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun FaceScreen(model: AppModel, id: String, author: (String) -> Unit = {}, back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var face by remember(id) { mutableStateOf<Watchface?>(null) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    /** Install progress 0…1, null when idle. */
    var progress by remember(id) { mutableStateOf<Float?>(null) }
    var installed by remember(id) { mutableStateOf(false) }
    /** Images the install compressed to fit the watch's memory (fitForWatch). */
    var compressed by remember(id) { mutableStateOf(0) }
    var closeNothingX by remember { mutableStateOf(false) }
    /** A file past the safe image size, waiting for the user's yes. */
    var big by remember { mutableStateOf<Triple<ByteArray, Int, Int>?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    /** The watch is full: the install waits here for the dial to replace. */
    var picking by remember { mutableStateOf<Pair<List<DialSlot>, CompletableDeferred<Long?>>?>(null) }
    /** Picked on the Install tap, before connecting. */
    var preset by remember { mutableStateOf<Long?>(null) }
    suspend fun ask(slots: List<DialSlot>) = CompletableDeferred<Long?>().also { picking = slots to it }.await()

    LaunchedEffect(id) {
        try { face = model.face(id) } catch (e: Exception) { error = e.message ?: str(R.string.face_load_failed) }
    }

    fun upload(bin: ByteArray) = scope.launch {
        val f = face ?: return@launch
        progress = 0f
        try {
            compressed = installFaceOnWatch(context, model.store, bin, f.id, f.name, pick = { slots -> ask(slots) }, preset = preset) { p -> scope.launch { progress = p / 100f } }
            context.getSharedPreferences("dials", 0).edit().putString("preview-${dialId(f.id)}", f.thumbURL).apply()
            installed = true
            Analytics.track("face_installed", mapOf("id" to f.id))
        } catch (e: CancellationException) {
            if (!isActive) throw e // a dismissed slot dialog cancels the install, not the screen
        } catch (e: Exception) {
            failure = e.message ?: str(R.string.watch_no_answer)
        } finally {
            progress = null
        }
    }

    fun fetchAndInstall() = scope.launch {
        val f = face ?: return@launch
        progress = 0f
        try {
            val (bin, verdict) = fetchFace(f)
            if (verdict is FaceVerdict.Confirm) {
                progress = null
                big = Triple(bin, verdict.w, verdict.h)
            } else {
                upload(bin)
            }
        } catch (e: Exception) {
            progress = null
            failure = e.message
        }
    }

    // full when last read: the dial to replace first, then the Nothing X warning
    val install = withBluetooth {
        scope.launch {
            preset = null
            val known = face?.let { knownFull(context, it.id) }
            if (known != null) preset = ask(known) ?: return@launch
            closeNothingX = true
        }
    }

    Column {
        Bar(face?.name ?: "", back) {
            face?.let { f ->
                IconButton({ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(f.pageURL))) }) {
                    Icon(painterResource(R.drawable.ic_open_in_new), stringResource(R.string.open_on_fmc))
                }
            }
        }
        LazyColumn {
            val f = face
            when {
                f != null -> {
                    item {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            FacePreview(f.previewURL, Modifier.widthIn(max = 240.dp).fillMaxWidth(), size = 240.dp)
                            Text(f.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center)
                            f.otherDevice?.let { other ->
                                Text(stringResource(R.string.made_for_other, other), color = Fmc.warning,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(Fmc.warning.copy(alpha = 0.12f)).padding(12.dp))
                            }
                            val p = progress
                            if (p != null) {
                                Text(stringResource(R.string.installing))
                                LinearProgressIndicator({ p }, Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                            } else {
                                PrimaryButton(if (installed) stringResource(R.string.installed) else stringResource(R.string.install), !installed && f.binURL != null) {
                                    install()
                                }
                                if (installed && compressed > 0) {
                                    Text(pluralStringResource(R.plurals.fit_note, compressed, compressed),
                                        style = MaterialTheme.typography.bodySmall, color = Fmc.secondary, textAlign = TextAlign.Center)
                                }
                            }
                            Text(stringResource(R.string.close_nothing_x_hint),
                                style = MaterialTheme.typography.bodySmall, color = Fmc.secondary, textAlign = TextAlign.Center)
                        }
                    }
                    item {
                        Section {
                            f.author?.let { name ->
                                Row(Modifier.fillMaxWidth().clickable(enabled = f.ownerId != null) { f.ownerId?.let(author) },
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.author), Modifier.weight(1f))
                                    Avatar(f.authorAvatar, name, 24.dp)
                                    Text(name, color = Fmc.secondary)
                                    Text("›", color = Fmc.secondary)
                                }
                                HorizontalDivider()
                            }
                            Row { Text(stringResource(R.string.downloads), Modifier.weight(1f)); Text("${f.downloads}", color = Fmc.secondary) }
                            HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.likes), Modifier.weight(1f)); LikeButton(model, f) }
                        }
                    }
                    if (f.description.isNotEmpty()) item { Section(stringResource(R.string.about)) { SelectionContainer { Markdown(f.description) } } }
                }
                error != null -> item {
                    Text(stringResource(R.string.face_load_error, error.orEmpty()), Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = TextAlign.Center, color = Fmc.secondary)
                }
                else -> item {
                    Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    if (closeNothingX) AlertDialog(
        onDismissRequest = { closeNothingX = false },
        title = { Text(stringResource(R.string.close_nothing_x_title)) },
        text = {
            Text(stringResource(R.string.close_nothing_x_face))
        },
        confirmButton = { TextButton({ closeNothingX = false; fetchAndInstall() }) { Text(stringResource(R.string.install)) } },
        dismissButton = { TextButton({ closeNothingX = false }) { Text(stringResource(R.string.cancel)) } },
    )
    big?.let { (bin, w, h) ->
        AlertDialog(
            onDismissRequest = { big = null },
            title = { Text(stringResource(R.string.large_images_title)) },
            text = { Text(stringResource(R.string.large_images_text, w, h)) },
            confirmButton = { TextButton({ big = null; upload(bin) }) { Text(stringResource(R.string.install_anyway), color = Fmc.danger) } },
            dismissButton = { TextButton({ big = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    picking?.let { (slots, answer) ->
        fun done(id: Long?) { picking = null; answer.complete(id) }
        AlertDialog(
            onDismissRequest = { done(null) },
            title = { Text(stringResource(R.string.watch_full_title)) },
            text = {
                // the faces as on the Watch tab: tap the one to replace
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.watch_full_pick), color = Fmc.secondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(slots, key = { it.index }) { s ->
                            DialTile(model, s, Modifier.clip(RoundedCornerShape(12.dp)).clickable { done(s.id) })
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ done(null) }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    failure?.let {
        AlertDialog(onDismissRequest = { failure = null }, title = { Text(stringResource(R.string.install_failed)) }, text = { Text(it) },
            confirmButton = { TextButton({ failure = null }) { Text(stringResource(R.string.ok)) } })
    }
}

// Watch

@Composable
fun WatchScreen(model: AppModel, nav: Nav) {
    val context = LocalContext.current
    val check = withBluetooth { model.check() }
    val scope = rememberCoroutineScope()
    Column {
        Bar("FMC Companion", actions = {
            IconButton({ nav.settings = true }) { Icon(Icons.Default.Settings, stringResource(R.string.settings)) }
        })
        LazyColumn {
            item {
                AnimatedVisibility(
                    visible = AppUpdate.available != null,
                    enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                    exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
                ) {
                    AppUpdate.available?.let { r ->
                        Section(footer = AppUpdate.error) {
                            val progress = AppUpdate.progress
                            if (progress != null) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.downloading_update, r.name))
                                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                                }
                            } else {
                                NavRow(stringResource(R.string.update_to, r.name), onClick = { scope.launch { AppUpdate.install(context) } }) {
                                    IconTile(painterResource(R.drawable.ic_watch), Color(0xFF34C759))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Section(footer = stringResource(R.string.watch_footer)) {
                    WatchStatus(model) { check() }
                    // the watch is usually there: read it on opening, as the Apps tab does
                    LaunchedEffect(Unit) { if (model.phase is Phase.Idle) check() }
                }
            }
            // faces and firmware are the watch's: only while it is connected
            if (model.phase is Phase.Checked) item {
                var reading by remember { mutableStateOf(false) }
                var readError by remember { mutableStateOf<String?>(null) }
                suspend fun read(bind: Boolean, retry: Boolean = true) {
                    reading = true
                    readError = try {
                        readFacesOnWatch(context, model.store, bind); null
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        // right after a connect or Bluetooth coming back the watch can be slow to answer: once more
                        if (!bind && retry && e is WireError.Timeout) return read(false, retry = false)
                        if (!bind) DevLog.log(context, "faces: ${if (e is UploadError.AuthFailed) "the saved stock key was refused" else e.toString()}")
                        if (!bind && (e is UploadError.AuthFailed || e is WireError.Timeout)) null // the list stays as last read
                        else str(R.string.faces_read_failed, e.message.toString())
                    }
                    reading = false
                    model.facesRead++
                }
                // right after each connect, with the saved key only: a bind takes the watch from Nothing X
                LaunchedEffect(model.phase) { if (model.phase is Phase.Checked) read(bind = false) }
                var busy by remember { mutableStateOf(false) }
                var deleting by remember { mutableStateOf<DialSlot?>(null) }
                // a change to the list on the watch, then the list again
                fun change(op: suspend () -> Unit) = scope.launch {
                    busy = true
                    readError = try { op(); null } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        else str(R.string.faces_change_failed, e.message.toString())
                    }
                    busy = false
                    model.facesRead++
                }
                deleting?.let { slot ->
                    AlertDialog({ deleting = null }, title = { Text(stringResource(R.string.delete_face_title, slot.label)) },
                        text = { Text(stringResource(R.string.delete_face_text)) },
                        confirmButton = { TextButton({ deleting = null; change { deleteFace(context, model.store, slot.id) } }) { Text(stringResource(R.string.delete), color = Fmc.danger) } },
                        dismissButton = { TextButton({ deleting = null }) { Text(stringResource(R.string.cancel)) } })
                }
                val on = remember(model.facesRead) { facesOnWatch(context) }
                LaunchedEffect(on.faces.map { it.id }) { model.resolveDials(on.faces.map { it.id }) }
                Section(on.count?.let { stringResource(R.string.watch_faces_count, it) } ?: stringResource(R.string.watch_faces)) {
                    val faces = on.faces
                    // as Nothing X and fmc.tools show them: the round previews in the watch's own order. An FMC face,
                    // from here or fmc.tools, is dialId(record id) and has its catalog preview.
                    val row = rememberLazyListState()
                    // edge to edge in the card: out over Section's 16 dp, the first face still lined up with the text
                    if (faces.isNotEmpty()) LazyRow(
                        Modifier.layout { m, c ->
                            val p = 16.dp.roundToPx()
                            val pl = m.measure(c.copy(maxWidth = c.maxWidth + 2 * p, minWidth = c.minWidth + 2 * p))
                            layout(pl.width - 2 * p, pl.height) { pl.place(-p, 0) }
                        },
                        state = row, contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(faces, key = { it.index }) { slot ->
                            var menu by remember { mutableStateOf(false) }
                            Box {
                                val tileAlpha by animateFloatAsState(if (busy) 0.45f else 1f, Motion.quick(), label = "tileAlpha")
                                DialTile(model, slot, Modifier.alpha(tileAlpha).combinedClickable(enabled = !busy,
                                    onLongClick = { menu = true }) { change { activateFace(context, model.store, slot.id) } },
                                    active = slot.id == on.active)
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem({ Text(stringResource(R.string.show_on_watch)) }, { menu = false; change { activateFace(context, model.store, slot.id) } })
                                    DropdownMenuItem({ Text(stringResource(R.string.delete), color = Fmc.danger) }, { menu = false; deleting = slot })
                                }
                            }
                        }
                    }
                    // ponytail: Compose has no scrollbar for a row; a plain thumb, not draggable
                    val info = row.layoutInfo
                    if (faces.isNotEmpty() && info.visibleItemsInfo.size < info.totalItemsCount) {
                        val n = info.totalItemsCount.toFloat()
                        val w = info.visibleItemsInfo.firstOrNull()?.size?.coerceAtLeast(1) ?: 1
                        val start = (row.firstVisibleItemIndex + row.firstVisibleItemScrollOffset.toFloat() / w) / n
                        val shown = (info.viewportEndOffset - info.viewportStartOffset).toFloat() / (w + 14 * LocalDensity.current.density) / n
                        Row(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Fmc.secondary.copy(alpha = 0.2f))) {
                            if (start > 0.001f) Spacer(Modifier.weight(start))
                            Box(Modifier.weight(shown.coerceIn(0.05f, 1f)).fillMaxHeight().clip(CircleShape).background(Fmc.secondary))
                            val rest = 1f - start - shown
                            if (rest > 0.001f) Spacer(Modifier.weight(rest))
                        }
                    }
                    if (reading) Text(stringResource(R.string.reading_watch), color = Fmc.secondary)
                    else if (faces.isEmpty()) Text(stringResource(R.string.connect_to_see_faces), color = Fmc.secondary)
                    readError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
            item {
                LaunchedEffect(model.phase) { if (model.phase is Phase.Checked) FirmwareModel.refresh(model.account.token) }
                Section {
                    NavRow(stringResource(R.string.settings), onClick = { nav.settings = true }) {
                        IconTile(Icons.Default.Settings, Color(0xFF757575))
                    }
                    if (model.phase is Phase.Checked) {
                        HorizontalDivider()
                        NavRow(stringResource(R.string.firmware), count = FirmwareModel.updates(model.loader), onClick = { nav.firmware = true }) {
                            IconTile(painterResource(R.drawable.ic_memory), Color(0xFF757575))
                        }
                    }
                }
            }
        }
    }
}

// Settings

@Composable
fun SettingsScreen(model: AppModel, nav: Nav, signIn: () -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.switchServices(true) }
    val startServices = withBluetooth { notify.launch(Manifest.permission.POST_NOTIFICATIONS) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    Column {
        Bar(stringResource(R.string.settings), back = back)
        LazyColumn {
            item {
                Section(stringResource(R.string.theme)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        val options = listOf(
                            AppTheme.Classic to stringResource(R.string.theme_classic),
                            AppTheme.Material3 to stringResource(R.string.theme_material3),
                        )
                        options.forEachIndexed { i, (mode, label) ->
                            SegmentedButton(
                                selected = model.theme == mode,
                                onClick = { model.selectTheme(mode) },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                                label = { Text(label) },
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = model.theme == AppTheme.Material3,
                        enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                        exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
                    ) {
                        val c = MaterialTheme.colorScheme
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.theme_material3_footer),
                                style = MaterialTheme.typography.bodySmall, color = Fmc.secondary)
                            // The palette the phone gave us, straight from the live colour scheme.
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(c.primary, c.secondary, c.tertiary,
                                    c.primaryContainer, c.secondaryContainer, c.tertiaryContainer)
                                    .forEach { swatch ->
                                        Surface(Modifier.size(28.dp), CircleShape, color = swatch) {}
                                    }
                            }
                        }
                    }
                }
            }
            item {
                Section(stringResource(R.string.services_switch), footer = stringResource(R.string.services_footer)) {
                    val expressive = LocalExpressive.current
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.services_switch), Modifier.weight(1f))
                        Switch(model.services, { on -> if (on) startServices() else model.switchServices(false) },
                            colors = if (!expressive) SwitchDefaults.colors() else SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ))
                    }
                }
            }
            item {
                Section {
                    if (model.phase is Phase.Checked) {
                        NavRow(stringResource(R.string.firmware), count = FirmwareModel.updates(model.loader), onClick = { nav.firmware = true }) {
                            IconTile(painterResource(R.drawable.ic_memory), Color(0xFF757575))
                        }
                        HorizontalDivider()
                    }
                    NavRow(stringResource(R.string.rescue_title), onClick = { nav.rescue = true }) { IconTile(Icons.Default.Warning, Color(0xFFE53935)) }
                    HorizontalDivider()
                    NavRow(stringResource(R.string.developer_options), onClick = { nav.developer = true }) { IconTile(Icons.Default.Build, Color(0xFF757575)) }
                    HorizontalDivider()
                    LanguageRow()
                }
            }
            item {
                Section(stringResource(R.string.fmc_account)) {
                    val user = model.user
                    if (user != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            val avatar = rememberImage(user.avatarUrl, 44.dp)
                            Surface(Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                if (avatar != null) Image(avatar, null, Modifier.size(44.dp), contentScale = ContentScale.Crop)
                                else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimary) }
                            }
                            Column {
                                Text(user.name.ifEmpty { stringResource(R.string.signed_in) }, fontWeight = FontWeight.SemiBold)
                                Text(user.email, style = MaterialTheme.typography.bodyMedium, color = Fmc.secondary)
                            }
                        }
                        HorizontalDivider()
                        Text(stringResource(R.string.sign_out), Modifier.fillMaxWidth().clickable { model.signOut() })
                        HorizontalDivider()
                        Text(stringResource(R.string.delete_account), Modifier.fillMaxWidth().clickable { deleting = true }, color = Fmc.danger)
                    } else {
                        Row(Modifier.fillMaxWidth().clickable(onClick = signIn), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.AccountCircle, null)
                            Text(stringResource(R.string.sign_in), Modifier.weight(1f))
                            Text("›", color = Fmc.secondary)
                        }
                    }
                }
            }
            item {
                Section(stringResource(R.string.about)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.app_name))
                        Text("${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}", color = Fmc.secondary)
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().clickable {
                        val uri = Uri.fromParts("package", context.packageName, null)
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
                    }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(stringResource(R.string.open_app_settings), Modifier.weight(1f))
                        Icon(painterResource(R.drawable.ic_open_in_new), null, tint = Fmc.secondary)
                    }
                }
            }
        }
    }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text(stringResource(R.string.delete_account_title)) },
        text = { Text(stringResource(R.string.delete_account_text)) },
        confirmButton = {
            TextButton({
                deleting = false
                scope.launch { try { model.deleteAccount() } catch (e: Exception) { deleteError = e.message } }
            }) { Text(stringResource(R.string.delete_account), color = Fmc.danger) }
        },
        dismissButton = { TextButton({ deleting = false }) { Text(stringResource(R.string.cancel)) } },
    )
    deleteError?.let {
        AlertDialog(onDismissRequest = { deleteError = null }, title = { Text(stringResource(R.string.delete_account_failed)) },
            text = { Text(it) }, confirmButton = { TextButton({ deleteError = null }) { Text(stringResource(R.string.ok)) } })
    }
}

/** The app's language, set in the system's per-app language page (Android 13+), else the app's settings page. */
@Composable
private fun LanguageRow() {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    Row(Modifier.fillMaxWidth().clickable {
        val uri = Uri.fromParts("package", context.packageName, null)
        context.startActivity(Intent(if (Build.VERSION.SDK_INT >= 33) Settings.ACTION_APP_LOCALE_SETTINGS
            else Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
    }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconTile(painterResource(R.drawable.ic_public), Color(0xFF757575))
        Text(stringResource(R.string.language), Modifier.weight(1f))
        Text(locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }, color = Fmc.secondary)
        Icon(painterResource(R.drawable.ic_open_in_new), null, tint = Fmc.secondary)
    }
}

/** The watch tile from workout-companion's WatchView, with Zeron's capsule button under it. */
@Composable
private fun WatchStatus(model: AppModel, check: () -> Unit) {
    val phase = model.phase
    // the bezel is the layout: the same gap above it and to the button; case and strap get cut by both
    Column(Modifier.padding(bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val context = LocalContext.current
            val on = remember(model.facesRead) { facesOnWatch(context) }
            val active = on.faces.firstOrNull { it.id == on.active }
            // a shorter row than the bezel: the card's top and the button below cut into the watch
            // the bezel lines up with the button's left edge; the case hangs out past it
            WatchFrame(76.dp, Modifier.padding(end = 16.dp)) {
                // the face on screen once connected; till then what the app is doing
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (phase) {
                        Phase.Checking -> CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        is Phase.Checked -> if (active != null) DialPicture(model, active, Modifier.fillMaxSize(), size = 76.dp)
                        else -> Icon(painterResource(R.drawable.ic_watch), null, Modifier.size(24.dp), tint = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
            Column {
                val title = if (model.installing) stringResource(R.string.status_updating) else when (phase) {
                    Phase.Idle -> stringResource(R.string.status_not_connected)
                    Phase.Checking -> stringResource(R.string.status_connecting)
                    is Phase.Checked -> stringResource(R.string.status_connected)
                    is Phase.Failed -> stringResource(R.string.status_failed)
                }
                val (detail, color) = if (model.installing) stringResource(R.string.status_installing_update) to Fmc.secondary else when (phase) {
                    Phase.Idle -> stringResource(R.string.status_tap_connect) to Fmc.secondary
                    Phase.Checking -> stringResource(R.string.status_takes_seconds) to Fmc.secondary
                    is Phase.Checked -> (if (phase.state == null) stringResource(R.string.status_ready) else stringResource(R.string.status_loader_found)) to Fmc.success
                    is Phase.Failed -> phase.message to Fmc.warning
                }
                val animatedColor by animateColorAsState(targetValue = color, animationSpec = Motion.quick(), label = "statusColor")
                // one line each: a taller row would move the watch
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                AnimatedContent(detail, transitionSpec = {
                    (fadeIn(Motion.quick()) + slideInVertically { it / 6 }) togetherWith
                        (fadeOut(Motion.quick()) + slideOutVertically { -it / 6 })
                }, label = "statusDetail") { text ->
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = animatedColor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        // opaque while disabled: the strap runs under it
        Box(Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)) {
            PrimaryButton(if (phase == Phase.Idle) stringResource(R.string.connect) else stringResource(R.string.check_again), phase != Phase.Checking, check)
        }
    }
}

// Sign in

/** Zeron's welcome page (mark, title, tagline, capsule buttons) around the fields. */
@Composable
fun SignInScreen(model: AppModel, done: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val busy = job != null
    LaunchedEffect(Unit) { model.loadProviders() }

    fun run(action: suspend () -> Unit) {
        job = scope.launch {
            try {
                action()
                done()
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                error = e.message ?: str(R.string.sign_in_failed)
            } finally {
                job = null
            }
        }
    }

    Column {
        Bar("", done)
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Badge(64.dp) { Icon(painterResource(R.drawable.ic_watch), null, Modifier.size(32.dp)) }
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.sign_in_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.sign_in_tagline), color = Fmc.secondary, textAlign = TextAlign.Center)
                }
            }
            item {
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.email)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            }
            item {
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.password)) },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            }
            error?.let { item { Text(it, color = Fmc.danger) } }
            item {
                if (busy) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        TextButton({ job?.cancel() }) { Text(stringResource(R.string.cancel)) }
                    }
                } else {
                    PrimaryButton(stringResource(R.string.sign_in), email.isNotBlank() && password.isNotEmpty()) {
                        run { model.signIn(email.trim(), password) }
                    }
                }
            }
            if (model.providers.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text(stringResource(R.string.or), Modifier.padding(horizontal = 12.dp), color = Fmc.secondary)
                        HorizontalDivider(Modifier.weight(1f))
                    }
                }
                items(model.providers.size) { i ->
                    val p = model.providers[i]
                    SecondaryButton(stringResource(R.string.continue_with, p.displayName), !busy) {
                        error = null
                        run {
                            model.signIn(p) { url -> openAuthPage(context, url) }
                            // closes the provider's tab, which sits on top of us in this task
                            context.startActivity(Intent(context, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.sign_up_hint), Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Account.SIGN_UP)))
                }, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * The provider's page in a Custom Tab, the platform's place for OAuth (Google refuses WebViews). The extra is the
 * Custom Tabs protocol itself, no androidx.browser needed; a browser without Custom Tabs opens a plain tab.
 */
private fun openAuthPage(context: android.content.Context, url: String) {
    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    i.putExtras(Bundle().apply { putBinder("android.support.customtabs.extra.SESSION", null) })
    context.startActivity(i)
}

