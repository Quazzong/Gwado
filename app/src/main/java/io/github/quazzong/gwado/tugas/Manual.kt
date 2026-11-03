package io.github.quazzong.gwado.tugas

import io.github.quazzong.gwado.db.Db
import java.security.SecureRandom
import java.time.Instant

/** Galat aksi tugas. kolom = isian yang salah ("judul" atau "kode_mk"), null kalau bukan isian tertentu. */
class TugasGalat(val kolom: String?, pesan: String) : IllegalArgumentException(pesan)

/** Kunci perubahan yang boleh dipakai [ubahManual]. */
val KOLOM_UBAH = listOf("judul", "deadline", "deskripsi", "kode_mk")

/** Tabel anak yang ikut dihapus bersama tugas manual. */
private val ANAK_TASKS = listOf("task_changes", "task_user_state")

private val acak = SecureRandom()

private fun idManual(): String =
    "m-" + ByteArray(4).also(acak::nextBytes).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

private fun rapikanJudul(judul: String?, kolom: String?): String {
    val j = judul.orEmpty().trim()
    if (j.isEmpty()) throw TugasGalat(kolom, "judul tidak boleh kosong")
    if (j.codePointCount(0, j.length) > 200) throw TugasGalat(kolom, "judul terlalu panjang (maks 200 karakter)")
    return j
}

private fun rapikanKode(kode: String?): String? = kode.orEmpty().trim().uppercase().ifEmpty { null }

private fun rapikanDeskripsi(deskripsi: String?): String? = deskripsi.orEmpty().trim().ifEmpty { null }

private fun galatKode(kode: String) =
    TugasGalat("kode_mk", "kode MK $kode tidak ada di jadwal kuliah atau mata kuliah yang diikuti")

/** Kode MK boleh dipakai tugas manual: ada di jadwal kuliah atau di mata kuliah situs yang diikuti. */
fun Db.kodeDikenal(kodeMk: String): Boolean = satu(
    "SELECT 1 AS ada FROM class_schedule WHERE kode_mk = ? " +
        "UNION ALL SELECT 1 FROM courses WHERE kode_mk = ? AND diikuti = 1 LIMIT 1",
    kodeMk, kodeMk,
) != null

/** Tambah tugas manual, dicatat di riwayat sebagai "dibuat". Kembalikan ID barunya. */
fun Db.tambahManual(
    judul: String?,
    deadline: Instant?,
    deskripsi: String?,
    sekarang: Instant,
    kodeMk: String? = null,
): String {
    val j = rapikanJudul(judul, null)
    val kode = rapikanKode(kodeMk)
    val id = idManual()
    val w = keDb(sekarang)
    transaksi {
        if (kode != null && !kodeDikenal(kode)) throw galatKode(kode)
        exec(
            "INSERT INTO tasks (id, jenis, judul, deskripsi, deadline, first_seen, asal, kode_mk_manual) " +
                "VALUES (?, 'manual', ?, ?, ?, ?, 'manual', ?)",
            id, j, rapikanDeskripsi(deskripsi), deadline?.let(::keDb), w, kode,
        )
        exec(
            "INSERT INTO task_changes (task_id, field, nilai_lama, nilai_baru, waktu, oleh) " +
                "VALUES (?, 'dibuat', NULL, ?, ?, 'aku')",
            id, j, w,
        )
    }
    return id
}

/**
 * Ubah tugas manual. perubahan = sebagian dari judul, deadline (Instant atau null untuk
 * menghapus), deskripsi, kode_mk. Hanya kolom yang benar-benar berubah yang ditulis dan
 * dicatat di riwayat. Kembalikan nama kolom yang berubah.
 */
fun Db.ubahManual(id: String, perubahan: Map<String, Any?>, sekarang: Instant): List<String> {
    val asing = perubahan.keys - KOLOM_UBAH.toSet()
    if (asing.isNotEmpty()) throw TugasGalat(null, "kolom tidak dikenal: ${asing.sorted().joinToString(", ")}")
    val baru = LinkedHashMap<String, String?>()
    if ("judul" in perubahan) baru["judul"] = rapikanJudul(perubahan["judul"] as String?, "judul")
    if ("deadline" in perubahan) baru["deadline"] = (perubahan["deadline"] as Instant?)?.let(::keDb)
    if ("deskripsi" in perubahan) baru["deskripsi"] = rapikanDeskripsi(perubahan["deskripsi"] as String?)
    if ("kode_mk" in perubahan) baru["kode_mk_manual"] = rapikanKode(perubahan["kode_mk"] as String?)
    val berubah = mutableListOf<String>()
    transaksi {
        val t = ambilTugas(id) ?: throw TugasGalat(null, "tugas tidak ditemukan")
        if (t.asal != "manual") throw TugasGalat(null, "hanya tugas manual yang bisa diubah")
        baru["kode_mk_manual"]?.let { if (!kodeDikenal(it)) throw galatKode(it) }
        val lama = mapOf(
            "judul" to t.judul,
            "deadline" to t.deadline,
            "deskripsi" to t.deskripsi,
            "kode_mk_manual" to t.kodeMkManual,
        )
        for ((kolom, nilai) in baru) {
            if (lama[kolom] == nilai) continue
            exec("UPDATE tasks SET $kolom = ? WHERE id = ?", nilai, id)   // kolom hanya dari daftar tetap di atas
            exec(
                "INSERT INTO task_changes (task_id, field, nilai_lama, nilai_baru, waktu, oleh) " +
                    "VALUES (?, ?, ?, ?, ?, 'aku')",
                id, kolom, lama[kolom], nilai, keDb(sekarang),
            )
            berubah += kolom
        }
    }
    return berubah
}

/** Hapus permanen tugas manual beserta riwayat dan status centangnya. Kembalikan judulnya. */
fun Db.hapusManual(id: String): String {
    var judul = ""
    transaksi {
        val t = ambilTugas(id) ?: throw TugasGalat(null, "tugas tidak ditemukan")
        if (t.asal != "manual") throw TugasGalat(null, "hanya tugas manual yang bisa dihapus")
        if (satu("SELECT 1 AS ada FROM tasks WHERE duplikat_dari = ?", id) != null) {
            throw TugasGalat(null, "ada tugas lain yang ditandai sebagai duplikat tugas ini")
        }
        for (tabel in ANAK_TASKS) exec("DELETE FROM $tabel WHERE task_id = ?", id)
        exec("DELETE FROM tasks WHERE id = ?", id)
        judul = t.judul
    }
    return judul
}

/**
 * Pilihan mata kuliah untuk tugas manual: dari jadwal kuliah dulu, lalu mata kuliah situs
 * yang diikuti; satu baris per kode MK (kode ke nama), urut nama. Sejalan dengan [kodeDikenal].
 */
fun Db.pilihanMatkul(): List<Pair<String, String>> {
    val hasil = LinkedHashMap<String, String>()
    semua("SELECT kode_mk, matkul FROM class_schedule WHERE kode_mk IS NOT NULL AND kode_mk != '' ORDER BY id")
        .forEach { hasil.putIfAbsent(it["kode_mk"] as String, it["matkul"] as String) }
    semua(
        "SELECT kode_mk, nama FROM courses WHERE diikuti = 1 AND kode_mk IS NOT NULL AND kode_mk != '' " +
            "ORDER BY source_id, external_id",
    ).forEach { hasil.putIfAbsent(it["kode_mk"] as String, it["nama"] as String) }
    return hasil.entries.map { it.key to it.value }.sortedBy { it.second.lowercase() }
}
