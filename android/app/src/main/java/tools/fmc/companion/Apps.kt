package tools.fmc.companion

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import tools.fmc.companion.wire.setOrder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import java.net.URLEncoder
import org.json.JSONObject
import tools.fmc.companion.wire.LoaderState
import tools.fmc.companion.wire.PackageHeader
import tools.fmc.companion.wire.parseHeader
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Badge as CountPill

// The apps on the watch and what each may use on the phone (docs/miniapp-rpc.md, "Permissions"): a list of apps,
// and per app "Allow <app> to access" with a switch per permission.

/** An app on the watch, or one that has answers on the phone but is not on the watch now. */
class WatchApp(val name: String, val header: PackageHeader? = null, val slot: Int? = null) {
    /** The store's name for it, else the package name. */
    val title get() = StoreApps.listing(name)?.name ?: name
    val detail get() = when {
        header == null || slot == null -> str(R.string.not_on_watch)
        update != null -> str(R.string.app_version_update, versionText(header.version), versionText(update!!.version), slot)
        else -> str(R.string.app_version, versionText(header.version), slot)
    }
    /** The store's build when it's newer than the one on the watch. */
    val update get() = header?.let { h -> StoreApps.latest(name)?.takeIf { it.version > h.version } }

    companion object {
        fun all(loader: LoaderState?, grants: Grants): List<WatchApp> {
            val installed = (loader?.apps ?: emptyMap()).toSortedMap().map { (slot, h) -> WatchApp(h.name, h, slot) }
            val names = installed.map { it.name }.toSet()
            return installed + grants.packages().filter { it !in names }.map { WatchApp(it) }
        }

        /** Every app in the FMC store, the ones on the watch with their version and slot, then the installed and asked ones the store doesn't list. */
        fun store(loader: LoaderState?, grants: Grants): List<WatchApp> = StoreApps.all.map { l ->
            loader?.apps?.entries?.firstOrNull { StoreApps.listing(it.value.name)?.slug == l.slug }
                ?.let { WatchApp(it.value.name, it.value, it.key) } ?: WatchApp(l.slug)
        } + all(loader, grants).filter { StoreApps.listing(it.name) == null }
    }
}

/** The FMC store's listing of watch apps (fmc.tools `apps`), matched to a package by slug or name. Loaded once per launch. */
/** The header's u32 version as the store's MAJOR.MINOR.PATCH (apps/scripts/store build_number), else the number. */
fun versionText(v: Long) = if (v >= 1024) "${v shr 20}.${v shr 10 and 1023}.${v and 1023}" else "$v"

object StoreApps {
    class Listing(val id: String, val slug: String, val name: String, val icon: String?, val pkg: String?,
                  val summary: String = "", val description: String = "", val author: String = "", val version: String = "",
                  val abi: String = "", val source: String = "", val downloads: Int = 0, val screenshots: List<String> = emptyList()) {
        val page get() = "$FMC/apps/$id"
        /** The download hook: counts the install as a download and redirects to the package. */
        val download get() = "$FMC/api/apps/$id/download"
    }

    private var byKey by mutableStateOf<Map<String, Listing>>(emptyMap())
    /** slug → the store package's header. Its u32 version, not the listing's text: SDK examples don't encode it. */
    private var headers by mutableStateOf<Map<String, PackageHeader>>(emptyMap())

    /** By name, for the All segment. */
    val all get() = byKey.values.distinct().sortedBy { it.name.lowercase() }
    var loaded by mutableStateOf(false)
        private set

    fun listing(pkg: String) = byKey[pkg.lowercase()]
    fun latest(pkg: String) = listing(pkg)?.let { headers[it.slug] }

