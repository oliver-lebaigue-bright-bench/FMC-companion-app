package tools.fmc.companion

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tools.fmc.companion.wire.LoaderState
import tools.fmc.companion.wire.dialId
import tools.fmc.companion.wire.recheckDue
import tools.fmc.companion.wire.retry

sealed interface Phase {
    data object Idle : Phase
    data object Checking : Phase

    /** Connected; `state` null = stock firmware (or a stub from before 2026-11-13, which has no K). */
    data class Checked(val state: LoaderState?) : Phase
    data class Failed(val message: String) : Phase
}

/** The app's state, for Compose. Features that need the App Loader show only when `loader` is set. */
class AppModel(app: Application) : AndroidViewModel(app) {
    val store = SecureStore(app)
    val account = Account(store)
    private val prefs = app.getSharedPreferences("app", 0)

    var user by mutableStateOf<User?>(null)
        private set
    var providers by mutableStateOf<List<OAuthProvider>>(emptyList())
        private set
    /** The catalog list on screen, as [query] asked for it (Catalog.kt). */
    var query by mutableStateOf(CatalogQuery())
        private set
    var faces by mutableStateOf<List<Watchface>>(emptyList())
        private set
    var catalogLoading by mutableStateOf(false)
        private set
    /** The last page came short: nothing more to ask for. */
    var catalogEnd by mutableStateOf(false)
        private set
    var catalogError by mutableStateOf<String?>(null)
        private set
    /** Like counts: from each face's likes_count, then moved by the user's own taps. */
    var counts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    /** The signed-in user's likes: face id → like id ("" while the create is in flight). */
    var myLikes by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    /** Faces of the Watch tab's dials, by dial id: from the lists loaded and from the slim index. */
    var dials by mutableStateOf<Map<Long, DialFace>>(emptyMap())
        private set
    private var page = 0
    /** Bumped by each new query: a page that comes back for an older one is dropped. */
    private var generation = 0
    /** Whether the server has watchfaces.likes_count; null until a list says. */
    private var hasLikesCount: Boolean? = null
    private val known = mutableMapOf<String, Watchface>()
    // the first page of each sort and the dial index: the system may clear cacheDir, the next launch refills it
    private val cacheDir = java.io.File(app.cacheDir, "catalog")

    fun likeCount(face: Watchface) = counts[face.id] ?: face.likesCount ?: 0
    fun liked(face: Watchface) = myLikes.containsKey(face.id)

    /** Shows [q]: from its first page again when it changed, else as it is. */
    suspend fun show(q: CatalogQuery) {
        if (q == query && (faces.isNotEmpty() || catalogLoading)) return
        query = q
        generation++
        page = 0
        catalogEnd = false
        catalogError = null
        faces = q.cacheName?.let { cached(it) }?.let { runCatching { Catalog.parse(it) }.getOrNull() } ?: emptyList()
        remember(faces)
        loadPage()
    }

    /** Pull to refresh: the first page again, the list on screen until it comes. */
    suspend fun refresh() {
        generation++
        page = 0
        catalogEnd = false
        loadMyLikes()
        loadPage()
    }

    /** The next page, unless one is coming or the last one came. */
    suspend fun more() {
        if (catalogLoading || catalogEnd || page == 0) return
        loadPage()
    }

    private suspend fun loadPage() {
        val q = query
        val gen = generation
        val next = page + 1
        if (q.filter == null) { faces = emptyList(); catalogEnd = true; return }
        catalogLoading = true
        try {
            val (items, body) = try {
                Catalog.page(q, next, hasLikesCount != false, account.token)
            } catch (e: ServerError) {
                // a server without likes_count refuses that sort: Popular asks again by downloads
                if (e.status != 400 || q.sort != CatalogSort.Popular || hasLikesCount == false) throw e
                hasLikesCount = false
                Catalog.page(q, next, false, account.token)
            }
            if (gen != generation) return
            items.firstOrNull()?.let { hasLikesCount = it.likesCount != null }
            if (next == 1) q.cacheName?.let { store(it, body) }
            faces = if (next == 1) items else faces + items.filter { f -> faces.none { it.id == f.id } }
            page = next
            catalogEnd = items.size < CatalogQuery.PER_PAGE
            catalogError = null
            remember(items)
            if (hasLikesCount == false) runCatching { Catalog.countLikes(items) }.onSuccess { counts = counts + it }
        } catch (e: Exception) {
            if (gen == generation && e !is kotlinx.coroutines.CancellationException) catalogError = e.message ?: str(R.string.catalog_load_failed)
        } finally {
            if (gen == generation) catalogLoading = false
        }
    }

