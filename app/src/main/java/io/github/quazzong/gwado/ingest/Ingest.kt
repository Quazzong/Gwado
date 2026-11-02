package io.github.quazzong.gwado.ingest

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.sumber.HasilSumber
import io.github.quazzong.gwado.sumber.Kelas
import io.github.quazzong.gwado.sumber.Tugas
import java.security.MessageDigest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

// Terjemahan serper/ingest.py (proses). Satu Isi = satu transaksi. Hanya menulis courses,
// tasks (asal='sumber'), task_changes (oleh='ingest'), dan source_runs. Tugas manual dan data
// pengguna (task_user_state, preferensi) tidak pernah disentuh. Dibuktikan golden ingest.

/** Run sukses penuh berturut-turut tanpa tugas itu sebelum ditandai hilang. */
const val AMBANG_HILANG = 2

/** Hasil fetch satu sumber satu run (format inbox SERPER tanpa file). error != null = run gagal. */
data class Isi(
    val fetchId: String,
    val sumber: String,
    val mulai: String,
    val selesai: String,
    val error: ErrorFetch? = null,
    val hasil: HasilSumber = HasilSumber(emptyList(), emptyList()),
)

data class ErrorFetch(val jenis: String, val pesan: String)

/** status: success | partial | failed | dilewati. */
data class HasilIngest(val status: String, val pesan: String)

/** Kondisi yang seharusnya tidak mungkin terjadi; transaksi dibatalkan. */
class IngestError(pesan: String) : RuntimeException(pesan)

/** ID stabil tugas sumber: 's-' + 16 hex pertama sha256("sumber\x1fexternal_id"). */
fun idTugas(sumber: String, externalId: String): String =
    "s-" + sha256Hex("$sumber\u001f$externalId").take(16)

/** Field yang ikut content_hash (FIELD_HASH di ingest.py). */
fun fieldHash(t: Tugas): Map<String, String?> = mapOf(
    "course_external_id" to t.courseExternalId,
    "jenis" to t.jenis,
    "link_id" to t.linkId,
    "judul" to t.judul,
    "deskripsi" to t.deskripsi,
    "deadline" to t.deadline,
    "url" to t.url,
)

/** sha256 dari json.dumps(field, ensure_ascii=False, sort_keys=True) versi Python. */
fun contentHash(t: Tugas): String =
    sha256Hex(fieldHash(t).toSortedMap().entries.joinToString(", ", "{", "}") { (k, v) ->
        "${jsonTeks(k)}: ${jsonTeks(v)}"
    })

/** Aturan semester aktif (§5.1 konteks): (1 = diikuti / 0 = dilewati, alasan). */
fun diikuti(sumber: String, k: Kelas, aturan: Map<String, String>): Pair<Int, String> = when (sumber) {
    "brone" -> {
        val label = aturan.getValue("semester_label")
        if (k.kategori == label) 1 to "kategori $label"
        else 0 to "kategori ${k.kategori?.ifEmpty { null } ?: "-"}, bukan $label"
    }
    "eling" -> {
        val kode = aturan.getValue("semester_kode")
        if (kode in (k.shortname ?: "")) 1 to "shortname memuat $kode"
        else 0 to "shortname tidak memuat $kode"
    }
    "classroom" -> {
        val batas = aturan.getValue("semester_mulai")
        val dibuat = (k.dibuat ?: "").take(10)
        if (dibuat.isNotEmpty() && dibuat >= batas) 1 to "dibuat $dibuat, sejak $batas"
        else 0 to "dibuat ${dibuat.ifEmpty { "-" }}, sebelum $batas"
    }
    else -> throw IngestError("sumber tidak dikenal: $sumber")
}

private val KUNCI_SEMESTER = listOf("semester_label", "semester_kode", "semester_mulai")

fun Db.aturanSemester(): Map<String, String> {
    val a = semua("SELECT kunci, nilai FROM preferences WHERE kunci IN (?, ?, ?)", *KUNCI_SEMESTER.toTypedArray())
        .associate { it["kunci"] as String to it["nilai"] as String }
    val kurang = KUNCI_SEMESTER.filter { it !in a }
    if (kurang.isNotEmpty()) throw IngestError("preferensi semester belum diisi: ${kurang.joinToString(", ")}")
    return a
}

