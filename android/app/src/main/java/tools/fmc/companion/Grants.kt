package tools.fmc.companion

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONArray
import tools.fmc.companion.wire.RefreshJob

// Who may call which miniapp service (docs/miniapp-rpc.md, "Permissions"). Keyed by the package name in the slot's
// header. SharedPreferences "grants": "g|pkg|cap" = allowed, "p|pkg|cap" = asked at (ms, pending until answered),
// "show|pkg" = text on the watch. The headers the user set per (pkg, host) are secrets: SecureStore "h|pkg|host".

class Grants(context: Context) {
    val prefs = context.getSharedPreferences("grants", Context.MODE_PRIVATE)!!
    private val secure = SecureStore(context)
    private val jobs = Jobs(context)

    private fun parts(prefix: String) = prefs.all.keys.filter { it.startsWith(prefix) }
        .map { it.removePrefix(prefix).split("|", limit = 2) }.filter { it.size == 2 }.map { it[0] to it[1] }

    /** One snapshot of the prefs: no getBoolean round trip per cap. */
    fun of(pkg: String): Map<String, Boolean> {
        val all = prefs.all
        val prefix = "g|$pkg|"
        return all.keys.filter { it.startsWith(prefix) }.associate { it.removePrefix(prefix) to (all[it] as? Boolean == true) }
    }

    /** Every package that asked for something or has an answer. */
    fun packages(): List<String> = (parts("g|") + parts("p|")).map { it.first }.distinct().sorted()

    /** Every cap the package asked for or has an answer for, from a single prefs snapshot. */
    fun caps(pkg: String): List<String> {
        val keys = prefs.all.keys
        return (keys.filter { it.startsWith("g|$pkg|") }.map { it.removePrefix("g|$pkg|") } +
            keys.filter { it.startsWith("p|$pkg|") }.map { it.removePrefix("p|$pkg|") }).distinct().sorted()
    }

    fun isPending(pkg: String, cap: String) = prefs.contains("p|$pkg|$cap") && !prefs.contains("g|$pkg|$cap")

    fun pendingCount(pkg: String) = caps(pkg).count { isPending(pkg, it) }

    /** Forgets every answer, the headers and the cookies: the next calls ask again. */
    fun reset(pkg: String) {
        caps(pkg).filter { it.startsWith("http:") }.forEach { secure["h|$pkg|${it.removePrefix("http:")}"] = null }
        Cookies.clear(pkg)
        jobs.clear(pkg)
        val e = prefs.edit()
        prefs.all.keys.filter { it.startsWith("g|$pkg|") || it.startsWith("p|$pkg|") || it == "show|$pkg" }.forEach { e.remove(it) }
        e.apply()
    }

    /** (pkg, cap) asked and not answered yet. One prefs snapshot, not one per pending cap. */
    fun pending(): List<Pair<String, String>> {
        val all = prefs.all
        val answered = all.keys.filter { it.startsWith("g|") }.map { it.removePrefix("g|") }.toSet()
        return all.keys.filter { it.startsWith("p|") }.mapNotNull { key ->
            val cap = key.removePrefix("p|")
            val pair = cap.split("|", limit = 2)
            pair.takeIf { it.size == 2 && cap !in answered }?.let { it[0] to it[1] }
        }
    }

    /** Pending asks per package, computed once for a whole list instead of once per row. */
    fun pendingCounts(): Map<String, Int> = pending().groupingBy { it.first }.eachCount()

    fun set(pkg: String, cap: String, allow: Boolean) =
        prefs.edit().putBoolean("g|$pkg|$cap", allow).remove("p|$pkg|$cap").apply()

    /** true when the pair was not asked in the last day: the caller tells the user. */
    fun markAsked(pkg: String, cap: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("p|$pkg|$cap", 0) < 24 * 3600_000L) return false
        prefs.edit().putLong("p|$pkg|$cap", now).apply()
        return true
    }

    /** The headers the user set for the app's requests to `host`; never logged, never sent to the watch. */
    fun headers(pkg: String, host: String): List<Pair<String, String>> = secure["h|$pkg|$host"]?.let { s ->
        runCatching { JSONArray(s).let { a -> (0 until a.length()).map { a.getJSONArray(it).let { p -> p.getString(0) to p.getString(1) } } } }.getOrNull()
    } ?: emptyList()

    fun setHeaders(pkg: String, host: String, list: List<Pair<String, String>>) {
        secure["h|$pkg|$host"] = if (list.isEmpty()) null else JSONArray(list.map { JSONArray(listOf(it.first, it.second)) }).toString()
    }

    fun showText(pkg: String) = prefs.getBoolean("show|$pkg", false)
    fun setShowText(pkg: String, on: Boolean) = prefs.edit().putBoolean("show|$pkg", on).apply()
}