    suspend fun load() {
        if (loaded) return
        val filter = URLEncoder.encode("published = true", "UTF-8")
        val body = runCatching {
            http("GET", "$FMC/api/collections/apps/records?filter=$filter&perPage=200&fields=id,collectionId,slug,name,icon,package,summary,description,author,version,abi,source,downloads,screenshots")
        }.getOrNull() ?: return
        byKey = parse(body)
        loaded = true
        // ponytail: one 64-byte request per store app, a few dozen at most; a version field with the header's u32 on
        // the store record would make this one request
        headers = coroutineScope {
            all.map { l -> async { l.pkg?.let { runCatching { parseHeader(download(it, 64)) }.getOrNull() }?.let { l.slug to it } } }
                .awaitAll().filterNotNull().toMap()
        }
    }

    /** slug and name, lowercased → listing. */
    fun parse(json: String): Map<String, Listing> {
        val items = JSONObject(json).getJSONArray("items")
        val out = mutableMapOf<String, Listing>()
        for (i in 0 until items.length()) {
            val r = items.getJSONObject(i)
            val icon = r.optString("icon")
            // ponytail: BitmapFactory has no SVG decoder, those apps keep the letter tile
            val files = "$FMC/api/files/${r.getString("collectionId")}/${r.getString("id")}"
            val url = if (icon.isEmpty() || icon.endsWith(".svg", ignoreCase = true)) null else "$files/$icon"
            val pkg = r.optString("package").takeIf { it.isNotEmpty() }?.let { "$files/$it" }
            val shots = r.optJSONArray("screenshots")?.let { a -> (0 until a.length()).map { "$files/${a.getString(it)}" } }.orEmpty()
            val l = Listing(r.getString("id"), r.getString("slug"), r.getString("name"), url, pkg, r.optString("summary"),
                r.optString("description"), r.optString("author"), r.optString("version"), r.optString("abi"),
                r.optString("source"), r.optInt("downloads"), shots)
            out[r.getString("slug").lowercase()] = l
            out[r.getString("name").lowercase()] = l
        }
        return out
    }
}

/** Grants, recomposing the caller when the service writes an ask or the user comes back from system settings. */
@Composable
fun liveGrants(): Grants {
    val context = LocalContext.current
    val grants = remember { Grants(context) }
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(grants) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> tick++ }
        grants.prefs.registerOnSharedPreferenceChangeListener(l)
        onDispose { grants.prefs.unregisterOnSharedPreferenceChangeListener(l) }
    }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose {} }
    @Suppress("UNUSED_EXPRESSION") tick // read: a non-Unit composable recomposes its caller
    return grants
}

/** A watch app's icon, always a circle: its store icon, or its first letter in white on grey. */
@Composable
fun AppIcon(name: String, side: Dp = 40.dp) {
    val bitmap = rememberImage(StoreApps.listing(name)?.icon, side)
    // systemGray2 behind the letter, as on iOS; nothing behind a store icon
    Surface(Modifier.size(side), shape = CircleShape, color = if (bitmap == null) Color(0xFF8E8E93) else Color.Transparent,
        contentColor = Color.White) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap, null, Modifier.size(side), contentScale = ContentScale.Crop)
            else Text(name.take(1).uppercase(), fontSize = (side.value * 0.45f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** A white symbol on a coloured circle, as Android's settings rows show them. */
@Composable
fun IconTile(icon: ImageVector, color: Color) = IconTile(rememberVectorPainter(icon), color)

@Composable
fun IconTile(painter: androidx.compose.ui.graphics.painter.Painter, color: Color) =
    Surface(Modifier.size(36.dp), shape = CircleShape, color = color, contentColor = Color.White) {
        Box(contentAlignment = Alignment.Center) { Icon(painter, null, Modifier.size(20.dp)) }
    }

/** A tappable row: leading tile, title (and a line under it), the count of unanswered asks, the chevron. */
@Composable
fun NavRow(title: String, detail: String? = null, count: Int = 0, onClick: () -> Unit,
           trailing: (@Composable () -> Unit)? = null, leading: @Composable () -> Unit) =
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(title)
            detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Fmc.secondary) }
        }
        if (count > 0) CountPill { Text("$count") }
        trailing?.invoke()
        Text("›", color = Fmc.secondary)
    }