/** Masukkan satu Isi. Kembalikan ringkasan (status, pesan) yang sama dengan ingest.proses(). */
fun Db.proses(isi: Isi): HasilIngest = transaksi {
    val sumber = isi.sumber
    val saat = isi.selesai

    if (satu("SELECT 1 FROM source_runs WHERE fetch_id = ?", isi.fetchId) != null) {
        return@transaksi HasilIngest("dilewati", "sudah pernah di-ingest")
    }
    val terakhir = satu("SELECT max(mulai) AS m FROM source_runs WHERE source_id = ?", sumber)!!["m"] as String?
    if (terakhir != null && isi.mulai < terakhir) {
        return@transaksi HasilIngest("dilewati", "lebih lama dari run terakhir ($terakhir)")
    }

    val e = isi.error
    if (e != null) {
        catatRun(isi, "failed", null, "${e.jenis}: ${e.pesan}")
        return@transaksi HasilIngest("failed", e.jenis)
    }

    val aturan = aturanSemester()
    val nilai = isi.hasil.kelas.map { k -> diikuti(sumber, k, aturan).let { (ikut, alasan) -> Triple(k, ikut, alasan) } }
    val ikutEid = nilai.filter { it.second == 1 }.map { it.first.externalId }.toSet()

    val alasanPartial = cekPartial(isi, ikutEid)
    if (alasanPartial != null) {
        catatRun(isi, "partial", null, alasanPartial)
        return@transaksi HasilIngest("partial", alasanPartial)
    }

    // Mata kuliah: semua disimpan, termasuk yang dilewati. kode_mk manual tidak ditimpa kosong.
    val idCourse = mutableMapOf<String, Long>()
    for ((k, ikut, alasan) in nilai) {
        idCourse[k.externalId] = satu(
            "INSERT INTO courses (source_id, external_id, nama, shortname, kode_mk, kategori, " +
                "mulai, selesai, dibuat, diikuti, alasan_diikuti) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (source_id, external_id) DO UPDATE SET " +
                "nama = excluded.nama, shortname = excluded.shortname, " +
                "kode_mk = coalesce(excluded.kode_mk, courses.kode_mk), kategori = excluded.kategori, " +
                "mulai = excluded.mulai, selesai = excluded.selesai, dibuat = excluded.dibuat, " +
                "diikuti = excluded.diikuti, alasan_diikuti = excluded.alasan_diikuti " +
                "RETURNING id",
            sumber, k.externalId, k.nama, k.shortname, k.kodeMk, k.kategori,
            k.mulai, k.selesai, k.dibuat, ikut, alasan,
        )!!["id"] as Long
    }

    // Tugas dari mata kuliah yang diikuti (dengan atau tanpa deadline)
    val terlihat = linkedSetOf<String>()
    var baru = 0
    var berubah = 0
    var kembali = 0
    for (t in isi.hasil.tugas) {
        if (t.courseExternalId !in ikutEid) continue
        val tid = idTugas(sumber, t.externalId)
        terlihat += tid
        val h = contentHash(t)
        val courseId = idCourse.getValue(t.courseExternalId)
        val deskripsi = t.deskripsi.ifEmpty { null }
        val lama = satu(
            "SELECT asal, judul, deadline, deskripsi, content_hash, status_situs FROM tasks WHERE id = ?", tid,
        )
        if (lama == null) {
            exec(
                "INSERT INTO tasks (id, source_id, external_id, course_id, jenis, link_id, judul, " +
                    "deskripsi, deadline, url, status_kumpul_situs, first_seen, last_seen, " +
                    "content_hash, asal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'sumber')",
                tid, sumber, t.externalId, courseId, t.jenis, t.linkId, t.judul,
                deskripsi, t.deadline, t.url, t.statusKumpulSitus, saat, saat, h,
            )
            ubah(tid, "dibuat", null, t.judul, saat)
            baru++
            continue
        }
        if (lama["asal"] != "sumber") throw IngestError("ID $tid bentrok dengan tugas ${lama["asal"]}")
        if (lama["judul"] != t.judul) ubah(tid, "judul", lama["judul"] as String?, t.judul, saat)
        if (lama["deadline"] != t.deadline) ubah(tid, "deadline", lama["deadline"] as String?, t.deadline, saat)
        if ((lama["deskripsi"] as String?)?.ifEmpty { null } != deskripsi) ubah(tid, "deskripsi", null, null, saat)
        if (lama["status_situs"] == "hilang") {
            ubah(tid, "status_situs", "hilang", "aktif", saat)
            kembali++
        }
        if (lama["content_hash"] != h) berubah++
        exec(
            "UPDATE tasks SET course_id = ?, jenis = ?, link_id = ?, judul = ?, deskripsi = ?, " +
                "deadline = ?, url = ?, status_kumpul_situs = ?, last_seen = ?, content_hash = ?, " +
                "status_situs = 'aktif', jumlah_absen = 0 WHERE id = ? AND asal = 'sumber'",
            courseId, t.jenis, t.linkId, t.judul, deskripsi,
            t.deadline, t.url, t.statusKumpulSitus, saat, h, tid,
        )
    }

    // Absen: hanya tugas aktif dari mata kuliah yang MASIH diikuti
    var hilang = 0
    val aktif = semua(
        "SELECT t.id, t.jumlah_absen FROM tasks t JOIN courses c ON c.id = t.course_id " +
            "WHERE t.source_id = ? AND t.asal = 'sumber' AND t.status_situs = 'aktif' AND c.diikuti = 1",
        sumber,
    )
    for (r in aktif) {
        val tid = r["id"] as String
        if (tid in terlihat) continue
        val absen = (r["jumlah_absen"] as Long) + 1
        if (absen >= AMBANG_HILANG) {
            exec("UPDATE tasks SET status_situs = 'hilang', jumlah_absen = ? WHERE id = ?", absen, tid)
            ubah(tid, "status_situs", "aktif", "hilang", saat)
            hilang++
        } else {
            exec("UPDATE tasks SET jumlah_absen = ? WHERE id = ?", absen, tid)
        }
    }

    catatRun(isi, "success", terlihat.size, null)
    HasilIngest(
        "success",
        "${ikutEid.size}/${nilai.size} matkul diikuti, ${terlihat.size} tugas: " +
            "$baru baru, $berubah berubah, $kembali muncul lagi, $hilang hilang",
    )
}