/** The ask notification's title: "‹package› wants ‹cap›". */
fun askTitle(context: Context, pkg: String, cap: String, write: Boolean) = when {
    write -> context.getString(R.string.ask_http_write, pkg, cap.removePrefix("http:"))
    cap == "calendar" -> context.getString(R.string.ask_calendar, pkg)
    cap == "location" -> context.getString(R.string.ask_location, pkg)
    cap == "weather" -> context.getString(R.string.ask_weather, pkg)
    cap == "notifications" -> context.getString(R.string.ask_notifications, pkg)
    cap == "notify" -> context.getString(R.string.ask_notify, pkg)
    cap == "media" -> context.getString(R.string.ask_media, pkg)
    else -> context.getString(R.string.ask_http, pkg, cap.removePrefix("http:"))
}

/** The Android permission a cap needs; notifications and http have none to ask with a dialog. */
fun osPermission(cap: String) = when (cap) {
    "calendar" -> Manifest.permission.READ_CALENDAR
    "location", "weather" -> Manifest.permission.ACCESS_COARSE_LOCATION
    "notify" -> Manifest.permission.POST_NOTIFICATIONS
    else -> null
}

fun osAllows(context: Context, cap: String): Boolean = when {
    // media sessions of other apps need the notification listener too
    cap == "notifications" || cap == "media" -> context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    cap == "notify" -> NotificationManagerCompat.from(context).areNotificationsEnabled()
    else -> osPermission(cap)?.let { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED } ?: true
}

fun channel(context: Context, id: String, name: String, importance: Int) =
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(id, name, importance))

/** Opens the asking app's page, or with no package the Apps list. A request code per package keeps their extras apart. */
fun appsIntent(context: Context, pkg: String? = null): PendingIntent = PendingIntent.getActivity(
    context, pkg?.hashCode() ?: 1,
    Intent(context, MainActivity::class.java).putExtra("apps", true).putExtra("app", pkg).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

/** "‹package› wants ‹cap›"; nothing without POST_NOTIFICATIONS (its page under Apps still shows it). `write`:
 *  http.request, "wants to read and change data on ‹host›". */
fun notifyAsk(context: Context, pkg: String, cap: String, write: Boolean = false) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    channel(context, "ask", context.getString(R.string.channel_ask), NotificationManager.IMPORTANCE_DEFAULT)
    val n = NotificationCompat.Builder(context, "ask")
        .setSmallIcon(R.drawable.ic_watch)
        .setContentTitle(askTitle(context, pkg, cap, write))
        .setContentText(context.getString(R.string.ask_text))
        .setContentIntent(appsIntent(context, pkg))
        .setAutoCancel(true)
        .build()
    context.getSystemService(NotificationManager::class.java).notify("$pkg|$cap".hashCode(), n)
}

/** A cookie jar per (package, host) for the watch apps' requests, kept across them.
 *  ponytail: in memory, gone when the service dies; a file (encrypted) if apps need a login to outlive that. */
object Cookies {
    private val jars = ConcurrentHashMap<String, Jar>()

    fun of(pkg: String, host: String): CookieJar = jars.getOrPut("$pkg|$host") { Jar() }
    fun clear(pkg: String) = jars.keys.removeAll { it.startsWith("$pkg|") }

    private class Jar : CookieJar {
        private val list = mutableListOf<Cookie>()

        @Synchronized override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            for (c in cookies) {
                list.removeAll { it.name == c.name && it.domain == c.domain && it.path == c.path }
                list += c
            }
        }

        @Synchronized override fun loadForRequest(url: HttpUrl): List<Cookie> {
            list.removeAll { it.expiresAt < System.currentTimeMillis() }
            return list.filter { it.matches(url) }
        }
    }
}

/**
 * A watch app's notify (0x51): its own title and text, opening the app's page. Channels fix the sound, so three:
 * with the default sound, quiet, and "find my phone" on the alarm stream, repeating until it is seen
 * (FLAG_INSISTENT). DND still wins unless the user lets the channel through.
 */