/** The order of the apps on the watch, as package names, kept on the phone per watch (prefs "app": "watch" the last
 *  one read, "order|<watch>" its names); the Installed list shows it. */
object AppOrder {
    private fun prefs(context: Context) = context.getSharedPreferences("app", 0)
    fun watch(context: Context, id: String?) {
        prefs(context).edit().putString("watch", id).apply()
        names = of(context)
    }

    fun of(context: Context): List<String> = prefs(context).let { p ->
        p.getString("order|${p.getString("watch", "")}", null)?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    /** The saved order of the watch read last (empty before a read: the shipped loader's slot order). */
    @Volatile var names: List<String> = emptyList()
        private set
    /** Changed on the phone and not on the watch yet: sent on Done, again after a Busy on the next read. */
    @Volatile var unsent = false
        private set
    var sending by mutableStateOf(false)
        private set
    /** Why the last send didn't reach the watch. */
    var status by mutableStateOf<String?>(null)
        private set

    /** The loader package with this order in it (cmf-watch-sdk docs/app-order.md); as it is when it has no table.
     *  Every loader the phone installs goes through here, or an update would put the menu back in slot order. */
    fun patched(loader: ByteArray): ByteArray = runCatching { setOrder(loader, names.take(16)) }.getOrDefault(loader)

    /** The saved order first, then the apps it doesn't name, as they came (by slot). */
    fun sorted(apps: List<WatchApp>, names: List<String>) =
        apps.sortedBy { a -> names.indexOf(a.name).let { if (it < 0) Int.MAX_VALUE else it } }

    fun set(context: Context, names: List<String>) {
        prefs(context).let { p -> p.edit().putString("order|${p.getString("watch", "")}", names.joinToString("\n")).apply() }
        this.names = names
        unsent = true
    }

    /** The order goes inside the loader: the store's loader with the table patched, installed to slot 0xFF. */
    suspend fun send(context: Context, model: AppModel) {
        if (!unsent || sending || model.loader?.loader == null) return
        sending = true
        try {
            if (FirmwareModel.check?.loaderPackage == null) FirmwareModel.refresh(model.account.token)
            val loader = FirmwareModel.check?.loaderPackage
                ?: return run { status = str(R.string.order_loader_missing) }
            kInstall(context, model, setOrder(loader, names.take(16))) {}
            DevLog.log(context, "app order sent: ${names.joinToString()}")
            unsent = false
            status = null
            model.check()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: tools.fmc.companion.wire.InstallError.Busy) {
            // the menu holds the loader while it is open; tried again on the next read of the watch
            status = str(R.string.order_busy)
        } catch (_: tools.fmc.companion.wire.OrderError) {
            status = str(R.string.order_old_loader)
        } catch (e: Exception) {
            status = str(R.string.order_send_failed, e.message.toString())
        } finally {
            sending = false
        }
    }
}

@Composable
fun AppsScreen(model: AppModel, open: (String) -> Unit) {
    val context = LocalContext.current
    val grants = liveGrants()
    LaunchedEffect(Unit) { StoreApps.load() }
    // the list of installed apps is the watch's: read it here too, not only from Connect on the Watch tab
    // ponytail: only when Bluetooth is already allowed; the Watch tab's Connect asks for it
    LaunchedEffect(Unit) {
        val bt = listOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN)
            .all { androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (bt && model.phase == Phase.Idle) model.check()
    }
    var showAll by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(AppOrder.of(context)) }
    val scope = rememberCoroutineScope()
    // Installed is the watch's list: nothing until it is read, not the apps that only asked for something
    val loader = model.listedLoader
    val apps = if (showAll) WatchApp.store(loader, grants) else if (loader == null) emptyList() else WatchApp.all(loader, grants).filter { it.slot != null }
    // first load: placeholder rows; a refresh keeps the rows it has
    val loading = apps.isEmpty() && if (showAll) !StoreApps.loaded else model.isChecking
    val on = AppOrder.sorted(apps.filter { it.slot != null }, order)
    // one prefs snapshot for the whole list, not a fresh scan of every grant for every row
    val pendingCounts = grants.pendingCounts()
    Column {
        Bar(stringResource(R.string.apps)) {
            if (!showAll && on.isNotEmpty()) TextButton({
                if (editing) scope.launch { AppOrder.send(context, model) }
                editing = !editing
            }) { Text(if (editing) stringResource(R.string.done) else stringResource(R.string.edit)) }
        }
        LazyColumn {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    listOf(stringResource(R.string.installed), stringResource(R.string.shelf_all)).forEachIndexed { i, label ->
                        SegmentedButton(showAll == (i == 1), { showAll = i == 1; editing = false },
                            SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
                    }
                }
            }
            fun group(title: String, list: List<WatchApp>, footer: String? = null) = item(title) {
                Section(title, footer) {
                    list.forEachIndexed { i, app ->
                        if (i > 0) HorizontalDivider()
                        NavRow(app.title, app.detail, pendingCounts[app.name] ?: 0, { open(if (showAll) "store:${app.name}" else app.name) },
                            trailing = if (app.update != null || (app.slot == null && StoreApps.listing(app.name)?.pkg != null)) {{ GetButton(model, app) }}
                                else if (showAll && app.slot != null) {{ Text(stringResource(R.string.installed), color = Fmc.secondary, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall) }} else null) { AppIcon(app.name) }
                    }
                }
            }
            val (updates, current) = on.partition { it.update != null }
            if (showAll) { if (apps.isNotEmpty()) group(str(R.string.fmc_store), apps, if (model.listedLoader == null) str(R.string.connect_to_install_apps) else null) }
            else {
                // one list while editing: the order is of every app on the watch
                if (editing) item("order") {
                    Section(stringResource(R.string.on_the_watch), stringResource(R.string.order_footer)) {
                        on.forEachIndexed { i, app ->
                            if (i > 0) HorizontalDivider()
                            fun move(to: Int) {
                                val names = on.map { it.name }.toMutableList().apply { add(to, removeAt(i)) }
                                order = names
                                AppOrder.set(context, names)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                AppIcon(app.name)
                                Text(app.title, Modifier.weight(1f))
                                IconButton({ move(i - 1) }, enabled = i > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up, app.title))
                                }
                                IconButton({ move(i + 1) }, enabled = i < on.size - 1) {
                                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down, app.title))
                                }
                            }
                        }
                    }
                } else if (updates.isNotEmpty()) group(str(R.string.updates), updates, str(R.string.updates_footer))
                if (!editing && current.isNotEmpty()) group(str(R.string.on_the_watch), current,
                    if (AppOrder.sending) str(R.string.sending_order) else AppOrder.status)
            }
            if (loading) item {
                Section {
                    repeat(6) { i ->
                        if (i > 0) HorizontalDivider()
                        Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            val tone = Fmc.secondary.copy(alpha = 0.15f)
                            Box(Modifier.size(40.dp).clip(CircleShape).background(tone))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.width(140.dp).height(14.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).background(tone))
                                Box(Modifier.width(100.dp).height(10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).background(tone))
                            }
                        }
                    }
                }
            } else if (apps.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.no_apps), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (showAll) stringResource(R.string.store_load_failed) else stringResource(R.string.connect_to_see_apps),
                        color = Fmc.secondary, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun AppScreen(model: AppModel, value: String, open: (String) -> Unit, back: () -> Unit) {
    // "headers:<name>|<host>": the headers the app's requests to that host carry
    if (value.startsWith("headers:")) {
        val (pkg, host) = value.removePrefix("headers:").split("|", limit = 2)
        return HeadersScreen(pkg, host, back)
    }
    // "store:<name>" from All: the store page; a bare name: the app's settings (permissions, Update, Delete)
    val store = value.startsWith("store:")
    val name = value.removePrefix("store:")
    val context = LocalContext.current
    val grants = liveGrants()
    LaunchedEffect(Unit) { StoreApps.load() }
    var resetting by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        RpcService.start(context) // location adds its foreground service type
    }
    fun askOs(cap: String) {
        if (osAllows(context, cap)) return
        osPermission(cap)?.let { return ask.launch(it) }
        if (cap == "notifications" || cap == "media") context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, NotificationWatcher::class.java).flattenToString()))
    }
    fun openSettings() = context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))

    val app = WatchApp.all(model.listedLoader, grants).firstOrNull { it.name == name } ?: WatchApp(name)
    val listing = StoreApps.listing(name)
    val caps = grants.caps(name)
    val allowed = grants.of(name)
    val offInOs = caps.filter { allowed[it] == true && !osAllows(context, it) }
    Column {
        Bar(app.title, back) {
            listing?.takeIf { store }?.let { l ->
                IconButton({ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(l.page))) }) {
                    Icon(painterResource(R.drawable.ic_open_in_new), stringResource(R.string.open_on_fmc))
                }
            }
        }
        LazyColumn {
            if (store && listing != null) {
            // the App Store's header: icon, name, summary, the button
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppIcon(name, 108.dp)
                    Column(Modifier.weight(1f).heightIn(min = 108.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(app.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2)
                        if (!listing?.summary.isNullOrEmpty()) Text(listing!!.summary, color = Fmc.secondary,
                            style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        if (listing != null || app.slot != null) GetButton(model, app, large = true)
                        if (listing?.pkg != null && model.listedLoader == null) Text(stringResource(R.string.connect_to_install),
                            style = MaterialTheme.typography.bodySmall, color = Fmc.secondary)
                    }
                }
            }
                item { Column { InfoStrip(app, listing) } }
                if (listing.screenshots.isNotEmpty()) item {
                    androidx.compose.foundation.lazy.LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(listing.screenshots.size) { i ->
                            val img = rememberImage(listing.screenshots[i], 180.dp)
                            Box(Modifier.size(180.dp).clip(CircleShape).background(Fmc.secondary.copy(alpha = 0.15f))) {
                                img?.let { Image(it, null, Modifier.size(180.dp), contentScale = ContentScale.Crop) }
                            }
                        }
                    }
                }
                if (listing.description.isNotEmpty()) item {
                    Section(stringResource(R.string.description)) { SelectionContainer { com.mikepenz.markdown.m3.Markdown(listing.description) } }
                }
                item {
                    Section(stringResource(R.string.information)) {
                        val rows = buildList {
                            if (listing.author.isNotEmpty()) add(stringResource(R.string.app_developer) to listing.author)
                            if (listing.version.isNotEmpty()) add(stringResource(R.string.version) to listing.version)
                            app.header?.let { h -> add(stringResource(R.string.on_the_watch) to stringResource(R.string.slot_value, versionText(h.version), app.slot ?: 0)) }
                            if (listing.abi.isNotEmpty()) add(stringResource(R.string.loader_abi) to listing.abi)
                        }
                        rows.forEachIndexed { i, (k, v) ->
                            if (i > 0) HorizontalDivider()
                            Row { Text(k, Modifier.weight(1f)); Text(v, color = Fmc.secondary) }
                        }
                        if (listing.source.isNotEmpty()) {
                            if (rows.isNotEmpty()) HorizontalDivider()
                            Row(Modifier.fillMaxWidth().clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(listing.source)))
                            }, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.source_code), Modifier.weight(1f))
                                Icon(painterResource(R.drawable.ic_open_in_new), null, tint = Fmc.secondary)
                            }
                        }
                    }
                }
            } else {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon(name, 72.dp)
                    Text(app.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(app.detail, color = Fmc.secondary)
                }
            }
            if (app.update != null || listing != null) item {
                Section {
                    app.update?.let { new ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.version_out, versionText(new.version)), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            GetButton(model, app)
                        }
                        if (listing != null) HorizontalDivider()
                    }
                    if (listing != null) Text(stringResource(R.string.view_in_store), Modifier.fillMaxWidth().clickable { open("store:$name") })
                }
            }
            app.slot?.let { slot ->
                item {
                    var confirm by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    Section(footer = stringResource(R.string.delete_app_footer, app.title)) {
                        Text(stringResource(R.string.delete_from_watch), Modifier.fillMaxWidth().clickable(enabled = name !in AppUpdates.progress) { confirm = true },
                            color = Fmc.danger)
                    }
                    if (confirm) DeleteDialog(app, { confirm = false }) { scope.launch { AppUpdates.remove(context, model, name, slot) } }
                }
            }
            item {
                // background jobs: the app's own refreshes, each with a switch
                var jobs by remember { mutableStateOf(Jobs(context).of(name)) }
                if (jobs.isNotEmpty()) Section(stringResource(R.string.background_refresh),
                    footer = stringResource(R.string.background_refresh_footer, app.title)) {
                    jobs.forEachIndexed { i, j ->
                        if (i > 0) HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(jobLabel(j), Modifier.weight(1f))
                            Switch(j.on, { on -> Jobs(context).setOn(name, j.id, on); jobs = Jobs(context).of(name) })
                        }
                    }
                }
            }
            if (caps.isEmpty()) item {
                Section { Text(stringResource(R.string.asked_nothing, app.title), color = Fmc.secondary) }
            } else {
                item {
                    Section(stringResource(R.string.allow_access, app.title),
                        footer = (if (offInOs.isEmpty()) "" else stringResource(R.string.some_off_in_settings) + "\n\n") + stringResource(R.string.nearby_warning)) {
                        caps.forEachIndexed { i, cap ->
                            if (i > 0) HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                CapTile(cap)
                                Column(Modifier.weight(1f)) {
                                    Text(capTitle(cap))
                                    when {
                                        grants.isPending(name, cap) -> Text(stringResource(R.string.waiting_answer),
                                            style = MaterialTheme.typography.bodyMedium, color = Fmc.warning)
                                        cap in offInOs -> Text(stringResource(R.string.off_in_settings),
                                            Modifier.clickable { askOs(cap) }, style = MaterialTheme.typography.bodyMedium,
                                            color = Fmc.warning)
                                    }
                                }
                                Switch(allowed[cap] == true, { on ->
                                    grants.set(name, cap, on)
                                    if (on) askOs(cap)
                                })
                            }
                            if (cap.startsWith("http:")) {
                                val host = cap.removePrefix("http:")
                                val n = grants.headers(name, host).size
                                Row(Modifier.fillMaxWidth().padding(start = 50.dp).clickable { open("headers:$name|$host") },
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.headers), Modifier.weight(1f))
                                    Text(if (n == 0) stringResource(R.string.none) else "$n", color = Fmc.secondary)
                                    Text("  ›", color = Fmc.secondary)
                                }
                            }
                        }
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            IconTile(Icons.Default.Email, Color(0xFF757575))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.show_text))
                                Text(stringResource(R.string.show_text_detail), style = MaterialTheme.typography.bodyMedium, color = Fmc.secondary)
                            }
                            Switch(grants.showText(name), { grants.setShowText(name, it) })
                        }
                        if (offInOs.isNotEmpty()) {
                            HorizontalDivider()
                            Text(stringResource(R.string.open_app_settings), Modifier.fillMaxWidth().clickable { openSettings() })
                        }
                    }
                }
                item {
                    Section(footer = stringResource(R.string.asks_again, app.title)) {
                        Text(stringResource(R.string.reset_permissions), Modifier.fillMaxWidth().clickable { resetting = true }, color = Fmc.danger)
                    }
                }
            }
            }
        }
    }
    if (resetting) AlertDialog(
        onDismissRequest = { resetting = false },
        title = { Text(stringResource(R.string.reset_permissions_title, name)) },
        text = { Text(stringResource(R.string.asks_again, app.title)) },
        confirmButton = { TextButton({ resetting = false; grants.reset(name) }) { Text(stringResource(R.string.reset), color = Fmc.danger) } },
        dismissButton = { TextButton({ resetting = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

fun capTitle(cap: String) = when (cap) {
    "calendar" -> str(R.string.cap_calendar)
    "location" -> str(R.string.cap_location)
    "weather" -> str(R.string.cap_weather)
    "notifications" -> str(R.string.cap_notifications)
    "notify" -> str(R.string.cap_notify)
    "media" -> str(R.string.cap_media)
    else -> cap.removePrefix("http:")
}

@Composable
private fun CapTile(cap: String) = when (cap) {
    "calendar" -> IconTile(Icons.Default.DateRange, Color(0xFFE53935))
    "location" -> IconTile(Icons.Default.LocationOn, Color(0xFF1E88E5))
    "weather" -> IconTile(painterResource(R.drawable.ic_cloud), Color(0xFF00ACC1))
    "notifications" -> IconTile(Icons.Default.Notifications, Color(0xFFE53935))
    "notify" -> IconTile(Icons.Default.Notifications, Color(0xFFFB8C00))
    "media" -> IconTile(Icons.Default.PlayArrow, Color(0xFFD81B60))
    else -> IconTile(painterResource(R.drawable.ic_public), Color(0xFF757575))
}

/** Store installs and updates: the package over K into the app's own slot, else the first free one. */
object AppUpdates {
    /** name → 0…1 while installing */
    val progress = androidx.compose.runtime.mutableStateMapOf<String, Double>()
    var error by mutableStateOf<String?>(null)

    suspend fun update(context: Context, model: AppModel, name: String) {
        val listing = StoreApps.listing(name)?.takeIf { it.pkg != null } ?: return
        if (name in progress) return
        progress[name] = 0.0
        try {
            val pkg = download(listing.download)
            kInstall(context, model, pkg) { progress[name] = it }
            DevLog.log(context, "$name installed")
            Analytics.track("app_installed", mapOf("app" to listing.slug))
            model.check()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: tools.fmc.companion.wire.InstallError.Busy) {
            error = str(R.string.app_busy, listing.name)
        } catch (e: tools.fmc.companion.wire.InstallError.NoFreeSlot) {
            error = appContext.resources.getQuantityString(R.plurals.no_free_slot, e.slots, e.slots)
        } catch (e: Exception) {
            error = str(R.string.app_install_failed, listing.name, e.message.toString())
        } finally {
            progress.remove(name)
        }
    }

    suspend fun remove(context: Context, model: AppModel, name: String, slot: Int) {
        if (name in progress) return
        val title = StoreApps.listing(name)?.name ?: name
        progress[name] = 0.0
        try {
            kRun(context, model) { tools.fmc.companion.wire.remove(it, slot) }
            DevLog.log(context, "$name removed from slot $slot")
            Analytics.track("app_removed", mapOf("app" to name))
            // as phones do on uninstall: a reinstall asks again
            Grants(context).reset(name)
            appStorage(context).clear(name) // its data on the phone goes with it; Reset permissions keeps it
            model.check()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: tools.fmc.companion.wire.InstallError.Busy) {
            // the stub answers BUSY to removing a page's app even behind the face (cmf-watch-sdk docs/app-guide.md)
            error = str(R.string.app_running, title)
        } catch (e: Exception) {
            error = str(R.string.app_delete_failed, title, e.message.toString())
        } finally {
            progress.remove(name)
        }
    }
}

@Composable
fun DeleteDialog(app: WatchApp, dismiss: () -> Unit, delete: () -> Unit) = AlertDialog(onDismissRequest = dismiss,
    title = { Text(stringResource(R.string.delete_app_title, app.title)) }, text = { Text(stringResource(R.string.delete_app_text)) },
    confirmButton = { TextButton({ dismiss(); delete() }) { Text(stringResource(R.string.delete), color = Fmc.danger) } },
    dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } })

/** The App Store's button: Get, Update or Installed, and the progress while it installs; failures show as a dialog. */
@Composable
fun GetButton(model: AppModel, app: WatchApp, large: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val p = AppUpdates.progress[app.name]
    val width = if (large) 84.dp else 72.dp
    when {
        p != null -> LinearProgressIndicator({ p.toFloat() }, Modifier.width(width))
        app.slot != null && app.update == null ->
            Text(stringResource(R.string.installed), Modifier.width(width), color = Fmc.secondary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        else -> androidx.compose.material3.Button({ scope.launch { AppUpdates.update(context, model, app.name) } },
            Modifier.widthIn(min = width).height(if (large) 36.dp else 30.dp),
            enabled = model.loader != null && StoreApps.listing(app.name)?.pkg != null,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
            Text(if (app.slot == null) stringResource(R.string.get) else stringResource(R.string.update), fontWeight = FontWeight.Bold)
        }
    }
    AppUpdates.error?.let {
        AlertDialog(onDismissRequest = { AppUpdates.error = null }, title = { Text(stringResource(R.string.install_failed)) },
            text = { Text(it) }, confirmButton = { TextButton({ AppUpdates.error = null }) { Text(stringResource(R.string.ok)) } })
    }
}

/** The App Store's row of facts under the header. */
@Composable
private fun ColumnScope.InfoStrip(app: WatchApp, listing: StoreApps.Listing) {
    @Composable
    fun cell(title: String, value: String, caption: String? = null) =
        Column(Modifier.width(104.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Fmc.secondary, fontWeight = FontWeight.SemiBold)
            Text(value, Modifier.height(30.dp), style = MaterialTheme.typography.titleLarge, color = Fmc.secondary, fontWeight = FontWeight.Bold, maxLines = 1)
            caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Fmc.secondary, maxLines = 1) }
        }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top) {
        val cells = buildList<@Composable () -> Unit> {
            add { cell(stringResource(R.string.info_downloads), compact(listing.downloads)) }
            if (listing.version.isNotEmpty()) add { cell(stringResource(R.string.info_version), listing.version) }
            if (listing.author.isNotEmpty()) add { cell(stringResource(R.string.info_developer), "👤", listing.author) }
            app.slot?.let { add { cell(stringResource(R.string.info_slot), "$it", stringResource(R.string.info_on_watch)) } }
            if (listing.abi.isNotEmpty()) add { cell("ABI", listing.abi) }
        }
        cells.forEachIndexed { i, c ->
            if (i > 0) androidx.compose.material3.VerticalDivider(Modifier.padding(top = 10.dp).height(36.dp))
            c()
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

/** Name/value pairs the app's requests to `host` carry (an API key, `Authorization: Bearer …`). Kept encrypted on the
 *  phone, never sent to the watch; saved as typed. */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun HeadersScreen(pkg: String, host: String, back: () -> Unit) {
    val context = LocalContext.current
    val grants = remember { Grants(context) }
    val rows = remember { androidx.compose.runtime.mutableStateListOf(*grants.headers(pkg, host).toTypedArray()) }
    // Keystrokes only touch Compose state; the Keystore round trip and write happen once typing pauses.
    LaunchedEffect(Unit) {
        snapshotFlow { rows.map { it.first to it.second } }
            .debounce(300)
            .collect { list -> grants.setHeaders(pkg, host, list.filter { it.first.isNotBlank() }) }
    }
    Column {
        Bar(stringResource(R.string.headers), back)
        LazyColumn {
            item {
                Section(host, footer = stringResource(R.string.headers_footer, pkg, host)) {
                    rows.forEachIndexed { i, (k, v) ->
                        if (i > 0) HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                androidx.compose.material3.OutlinedTextField(k, { rows[i] = it to v }, Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.header_name)) }, singleLine = true)
                                androidx.compose.material3.OutlinedTextField(v, { rows[i] = k to it }, Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.header_value)) }, singleLine = true)
                            }
                            TextButton({ rows.removeAt(i) }) { Text(stringResource(R.string.delete), color = Fmc.danger) }
                        }
                    }
                    if (rows.isNotEmpty()) HorizontalDivider()
                    Text(stringResource(R.string.add_header), Modifier.fillMaxWidth().clickable { rows.add("" to "") }, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
