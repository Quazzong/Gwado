package io.github.quazzong.gwado.tugas

import io.github.quazzong.gwado.db.Db

data class BarisTugas(
    val id: String,
    val judul: String,
    val sumber: String?,
    val kelas: String?,
    val jenis: String,
    val deadline: String?,
    val statusKumpulSitus: String?,
    val statusSitus: String,
)

fun Db.daftarTugas(): List<BarisTugas> = semua(
    """
    SELECT t.id, t.judul, s.nama AS sumber, c.nama AS kelas, t.jenis, t.deadline,
           t.status_kumpul_situs, t.status_situs
    FROM tasks t
    LEFT JOIN courses c ON c.id = t.course_id
    LEFT JOIN sources s ON s.id = t.source_id
    WHERE t.asal = 'manual' OR c.diikuti = 1
    ORDER BY t.deadline IS NULL, t.deadline, t.id
    """,
).map {
    BarisTugas(
        id = it["id"] as String,
        judul = it["judul"] as String,
        sumber = it["sumber"] as String?,
        kelas = it["kelas"] as String?,
        jenis = it["jenis"] as String,
        deadline = it["deadline"] as String?,
        statusKumpulSitus = it["status_kumpul_situs"] as String?,
        statusSitus = it["status_situs"] as String,
    )
}
