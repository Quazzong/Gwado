package io.github.quazzong.gwado.sumber

import kotlinx.serialization.json.JsonObject

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

    /** "COM60004_2024|174.04.17|2026.1" → "COM60004"; tidak ketemu → null. */
    fun kodeMk(shortname: String?): String? = KODE_MK.find(shortname ?: "")?.groupValues?.get(1)

    /** Respons mod_assign_get_submission_status → "belum" | "draf" | "terkumpul" | null. */
    fun statusKumpul(mentah: JsonObject?): String? {
        if (mentah.isNullOrEmpty()) return null
        val sub = mentah.obj("lastattempt")?.obj("submission") ?: return null
        return STATUS_ASSIGN[sub.teks("status")]
    }

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

    /** statusPerAssign: {id assign: respons submission_status mentah}. */
    fun tugas(
        situs: String,
        assignments: JsonObject,
        quizzes: JsonObject,
        statusPerAssign: Map<Long, JsonObject>,
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
                    statusKumpulSitus = statusKumpul(a.angka("id")?.let { statusPerAssign[it] }),
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
                statusKumpulSitus = null, // status percobaan kuis tidak tersedia untuk mahasiswa
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
    ) = HasilSumber(kelas(coursesAll), tugas(situs, assignments, quizzes, statusPerAssign))
}