    /** One map write per page instead of one per face, so a page of 40 faces doesn't copy 40 growing maps. */
    private fun remember(faces: List<Watchface>) {
        val newCounts = mutableMapOf<String, Int>()
        val newDials = mutableMapOf<Long, DialFace>()
        faces.forEach { face ->
            known[face.id] = face
            face.likesCount?.let { newCounts[face.id] = it }
            newDials[dialId(face.id)] = DialFace(face.id, face.collectionId, face.name, face.preview)
        }
        if (newCounts.isNotEmpty()) counts = counts + newCounts
        dials = dials + newDials
    }

    /** The signed-in user's own likes, for the hearts; nothing signed out. */
    suspend fun loadMyLikes() {
        val u = user ?: run { myLikes = emptyMap(); return }
        runCatching { Catalog.myLikes(u.id, account.token) }.onSuccess { myLikes = it }
    }

    /** Likes or unlikes as the signed-in user; the count moves at once and back if the server says no. */
    suspend fun toggleLike(face: Watchface) {
        val u = user ?: return
        val mine = myLikes[face.id]
        val count = likeCount(face)
        if (mine == "") return // its create is still in flight
        if (mine != null) {
            myLikes = myLikes - face.id
            counts = counts + (face.id to maxOf(0, count - 1))
        } else {
            myLikes = myLikes + (face.id to "")
            counts = counts + (face.id to count + 1)
        }
        runCatching {
            if (mine != null) Catalog.unlike(mine, account.token)
            else myLikes = myLikes + (face.id to Catalog.like(u.id, face.id, account.token))
        }.onFailure {
            myLikes = if (mine != null) myLikes + (face.id to mine) else myLikes - face.id
            counts = counts + (face.id to count)
        }
    }

    /** An author's published faces, all of them. */
    suspend fun author(id: String): List<Watchface> {
        val faces = Catalog.author(id, account.token)
        remember(faces)
        if (faces.firstOrNull()?.likesCount == null) runCatching { Catalog.countLikes(faces) }.onSuccess { counts = counts + it }
        return faces
    }

    /**
     * Names and pictures for the dials on the watch. Only when one isn't known yet: then the slim index of every face
     * the user may see, from disk, and from the server again if a dial is still unknown and the copy is over ten
     * minutes old (a face installed from the site since).
     */
    suspend fun resolveDials(ids: List<Long>) {
        val missing = { ids.filter { it !in dials && it !in stockDials(getApplication()) } }
        if (missing().isEmpty()) return
        val file = java.io.File(cacheDir, "dial-index.json")
        runCatching { org.json.JSONArray(file.readText()) }.getOrNull()?.let { a ->
            dials = dials + (0 until a.length()).map { DialFace.from(a.getJSONObject(it)) }.associateBy { dialId(it.id) }
            if (missing().isEmpty() || System.currentTimeMillis() - file.lastModified() < 600_000) return
        }
        val index = runCatching { Catalog.dialIndex(account.token) }.getOrNull() ?: return
        dials = dials + index.associateBy { dialId(it.id) }
        store("dial-index", org.json.JSONArray(index.map { it.json() }).toString())
    }

    private fun cached(name: String) = runCatching { java.io.File(cacheDir, "$name.json").readText() }.getOrNull()

    private fun store(name: String, body: String) {
        runCatching {
            cacheDir.mkdirs()
            val tmp = java.io.File(cacheDir, "$name.json.tmp")
            tmp.writeText(body)
            tmp.renameTo(java.io.File(cacheDir, "$name.json"))
        }
    }
    var phase by mutableStateOf<Phase>(Phase.Idle)
        private set
    /** Bumped after each read or change of the faces on the watch: what shows them reads them again. */
    var facesRead by mutableStateOf(0)