fun postAlert(context: Context, pkg: String, a: tools.fmc.companion.wire.PhoneAlert) {
    val nm = context.getSystemService(NotificationManager::class.java)
    val id = when {
        a.find -> "find-phone".also {
            nm.createNotificationChannel(NotificationChannel(it, context.getString(R.string.find_phone), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                    android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                enableVibration(true)
            })
        }
        a.sound -> "watch-apps".also { channel(context, it, context.getString(R.string.channel_watch_apps), NotificationManager.IMPORTANCE_DEFAULT) }
        else -> "watch-apps-quiet".also { channel(context, it, context.getString(R.string.channel_watch_apps_silent), NotificationManager.IMPORTANCE_LOW) }
    }
    val app = StoreApps.listing(pkg)?.name ?: pkg
    val n = NotificationCompat.Builder(context, id)
        .setSmallIcon(R.drawable.ic_watch)
        .setContentTitle(if (a.find) context.getString(R.string.find_phone) else a.title.ifEmpty { app })
        .setContentText(if (a.find && a.body.isEmpty()) context.getString(R.string.find_phone_text, app) else a.body)
        .setContentIntent(appsIntent(context, pkg))
        .setAutoCancel(true)
        .apply { if (a.find) setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX) }
        .build()
    if (a.find) n.flags = n.flags or android.app.Notification.FLAG_INSISTENT
    nm.notify("notify|$pkg|${System.nanoTime()}".hashCode(), n)
}

/** Watch apps' background jobs (job.set), per package: prefs "jobs", "pkg" = JSON array. Not secret: an http job's
 *  URL and body are what the app sent; the headers stay in SecureStore. */
class Jobs(context: Context) {
    private val prefs = context.getSharedPreferences("jobs", Context.MODE_PRIVATE)

    fun of(pkg: String): List<RefreshJob> = runCatching {
        val a = JSONArray(prefs.getString(pkg, "[]"))
        (0 until a.length()).map(a::getJSONObject).map {
            RefreshJob(it.getInt("id"), it.getInt("minutes"), it.getInt("kind"), tools.fmc.companion.wire.hex(it.getString("spec")),
                it.optBoolean("on", true), if (it.has("last")) it.getLong("last") else null, it.optInt("target", 1))
        }
    }.getOrDefault(emptyList())

    fun all(): Map<String, List<RefreshJob>> = prefs.all.keys.associateWith(::of)

    private fun put(pkg: String, list: List<RefreshJob>) = prefs.edit().apply {
        if (list.isEmpty()) remove(pkg) else putString(pkg, JSONArray(list.map { j ->
            org.json.JSONObject().put("id", j.id).put("minutes", j.minutes).put("kind", j.kind).put("spec", j.spec.joinToString("") { "%02x".format(it) })
                .put("on", j.on).put("target", j.target).apply { j.last?.let { put("last", it) } }
        }).toString())
    }.apply()

    /** A new job with that id replaces the old one and runs soon; the user's switch stays as it was. */
    @Synchronized fun save(pkg: String, job: RefreshJob) {
        val old = of(pkg)
        put(pkg, old.filter { it.id != job.id } + job.copy(on = old.firstOrNull { it.id == job.id }?.on ?: true, last = null))
    }

    @Synchronized fun delete(pkg: String, id: Int) = put(pkg, of(pkg).filter { it.id != id })
    @Synchronized fun setOn(pkg: String, id: Int, on: Boolean) = put(pkg, of(pkg).map { if (it.id == id) it.copy(on = on) else it })
    @Synchronized fun ran(pkg: String, id: Int, at: Long) = put(pkg, of(pkg).map { if (it.id == id) it.copy(last = at) else it })
    fun clear(pkg: String) = prefs.edit().remove(pkg).apply()
}

/** "Every 30 min: GET api.example.com/rate" */
fun jobLabel(j: RefreshJob): String {
    val what = when (j.kind) {
        tools.fmc.companion.wire.JobKind.HTTP -> tools.fmc.companion.wire.parseHttpCall(j.spec)
            ?.let { "${it.method} ${it.url.removePrefix("https://")}" } ?: str(R.string.job_web_request)
        tools.fmc.companion.wire.JobKind.WEATHER -> str(R.string.cap_weather)
        else -> str(R.string.cap_calendar)
    }
    val into = if (j.target == tools.fmc.companion.wire.PushKind.WIDGET) str(R.string.job_into_widget, what) else what
    return if (j.minutes % 60 == 0) str(R.string.job_every_h, j.minutes / 60, into) else str(R.string.job_every_min, j.minutes, into)
}