/** Alasan partial, atau null. Dicek SEBELUM menulis apa pun. */
private fun Db.cekPartial(isi: Isi, ikutEid: Set<String>): String? {
    if (isi.hasil.kelas.isEmpty()) {
        val ada = satu("SELECT 1 FROM courses WHERE source_id = ? LIMIT 1", isi.sumber) != null
        return if (ada) "0 mata kuliah dari situs, padahal sebelumnya ada" else null
    }
    if (ikutEid.isEmpty()) {
        return "0 mata kuliah lolos filter semester (cek preferensi semester atau nonaktifkan sumber)"
    }
    if (isi.hasil.tugas.isEmpty()) {
        val daftar = JsonArray(ikutEid.sorted().map { JsonPrimitive(it) }).toString()
        val aktif = satu(
            "SELECT 1 FROM tasks t JOIN courses c ON c.id = t.course_id " +
                "WHERE t.source_id = ? AND t.asal = 'sumber' AND t.status_situs = 'aktif' " +
                "AND c.external_id IN (SELECT value FROM json_each(?)) LIMIT 1",
            isi.sumber, daftar,
        ) != null
        return if (aktif) "0 tugas dari situs, padahal ada tugas aktif" else null
    }
    return null
}

private fun Db.catatRun(isi: Isi, status: String, jumlah: Int?, pesan: String?) = exec(
    "INSERT INTO source_runs (source_id, fetch_id, mulai, selesai, status, jumlah_tugas, error_singkat) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)",
    isi.sumber, isi.fetchId, isi.mulai, isi.selesai, status, jumlah, pesan,
)

private fun Db.ubah(taskId: String, field: String, lama: String?, baru: String?, waktu: String) = exec(
    "INSERT INTO task_changes (task_id, field, nilai_lama, nilai_baru, waktu, oleh) " +
        "VALUES (?, ?, ?, ?, ?, 'ingest')",
    taskId, field, lama, baru, waktu,
)

private fun sha256Hex(teks: String): String =
    MessageDigest.getInstance("SHA-256").digest(teks.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

/** String JSON seperti json.dumps Python dengan ensure_ascii=False. */
private fun jsonTeks(s: String?): String {
    if (s == null) return "null"
    val b = StringBuilder("\"")
    for (c in s) {
        when (c) {
            '"' -> b.append("\\\"")
            '\\' -> b.append("\\\\")
            '\n' -> b.append("\\n")
            '\r' -> b.append("\\r")
            '\t' -> b.append("\\t")
            '\b' -> b.append("\\b")
            '\u000C' -> b.append("\\f")
            else -> if (c < ' ') b.append("\\u%04x".format(c.code)) else b.append(c)
        }
    }
    return b.append('"').toString()
}
