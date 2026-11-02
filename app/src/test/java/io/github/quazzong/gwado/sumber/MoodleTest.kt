package io.github.quazzong.gwado.sumber

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class MoodleTest {
    /** submission_status_<id assign>.json → {id: isi}, sama seperti buat_golden_parser.py */
    private fun statusMoodle(situs: String): Map<Long, JsonObject> =
        Kontrak.file("fixtures/$situs").listFiles()!!
            .filter { it.name.startsWith("submission_status_") }
            .associate {
                it.nameWithoutExtension.removePrefix("submission_status_").toLong() to
                    Json.parseToJsonElement(it.readText()).jsonObject
            }

    private fun cek(situs: String) {
        val d = "fixtures/$situs"
        val hasil = Moodle.normalisasi(
            "https://$situs.ub.ac.id",
            Kontrak.muat("$d/courses_all.json"),
            Kontrak.muat("$d/assignments.json"),
            Kontrak.muat("$d/quizzes.json"),
            statusMoodle(situs),
        )
        Kontrak.cocokGolden("moodle_$situs", hasil)
    }

    @Test fun goldenBrone() = cek("brone")

    @Test fun goldenEling() = cek("eling")
}
