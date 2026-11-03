package io.github.quazzong.gwado.semester

import io.github.quazzong.gwado.db.Db
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

// Aturan semester adalah data (§5.1 konteks): tiga nilai di tabel preferences.
// Edisi publik: ditebak dari tanggal (terjemahan serper/semester.py, dibuktikan golden),
// lalu dikonfirmasi atau dibetulkan pengguna. Ingest menolak jalan sebelum ketiganya tersimpan.

/** Kunci preferensi → label isian, dalam urutan tampil. */
val LABEL_SEMESTER: Map<String, String> = linkedMapOf(
    "semester_label" to "Label semester (kategori Brone), contoh Ganjil 2026/2027",
    "semester_kode" to "Kode semester (shortname Eling), contoh 20261",
    "semester_mulai" to "Mulai semester (kelas Classroom dibuat sejak), contoh 2026-08-01",
)

private val WIB = ZoneOffset.ofHours(7)

/** Tanggal WIB (satu hari = 00.00–24.00 WIB). */
fun tanggalWib(t: Instant): LocalDate = t.atOffset(WIB).toLocalDate()

/** Kalender UB: Ganjil Agustus–Januari, Genap Februari–Juli. Masukan = tanggal WIB. */
fun tebakSemester(tanggal: LocalDate): Map<String, String> {
    val y = tanggal.year
    val m = tanggal.monthValue
    if (m >= 8 || m == 1) {
        val awal = if (m >= 8) y else y - 1
        return mapOf(
            "semester_label" to "Ganjil $awal/${awal + 1}",
            "semester_kode" to "${awal}1",
            "semester_mulai" to "$awal-08-01",
        )
    }
    val awal = y - 1
    return mapOf(
        "semester_label" to "Genap $awal/$y",
        "semester_kode" to "${awal}2",            // format kode genap belum dicek dengan data asli
        "semester_mulai" to "$y-02-01",
    )
}

/** Pesan masalah isian, atau null kalau bisa disimpan. */
fun periksaSemester(nilai: Map<String, String>): String? {
    if (LABEL_SEMESTER.keys.any { nilai[it].isNullOrBlank() }) return "Ketiga isian semester wajib diisi"
    // aturan Classroom membandingkan teks tanggal, jadi bentuknya harus persis YYYY-MM-DD
    try {
        LocalDate.parse(nilai.getValue("semester_mulai").trim())
    } catch (e: DateTimeParseException) {
        return "Mulai semester harus tanggal YYYY-MM-DD, contoh 2026-08-01"
    }
    return null
}

/** Ketiga nilai tersimpan, atau null kalau belum lengkap (belum dikonfirmasi pengguna). */
fun Db.semesterTersimpan(): Map<String, String>? {
    val a = semua("SELECT kunci, nilai FROM preferences WHERE kunci IN (?, ?, ?)", *LABEL_SEMESTER.keys.toTypedArray())
        .associate { it["kunci"] as String to it["nilai"] as String }
    return a.takeIf { it.keys == LABEL_SEMESTER.keys }
}

/** Simpan pilihan pengguna, ketiganya sekaligus. Berlaku mulai ingest berikutnya. */
fun Db.simpanSemester(nilai: Map<String, String>) {
    periksaSemester(nilai)?.let { throw IllegalArgumentException(it) }
    transaksi {
        for (k in LABEL_SEMESTER.keys) {
            exec("INSERT OR REPLACE INTO preferences (kunci, nilai) VALUES (?, ?)", k, nilai.getValue(k).trim())
        }
    }
}

/** Per sumber: "N diikuti · M dilewati" */
fun Db.ringkasanMatkul(): Map<String, String> =
    semua("SELECT source_id, sum(diikuti = 1) AS diikuti, sum(diikuti = 0) AS dilewati FROM courses GROUP BY source_id")
        .associate { it["source_id"] as String to "${it["diikuti"]} diikuti · ${it["dilewati"]} dilewati" }
