package io.github.quazzong.gwado.semester

import io.github.quazzong.gwado.db.Db

/** Satu mata kuliah dari situs. aksi null = mengikuti aturan semester. kodeMk null = situs tidak memberi kode. */
data class Matkul(
    val externalId: String,
    val nama: String,
    val diikuti: Boolean,
    val alasan: String?,
    val aksi: String?,
    val kodeMk: String? = null,
)

/** Semua mata kuliah satu sumber: yang diikuti dulu, lalu menurut nama. */
fun Db.daftarMatkul(sumber: String): List<Matkul> = semua(
    """
    SELECT c.external_id, c.nama, c.diikuti, c.alasan_diikuti, o.aksi, c.kode_mk
    FROM courses c
    LEFT JOIN course_overrides o ON o.source_id = c.source_id AND o.external_id = c.external_id
    WHERE c.source_id = ?
    ORDER BY c.diikuti DESC, c.nama, c.external_id
    """,
    sumber,
).map {
    Matkul(
        externalId = it["external_id"] as String,
        nama = it["nama"] as String,
        diikuti = it["diikuti"] == 1L,
        alasan = it["alasan_diikuti"] as String?,
        aksi = it["aksi"] as String?,
        kodeMk = it["kode_mk"] as String?,
    )
}

/** aksi "ikuti" / "abaikan", atau null untuk kembali ke aturan semester. Berlaku saat ingest berikutnya. */
fun Db.aturPengecualian(sumber: String, externalId: String, aksi: String?) {
    require(aksi == null || aksi == "ikuti" || aksi == "abaikan") { "aksi tidak dikenal: $aksi" }
    if (aksi == null) {
        exec("DELETE FROM course_overrides WHERE source_id = ? AND external_id = ?", sumber, externalId)
    } else {
        exec(
            "INSERT OR REPLACE INTO course_overrides (source_id, external_id, aksi) VALUES (?, ?, ?)",
            sumber, externalId, aksi,
        )
    }
}
