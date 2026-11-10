package io.github.quazzong.gwado.sumber

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

// ---------- Waktu ----------

internal val FORMAT_ISO: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

/** Detik Unix → "2026-10-20T05:00:00Z". Moodle memakai 0 untuk "tidak ada" → null. */
fun isoDariUnix(t: Long?): String? =
    if (t == null || t == 0L) null else FORMAT_ISO.format(Instant.ofEpochSecond(t))

// ---------- Membaca JSON ----------

internal fun JsonObject.obj(k: String): JsonObject? = this[k] as? JsonObject

internal fun JsonObject.daftar(k: String): List<JsonObject> =
    (this[k] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

/** Nilai teks, atau null kalau tidak ada / bukan teks. */
internal fun JsonObject.teks(k: String): String? =
    (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull && it.isString }?.content

/** Nilai angka bulat, atau null kalau tidak ada. */
internal fun JsonObject.angka(k: String): Long? =
    (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.longOrNull

/** Seperti str(x) di Python untuk ID: angka 101 → "101", teks tetap teks. Wajib ada. */
internal fun JsonObject.idTeks(k: String): String {
    val p = this[k] as? JsonPrimitive
    require(p != null && p !is JsonNull) { "kunci $k tidak ada" }
    return p.content
}

internal fun JsonObject.wajibTeks(k: String): String =
    requireNotNull(teks(k)) { "kunci $k tidak ada atau bukan teks" }

// ---------- Teks ala Python ----------

// Karakter yang dianggap spasi oleh str.split() Python (termasuk &nbsp; = \u00A0)
private fun spasiPython(c: Char): Boolean =
    c in "\t\n\u000B\u000C\r\u001C\u001D\u001E\u001F \u0085\u00A0\u1680\u2028\u2029\u202F\u205F\u3000" ||
        c in '\u2000'..'\u200A'

/** Sama dengan " ".join(s.split()) di Python. */
fun rapikanSpasi(s: String): String {
    val sb = StringBuilder()
    var butuhSpasi = false
    for (c in s) {
        if (spasiPython(c)) {
            if (sb.isNotEmpty()) butuhSpasi = true
        } else {
            if (butuhSpasi) sb.append(' ')
            butuhSpasi = false
            sb.append(c)
        }
    }
    return sb.toString()
}

private const val PEMISAH_BARIS = "\n\r\u000B\u000C\u001C\u001D\u001E\u0085\u2028\u2029"

/** Sama dengan s.splitlines() di Python (lebih luas dari sekadar \n). */
fun pisahBaris(s: String): List<String> {
    val hasil = mutableListOf<String>()
    var awal = 0
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c in PEMISAH_BARIS) {
            hasil.add(s.substring(awal, i))
            if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') i++
            awal = i + 1
        }
        i++
    }
    if (awal < s.length) hasil.add(s.substring(awal))
    return hasil
}

// ---------- HTML → teks polos (meniru HTMLParser Python yang dipakai pembuat golden) ----------

private val BLOK = setOf("p", "br", "div", "li", "tr", "h1", "h2", "h3", "h4", "h5", "h6")
private val LEWATI = setOf("script", "style")

// Sebagian entitas HTML yang umum. Kalau golden gagal karena entitas lain, tambahkan di sini
private val ENTITAS = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
    "nbsp" to "\u00A0", "ndash" to "\u2013", "mdash" to "\u2014", "hellip" to "\u2026",
    "lsquo" to "\u2018", "rsquo" to "\u2019", "ldquo" to "\u201C", "rdquo" to "\u201D",
    "bull" to "\u2022", "middot" to "\u00B7", "times" to "\u00D7", "deg" to "\u00B0",
    "copy" to "\u00A9", "reg" to "\u00AE", "laquo" to "\u00AB", "raquo" to "\u00BB",
)
private val POLA_ENTITAS = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")

private fun kodeKeTeks(kode: Int?): String? =
    if (kode == null || kode <= 0 || kode > 0x10FFFF || kode in 0xD800..0xDFFF) null
    else String(Character.toChars(kode))

private fun uraiEntitas(s: String): String = POLA_ENTITAS.replace(s) { m ->
    val isi = m.groupValues[1]
    when {
        isi.startsWith("#x") || isi.startsWith("#X") -> kodeKeTeks(isi.substring(2).toIntOrNull(16))
        isi.startsWith("#") -> kodeKeTeks(isi.substring(1).toIntOrNull())
        else -> ENTITAS[isi]
    } ?: m.value
}

/** Posisi '>' penutup tag, melewati isi atribut dalam tanda kutip. -1 kalau tidak ada. */
private fun akhirTag(html: String, dari: Int): Int {
    var kutip: Char? = null
    for (i in dari until html.length) {
        val c = html[i]
        if (kutip != null) {
            if (c == kutip) kutip = null
        } else if (c == '"' || c == '\'') {
            kutip = c
        } else if (c == '>') {
            return i
        }
    }
    return -1
}

/** HTML deskripsi → teks polos, satu paragraf per baris. */
fun teksPolos(html: String?): String {
    if (html.isNullOrEmpty()) return ""
    val sb = StringBuilder()
    val n = html.length
    var i = 0
    while (i < n) {
        if (html[i] != '<') {
            val j = html.indexOf('<', i).let { if (it < 0) n else it }
            sb.append(uraiEntitas(html.substring(i, j)))
            i = j
            continue
        }
        if (html.startsWith("<!--", i)) {                       // komentar
            val akhir = html.indexOf("-->", i + 4)
            i = if (akhir < 0) n else akhir + 3
            continue
        }
        if (i + 1 < n && (html[i + 1] == '!' || html[i + 1] == '?')) { // <!DOCTYPE>, <?xml?>
            val akhir = html.indexOf('>', i + 2)
            i = if (akhir < 0) n else akhir + 1
            continue
        }
        val tutup = i + 1 < n && html[i + 1] == '/'
        val awalNama = if (tutup) i + 2 else i + 1
        if (awalNama >= n || !html[awalNama].isLetter()) {       // "<" biasa, bukan tag
            sb.append('<')
            i++
            continue
        }
        var k = awalNama
        while (k < n && !html[k].isWhitespace() && html[k] != '/' && html[k] != '>') k++
        val nama = html.substring(awalNama, k).lowercase()
        val akhir = akhirTag(html, k)
        if (akhir < 0) break                                     // tag terpotong di akhir teks
        i = akhir + 1
        if (!tutup && nama in LEWATI) {                          // isi <script>/<style> dibuang
            val penutup = html.indexOf("</$nama", i, ignoreCase = true)
            i = if (penutup < 0) n else html.indexOf('>', penutup).let { if (it < 0) n else it + 1 }
            continue
        }
        if (nama in BLOK) sb.append('\n')
    }
    return pisahBaris(sb.toString()).map(::rapikanSpasi).filter { it.isNotEmpty() }.joinToString("\n")
}

/** Sama dengan s.strip() di Python (aturan spasinya beda dengan trim() bawaan Kotlin). */
fun potongSpasi(s: String): String = s.trim { spasiPython(it) }