    var services by mutableStateOf(prefs.getBoolean("services", false))
        private set

    /** Which design the whole app uses; picked in Settings, remembered across launches. */
    var theme by mutableStateOf(AppTheme.from(prefs.getString("theme", null)))
        private set

    fun selectTheme(value: AppTheme) {
        theme = value
        prefs.edit().putString("theme", value.name).apply()
    }

    /** The miniapp services' foreground service (RpcService); on again at every launch while switched on. */
    fun switchServices(on: Boolean) {
        services = on
        prefs.edit().putBoolean("services", on).apply()
        if (on) RpcService.start(getApplication()) else RpcService.stop(getApplication())
    }

    val loader get() = (phase as? Phase.Checked)?.state

    /** The last read while a new one runs: lists keep their rows through a refresh. */
    private var lastLoader by mutableStateOf<LoaderState?>(null)
    val listedLoader get() = if (phase == Phase.Checking) lastLoader else loader
    // not Idle: without Bluetooth permission the Apps tab never starts a read
    val isChecking get() = phase == Phase.Checking

    init {
        viewModelScope.launch { AppUpdate.check() }
        viewModelScope.launch { WatchLink.back.collect { linkCameBack() } }
        viewModelScope.launch {
            account.restore()
            user = account.user
            reload()
        }
    }

    /** When the last read began (ms). */
    private var lastCheck: Long? = null
    /** A firmware was sent: the watch installs it and restarts, and is read every 20 s till it answers with it. */
    var installing by mutableStateOf(false)
        private set

    /**
     * RpcService has the watch again after a drop (a restart, out of range): read it, so stub, loader and apps aren't
     * the old ones. Not before the user's first read, not twice within 30 s, not during a restart wait; installs and
     * flashes stop the service, so none of them is running. Android shares the LE link between GATT clients.
     */
    fun linkCameBack() {
        if (!recheckDue(phase == Phase.Idle, phase == Phase.Checking || installing, lastCheck, System.currentTimeMillis())) return
        DevLog.log(getApplication(), "link back: reading the watch again")
        check()
    }

    /** After a firmware is sent: every 20 s for up to 10 min, until the watch answers with [stub] or later; not tied
     *  to a screen. */
    fun afterFlash(stub: Long) = viewModelScope.launch {
        installing = true
        WatchLink.using.incrementAndGet()
        val ok = try {
            retry(20_000, 600_000) { check().join(); (loader?.stub?.version ?: 0) >= stub }
        } finally {
            installing = false
            WatchLink.using.decrementAndGet()
        }
        if (ok) FirmwareModel.landed(getApplication())
    }

    fun check() = viewModelScope.launch {
        lastCheck = System.currentTimeMillis()
        phase = Phase.Checking
        phase = try {
            Phase.Checked(readWatch(getApplication(), store))
        } catch (e: Exception) {
            Phase.Failed(e.message ?: str(R.string.watch_read_failed))
        }
        lastLoader = loader
        // an order the watch refused with Busy goes now
        if (AppOrder.unsent) viewModelScope.launch { AppOrder.send(getApplication(), this@AppModel) }
        if (phase is Phase.Checked) Analytics.track("watch_connected", mapOf("firmware" to if (loader == null) "stock" else "patched"))
    }

    suspend fun reload() = loadMyLikes()

    /** A face from a list loaded, else from the server: a deep link can name one before any list. */
    suspend fun face(id: String): Watchface = known[id] ?: Catalog.face(id, account.token).also {
        remember(listOf(it))
        if (it.likesCount == null) runCatching { Catalog.countLikes(listOf(it)) }.onSuccess { n -> counts = counts + n }
    }

    suspend fun loadProviders() {
        providers = account.providers()
    }

    suspend fun signIn(email: String, password: String) {
        account.signIn(email, password)
        signedIn()
    }

    suspend fun signIn(provider: OAuthProvider, open: (String) -> Unit) {
        account.signIn(provider, open)
        signedIn()
    }

    private suspend fun signedIn() {
        user = account.user
        reload()
    }

    fun signOut() {
        account.signOut()
        user = null
        myLikes = emptyMap()
    }

    suspend fun deleteAccount() {
        account.deleteAccount()
        signOut()
    }
}
