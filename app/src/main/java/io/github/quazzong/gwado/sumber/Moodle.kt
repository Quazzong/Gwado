package io.github.quazzong.gwado.sumber

import java.time.Duration
import java.time.Instant
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Parser Moodle (Brone, Eling): respons API mentah → kelas + tugas.
 * Terjemahan dari serper/sumber/moodle.py. Fungsi murni: tanpa jaringan, tanpa database.
 * Kebenarannya dijaga golden file di src/test/resources/kontrak.
 */
object Moodle {
    private val KODE_MK = Regex("(?<![A-Z])([A-Z]{3}\\d{5})(?!\\d)")

    private val STATUS_ASSIGN = mapOf(
        "new" to "belum",
        "reopened" to "belum",
        "draft" to "draf",
        "submitted" to "terkumpul",
    )

    /** state completion yang berarti aktivitas sudah selesai: selesai, selesai lulus, selesai gagal. */
    private val STATE_SELESAI = setOf(1L, 2L, 3L)

    /** "COM60004_2024|174.04.17|2026.1" → "COM60004"; tidak ketemu → null. */
    fun kodeMk(shortname: String?): String? = KODE_MK.find(shortname ?: "")?.groupValues?.get(1)

    /** Respons mod_assign_get_submission_status → "belum" | "draf" | "terkumpul" | null. */
    fun statusKumpul(mentah: JsonObject?): String? {
        if (mentah.isNullOrEmpty()) return null
        val sub = mentah.obj("lastattempt")?.obj("submission") ?: return null
        return STATUS_ASSIGN[sub.teks("status")]
    }

    /**
     * Respons core_completion_get_activities_completion_status per kelas → cmid yang selesai di situs.
     * Tanda ini dibuat mahasiswa sendiri ("Mark as done") atau oleh syarat otomatis, jadi bukan
     * bukti pengumpulan. Dilewati: tracking mati, tanpa completion, tidak terlihat, belum selesai.
     */
    fun selesaiPerCm(completionPerKelas: Map<Long, JsonObject>): Set<String> {
        val hasil = mutableSetOf<String>()
        for (r in completionPerKelas.values) {
            for (s in r.daftar("statuses")) {
                val cmid = s["cmid"] as? JsonPrimitive
                if (cmid == null || cmid is JsonNull || s.angka("tracking") !in setOf(1L, 2L)) continue
                if (s.bernilaiSalah("hascompletion") || s.bernilaiSalah("uservisible")) continue
                if (s.angka("state") in STATE_SELESAI) hasil += cmid.content
            }
        }
        return hasil
    }

    private fun JsonObject.bernilaiSalah(k: String): Boolean =
        (this[k] as? JsonPrimitive)?.let { !it.isString && it.booleanOrNull == false } == true

    /**
     * Status pengumpulan + tanda selesai di situs → satu status situs.
     * Urutan: terkumpul > draf > selesai > belum > tidak diketahui. Draf tidak ditimpa karena
     * artinya tugas memang belum terkirim. Hanya ditampilkan, tidak pernah mencentang tugas.
     */
    fun gabungStatus(kumpul: String?, selesai: Boolean): String? =
        if (selesai && kumpul != "terkumpul" && kumpul != "draf") "selesai" else kumpul

    fun kelas(coursesAll: JsonObject): List<Kelas> = coursesAll.daftar("courses").map { c ->
        Kelas(
            externalId = c.idTeks("id"),
            nama = c.wajibTeks("fullname"),
            shortname = c.teks("shortname"),
            kodeMk = kodeMk(c.teks("shortname")),
            kategori = c.teks("coursecategory"),
            mulai = isoDariUnix(c.angka("startdate")),
            selesai = isoDariUnix(c.angka("enddate")),
            dibuat = null,
        )
    }

    /** statusPerAssign: {id assign: respons submission_status mentah}; selesai: dari [selesaiPerCm]. */
    fun tugas(
        situs: String,
        assignments: JsonObject,
        quizzes: JsonObject,
        statusPerAssign: Map<Long, JsonObject>,
        selesai: Set<String> = emptySet(),
    ): List<Tugas> {
        val dasar = situs.trimEnd('/')
        val hasil = mutableListOf<Tugas>()
        for (c in assignments.daftar("courses")) {
            for (a in c.daftar("assignments")) {
                val cmid = a.idTeks("cmid")
                hasil += Tugas(
                    externalId = "assign:${a.idTeks("id")}",
                    courseExternalId = a.idTeks("course"),
                    jenis = "assign",
                    linkId = cmid,
                    judul = a.wajibTeks("name"),
                    deskripsi = teksPolos(a.teks("intro")),
                    deadline = isoDariUnix(a.angka("duedate")),
                    url = "$dasar/mod/assign/view.php?id=$cmid",
                    statusKumpulSitus = gabungStatus(
                        statusKumpul(a.angka("id")?.let { statusPerAssign[it] }),
                        cmid in selesai,
                    ),
                )
            }
        }
        for (q in quizzes.daftar("quizzes")) {
            val cmid = q.idTeks("coursemodule")
            hasil += Tugas(
                externalId = "quiz:${q.idTeks("id")}",
                courseExternalId = q.idTeks("course"),
                jenis = "quiz",
                linkId = cmid,
                judul = q.wajibTeks("name"),
                deskripsi = teksPolos(q.teks("intro")),
                deadline = isoDariUnix(q.angka("timeclose")),
                url = "$dasar/mod/quiz/view.php?id=$cmid",
                // status percobaan kuis tidak tersedia untuk mahasiswa, hanya tanda selesai
                statusKumpulSitus = gabungStatus(null, cmid in selesai),
            )
        }
        return hasil
    }

    fun normalisasi(
        situs: String,
        coursesAll: JsonObject,
        assignments: JsonObject,
        quizzes: JsonObject,
        statusPerAssign: Map<Long, JsonObject>,
        completionPerKelas: Map<Long, JsonObject> = emptyMap(),
    ) = HasilSumber(
        kelas(coursesAll),
        tugas(situs, assignments, quizzes, statusPerAssign, selesaiPerCm(completionPerKelas)),
    )

    /** Jendela status assign (moodle.JENDELA_DETAIL). */
    val JENDELA_DETAIL: Duration = Duration.ofDays(30)

    /** Apakah status pengumpulan assign ini perlu diambil (satu panggilan API per assign). Batas inklusif. */
    fun perluDetail(duedate: Long?, sekarang: Instant): Boolean {
        if (duedate == null || duedate == 0L) return true
        return Instant.ofEpochSecond(duedate) >= sekarang.minus(JENDELA_DETAIL)
    }
}
