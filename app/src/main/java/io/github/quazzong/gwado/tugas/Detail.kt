package io.github.quazzong.gwado.tugas

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.waktu.dariDb
import io.github.quazzong.gwado.waktu.formatTanggal
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Satu tugas lengkap untuk layar detail. */
data class DetailTugas(
    val id: String,
    val jenis: String,
    val judul: String,
    val deskripsi: String?,
    val deadline: String?,
    val url: String?,
    val asal: String,
    val statusSitus: String,
    val statusKumpulSitus: String?,
    val firstSeen: String?,
    val kodeMkManual: String?,
    val matkul: String?,
    val kodeMk: String?,
    val sumber: String?,
    val status: String,
    val doneAt: String?,
    val catatan: String?,
)

/** Satu baris riwayat siap tampil. dari/ke null hanya untuk baris "pertama diambil". */
data class BarisRiwayat(
    val waktu: String,
    val label: String,
    val dari: String?,
    val ke: String?,
    val oleh: String,
    val kuning: Boolean,
)

val LABEL_UBAH = mapOf(
    "deadline" to "deadline berubah",
    "judul" to "judul berubah",
    "status" to "status diubah",
    "status_situs" to "status situs berubah",
    "deskripsi" to "catatan diubah",
    "kode_mk_manual" to "matkul diubah",
)

val LABEL_STATUS = mapOf(
    "todo" to "belum selesai",
    "doing" to "sedang dikerjakan",
    "done" to "selesai",
)

private const val SQL_SATU = """
SELECT t.id, t.jenis, t.judul, t.deskripsi, t.deadline, t.url, t.asal, t.status_situs,
       t.status_kumpul_situs, t.first_seen, t.kode_mk_manual,
       $SQL_MATKUL AS matkul, c.kode_mk, s.nama AS sumber,
       coalesce(u.status, 'todo') AS status, u.done_at, u.catatan
FROM tasks t
LEFT JOIN courses c ON c.id = t.course_id
LEFT JOIN sources s ON s.id = t.source_id
LEFT JOIN task_user_state u ON u.task_id = t.id
WHERE t.id = ?
"""

/** Waktu ke teks database (UTC, detik penuh). */
private fun keDb(waktu: Instant): String = waktu.truncatedTo(ChronoUnit.SECONDS).toString()

/** Satu tugas, atau null kalau ID-nya tidak ada. */
fun Db.ambilTugas(id: String): DetailTugas? = satu(SQL_SATU, id)?.let {
    DetailTugas(
        id = it["id"] as String,
        jenis = it["jenis"] as String,
        judul = it["judul"] as String,
        deskripsi = it["deskripsi"] as String?,
        deadline = it["deadline"] as String?,
        url = it["url"] as String?,
        asal = it["asal"] as String,
        statusSitus = it["status_situs"] as String,
        statusKumpulSitus = it["status_kumpul_situs"] as String?,
        firstSeen = it["first_seen"] as String?,
        kodeMkManual = it["kode_mk_manual"] as String?,
        matkul = it["matkul"] as String?,
        kodeMk = it["kode_mk"] as String?,
        sumber = it["sumber"] as String?,
        status = it["status"] as String,
        doneAt = it["done_at"] as String?,
        catatan = it["catatan"] as String?,
    )
}

/** Satu sisi perubahan untuk ditampilkan. namaMk = kode MK ke nama, untuk tugas manual. */
fun nilaiUbah(field: String, v: String?, namaMk: Map<String, String>? = null): String = when {
    v.isNullOrEmpty() -> "—"
    field == "kode_mk_manual" -> namaMk?.get(v) ?: v
    field == "deskripsi" ->
        if (v.codePointCount(0, v.length) <= 60) v
        else v.substring(0, v.offsetByCodePoints(0, 60)).trimEnd() + "…"
    field == "status" -> LABEL_STATUS[v] ?: v
    field == "deadline" -> formatTanggal(dariDb(v))
    else -> v
}

/**
 * Riwayat satu tugas, terbaru di atas (waktu sama: urutan id). Tugas dari situs diakhiri
 * baris "pertama diambil". Null kalau ID-nya tidak ada.
 */
