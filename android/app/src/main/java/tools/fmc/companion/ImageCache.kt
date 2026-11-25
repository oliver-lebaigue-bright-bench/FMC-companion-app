package tools.fmc.companion

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pictures from the web on disk, under cacheDir/images: a file per URL, the least recently used ones dropped past
 * [MAX] bytes. fmc.tools sends previews with max-age 30 days and a file name that changes with the file, so a
 * picture once downloaded is good for as long as it stays here. iOS: URLCache.shared sized at launch.
 */
object ImageCache {
    const val MAX = 150L shl 20
    private var dir: File? = null
    /** Bytes on disk as last added up; null until the first put. */
    private var total: Long? = null

    fun init(cacheDir: File) {
        if (dir == null) dir = File(cacheDir, "images")
    }

    private val hex = "0123456789abcdef".toCharArray()

    private fun file(url: String) = dir?.let { d ->
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        val name = StringBuilder(digest.size * 2)
        for (b in digest) {
            val v = b.toInt() and 0xff
            name.append(hex[v ushr 4]).append(hex[v and 0x0f])
        }
        File(d, name.toString())
    }

    /** The bytes saved for [url], else downloaded and saved. */
    suspend fun bytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val f = file(url)
        if (f != null && f.isFile) runCatching { f.readBytes() }.getOrNull()?.let { return@withContext it }
        download(url).also { raw -> if (f != null) put(f, raw) }
    }

    /** Keeps a running byte total: the directory is listed once, then only when eviction is actually needed. */
    @Synchronized private fun put(f: File, raw: ByteArray) {
        runCatching {
            f.parentFile?.mkdirs()
            val tmp = File(f.path + ".tmp")
            tmp.writeBytes(raw)
            tmp.renameTo(f)
        }
        if (total == null) total = f.parentFile?.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0
        val sum = total!! + raw.size
        total = sum
        if (sum <= MAX) return
        val files = f.parentFile?.listFiles()?.filter { it.isFile } ?: return
        val entries = files.map { it.length() to it.lastModified() }
        evict(entries, MAX).forEach { files[it].delete() }
        total = files.filter { it.exists() }.sumOf { it.length() }
    }

    /** Which of (size, last used) to drop, oldest first, to be back under [max] with room to spare (90%). */
    fun evict(entries: List<Pair<Long, Long>>, max: Long): List<Int> {
        var sum = entries.sumOf { it.first }
        if (sum <= max) return emptyList()
        val out = mutableListOf<Int>()
        for (i in entries.indices.sortedBy { entries[it].second }) {
            if (sum <= max * 9 / 10) break
            out += i
            sum -= entries[i].first
        }
        return out
    }
}
