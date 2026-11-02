package io.github.quazzong.gwado.sumber

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Parser Google Classroom: respons API mentah → kelas + tugas.
 * Terjemahan dari serper/sumber/classroom.py. Fungsi murni: tanpa jaringan, tanpa database.
 */
object Classroom {
    private val STATUS = mapOf(
        "NEW" to "belum",
        "CREATED" to "belum",
        "RECLAIMED_BY_STUDENT" to "belum", // sudah dikumpulkan lalu ditarik kembali
        "TURNED_IN" to "terkumpul",
        "RETURNED" to "terkumpul",         // sudah dinilai dan dikembalikan dosen
    )

    /** "2026-09-02T07:16:05.485Z" → "2026-09-02T07:16:05Z" (pecahan detik dipotong). */
    fun isoDariGoogle(teks: String?): String? {
        if (teks.isNullOrEmpty()) return null
        return FORMAT_ISO.format(OffsetDateTime.parse(teks).toInstant())
    }

    /** dueDate + dueTime (UTC). Google tidak menulis field bernilai 0: {"hours": 16} = 16.00, {} = 00.00. */
    fun deadline(cw: JsonObject): String? {
        val tgl = cw.obj("dueDate")
        if (tgl.isNullOrEmpty()) return null
        val jam = cw.obj("dueTime")
        val t = LocalDateTime.of(
            requireNotNull(tgl.angka("year")) { "dueDate tanpa year" }.toInt(),
            requireNotNull(tgl.angka("month")) { "dueDate tanpa month" }.toInt(),
            requireNotNull(tgl.angka("day")) { "dueDate tanpa day" }.toInt(),
            (jam?.angka("hours") ?: 0L).toInt(),
            (jam?.angka("minutes") ?: 0L).toInt(),
        )
        return FORMAT_ISO.format(t.toInstant(ZoneOffset.UTC))
    }

    fun kelas(coursesMentah: JsonObject): List<Kelas> = coursesMentah.daftar("courses").map { c ->
        Kelas(
            externalId = c.idTeks("id"),
            nama = c.wajibTeks("name"),
            shortname = c.teks("section"),
            kodeMk = null,                    // Classroom tidak punya kode MK; dipetakan manual
            kategori = c.teks("courseState"), // ACTIVE, ARCHIVED, ...
            mulai = null,
            selesai = null,
            dibuat = isoDariGoogle(c.teks("creationTime")),
        )
    }

    /**
     * Keduanya: {id kelas: respons mentah}. Submissions boleh tidak lengkap.
     * Urutan tugas mengikuti urutan map courseworkPerKelas.
     */
    fun tugas(
        courseworkPerKelas: Map<String, JsonObject>,
        submissionsPerKelas: Map<String, JsonObject>,
    ): List<Tugas> {
        val status = mutableMapOf<String, String?>()
        for (mentah in submissionsPerKelas.values) {
            for (s in mentah.daftar("studentSubmissions")) {
                status[s.idTeks("courseWorkId")] = STATUS[s.teks("state")]
            }
        }
        return courseworkPerKelas.values.flatMap { mentah ->
            mentah.daftar("courseWork").map { cw ->
                Tugas(
                    externalId = cw.idTeks("id"),
                    courseExternalId = cw.idTeks("courseId"),
                    jenis = "coursework",
                    linkId = null,
                    judul = cw.wajibTeks("title"),
                    deskripsi = potongSpasi(cw.teks("description") ?: ""),
                    deadline = deadline(cw),
                    url = cw.teks("alternateLink"),
                    statusKumpulSitus = status[cw.idTeks("id")],
                )
            }
        }
    }

    fun normalisasi(
        coursesMentah: JsonObject,
        courseworkPerKelas: Map<String, JsonObject>,
        submissionsPerKelas: Map<String, JsonObject>,
    ) = HasilSumber(kelas(coursesMentah), tugas(courseworkPerKelas, submissionsPerKelas))

    /** Jendela kelas (classroom.JENDELA_KELAS). */
    val JENDELA_KELAS: Duration = Duration.ofDays(365)

    /**
     * Apakah tugas kelas ini perlu diambil (2 panggilan API per kelas). Batas inklusif.
     * Catatan: updateTime = waktu terakhir PENGATURAN kelas diubah, bukan waktu tugas terakhir diposting.
     */
    fun perluDetail(kelas: JsonObject, sekarang: Instant): Boolean {
        fun isi(k: String) = (kelas[k] as? JsonPrimitive)?.contentOrNull?.ifEmpty { null }
        val t = isi("updateTime") ?: isi("creationTime") ?: return true
        return OffsetDateTime.parse(t).toInstant() >= sekarang.minus(JENDELA_KELAS)
    }
}