fun Db.riwayatTampil(id: String, namaMk: Map<String, String>? = null): List<BarisRiwayat>? {
    val t = ambilTugas(id) ?: return null
    val baris = semua(
        "SELECT field, nilai_lama, nilai_baru, waktu, oleh FROM task_changes " +
            "WHERE task_id = ? ORDER BY waktu, id",
        id,
    ).sortedByDescending { it["waktu"] as String }.map { r ->
        val field = r["field"] as String
        BarisRiwayat(
            waktu = formatTanggal(dariDb(r["waktu"] as String)),
            label = LABEL_UBAH[field] ?: field,
            dari = nilaiUbah(field, r["nilai_lama"] as String?, namaMk),
            ke = nilaiUbah(field, r["nilai_baru"] as String?, namaMk),
            oleh = if (r["oleh"] == "ingest") "situs" else r["oleh"] as String,
            kuning = field == "deadline",
        )
    }
    val pertama = t.firstSeen
    if (t.asal != "sumber" || pertama.isNullOrEmpty()) return baris
    return baris + BarisRiwayat(formatTanggal(dariDb(pertama)), "pertama diambil", null, null, "situs", false)
}

/**
 * Centang (selesai = true) atau batal centang, dicatat di riwayat. False kalau statusnya
 * memang sudah begitu. Catatan pengguna tidak disentuh.
 */
fun Db.tandai(id: String, selesai: Boolean, sekarang: Instant): Boolean {
    var berubah = false
    transaksi {
        val lama = ambilTugas(id)?.status ?: throw IllegalArgumentException("tugas tidak ditemukan")
        if ((lama == "done") == selesai) return@transaksi
        val baru = if (selesai) "done" else "todo"
        val w = keDb(sekarang)
        exec(
            "INSERT INTO task_user_state (task_id, status, done_at) VALUES (?, ?, ?) " +
                "ON CONFLICT (task_id) DO UPDATE SET status = excluded.status, done_at = excluded.done_at",
            id, baru, if (selesai) w else null,
        )
        exec(
            "INSERT INTO task_changes (task_id, field, nilai_lama, nilai_baru, waktu, oleh) " +
                "VALUES (?, 'status', ?, ?, ?, 'aku')",
            id, lama, baru, w,
        )
        berubah = true
    }
    return berubah
}

private val SKEMA = Regex("[A-Za-z][A-Za-z0-9+.-]*")

/**
 * Skema (huruf kecil) dan host (huruf kecil, null kalau kosong) dibaca seperti
 * urllib.parse.urlsplit di Python. Null kalau kurung siku IPv6 tidak berpasangan.
 */
internal fun pecahUrl(url: String): Pair<String, String?>? {
    var sisa = url.trimStart { it <= ' ' }.filterNot { it == '\t' || it == '\r' || it == '\n' }
    var skema = ""
    val titikDua = sisa.indexOf(':')
    if (titikDua > 0 && SKEMA.matches(sisa.substring(0, titikDua))) {
        skema = sisa.substring(0, titikDua).lowercase()
        sisa = sisa.substring(titikDua + 1)
    }
    var netloc = ""
    if (sisa.startsWith("//")) {
        val akhir = sisa.indexOfAny(charArrayOf('/', '?', '#'), 2).let { if (it < 0) sisa.length else it }
        netloc = sisa.substring(2, akhir)
    }
    if (('[' in netloc) != (']' in netloc)) return null
    val hostPort = netloc.substringAfterLast('@')
    val host = if ('[' in hostPort) hostPort.substringAfter('[').substringBefore(']')
    else hostPort.substringBefore(':')
    return skema to host.lowercase().ifEmpty { null }
}

/** url kalau https ke salah satu domain sumber yang dikenal; selain itu null. */
fun linkSumber(url: String?, domain: Collection<String>): String? {
    if (url == null) return null
    val (skema, host) = pecahUrl(url) ?: return null
    return if (skema == "https" && host != null && host in domain) url else null
}
