package io.github.quazzong.gwado.tugas

import io.github.quazzong.gwado.db.Db

data class BarisTugas(
    val id: String,
    val jenis: String,
    val judul: String,
    val deadline: String?,
    val asal: String,
    val statusSitus: String,
    val statusKumpulSitus: String?,
    val matkul: String?,
    val sumber: String?,
    val status: String,
)

/**
 * Label mata kuliah: nama di jadwal kuliah lewat kode MK; lalu nama kelas situs; untuk tugas
 * manual, nama kelas situs yang diikuti dengan kode yang sama; terakhir kodenya sendiri.
 */
internal const val SQL_MATKUL = """coalesce(
         (SELECT j.matkul FROM class_schedule j
          WHERE j.kode_mk = coalesce(t.kode_mk_manual, nullif(c.kode_mk, ''))
          ORDER BY j.id LIMIT 1),
         c.nama,
         (SELECT k.nama FROM courses k
          WHERE k.kode_mk = t.kode_mk_manual AND k.diikuti = 1
          ORDER BY k.source_id, k.external_id LIMIT 1),
         t.kode_mk_manual)"""

private const val SQL_DAFTAR = """
SELECT t.id, t.jenis, t.judul, t.deadline, t.asal, t.status_situs, t.status_kumpul_situs,
       $SQL_MATKUL AS matkul, s.nama AS sumber,
       coalesce(u.status, 'todo') AS status
FROM tasks t
LEFT JOIN courses c ON c.id = t.course_id
LEFT JOIN sources s ON s.id = t.source_id
LEFT JOIN task_user_state u ON u.task_id = t.id
WHERE t.duplikat_dari IS NULL
  AND (t.course_id IS NULL OR c.diikuti = 1)
  AND (? OR (coalesce(u.status, 'todo') != 'done' AND t.status_situs = 'aktif'))
ORDER BY t.deadline IS NULL, t.deadline, t.judul, t.id
"""

/**
 * Daftar tugas urut deadline (tanpa deadline paling bawah). Duplikat dan tugas dari mata kuliah
 * yang tidak diikuti selalu disembunyikan; tugas selesai dan yang hilang dari situs hanya ikut
 * kalau [semua].
 */
fun Db.daftarTugas(semua: Boolean = false): List<BarisTugas> =
    semua(SQL_DAFTAR, if (semua) 1L else 0L).map {
        BarisTugas(
            id = it["id"] as String,
            jenis = it["jenis"] as String,
            judul = it["judul"] as String,
            deadline = it["deadline"] as String?,
            asal = it["asal"] as String,
            statusSitus = it["status_situs"] as String,
            statusKumpulSitus = it["status_kumpul_situs"] as String?,
            matkul = it["matkul"] as String?,
            sumber = it["sumber"] as String?,
            status = it["status"] as String,
        )
    }
