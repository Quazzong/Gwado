package io.github.quazzong.gwado.sumber

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
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

    /** completion_<id kelas>.json → {id kelas: isi}, sama seperti buat_golden_parser.py */
    private fun completionMoodle(situs: String): Map<Long, JsonObject> =
        Kontrak.file("fixtures/$situs").listFiles()!!
            .filter { it.name.startsWith("completion_") }
            .associate {
                it.nameWithoutExtension.removePrefix("completion_").toLong() to
                    Json.parseToJsonElement(it.readText()).jsonObject
            }

    private fun cek(situs: String, denganCompletion: Boolean = false) {
        val d = "fixtures/$situs"
        val hasil = Moodle.normalisasi(
            "https://$situs.ub.ac.id",
            Kontrak.muat("$d/courses_all.json"),
            Kontrak.muat("$d/assignments.json"),
            Kontrak.muat("$d/quizzes.json"),
            statusMoodle(situs),
            if (denganCompletion) completionMoodle(situs) else emptyMap(),
        )
        Kontrak.cocokGolden(if (denganCompletion) "moodle_${situs}_selesai" else "moodle_$situs", hasil)
    }

    @Test fun goldenBrone() = cek("brone")

    @Test fun goldenEling() = cek("eling")

    @Test fun goldenBroneSelesai() = cek("brone", denganCompletion = true)

    @Test fun goldenElingSelesai() = cek("eling", denganCompletion = true)

    @Test
    fun urutanStatus() {
        val kasus = listOf(
            Triple("terkumpul", true, "terkumpul"), Triple("draf", true, "draf"),
            Triple("belum", true, "selesai"), Triple(null, true, "selesai"),
            Triple("terkumpul", false, "terkumpul"), Triple("draf", false, "draf"),
            Triple("belum", false, "belum"), Triple(null, false, null),
        )
        for ((kumpul, selesai, harap) in kasus) {
            assertEquals("$kumpul + $selesai", harap, Moodle.gabungStatus(kumpul, selesai))
        }
    }

    private fun st(cmid: Int?, state: Int = 1, tracking: Int = 1, ada: Boolean = true, terlihat: Boolean = true) =
        buildJsonObject {
            if (cmid != null) put("cmid", cmid)
            put("state", state)
            put("tracking", tracking)
            put("hascompletion", ada)
            put("uservisible", terlihat)
        }

    private fun respons(vararg s: JsonObject) = buildJsonObject {
        putJsonArray("statuses") { s.forEach { add(it) } }
        putJsonArray("warnings") {}
    }

    @Test
    fun hanyaYangSelesaiDanTerlacak() {
        val satu = respons(
            st(10), st(11, state = 2), st(12, state = 3, tracking = 2),
            st(13, state = 0), st(14, tracking = 0), st(15, ada = false), st(16, terlihat = false), st(null),
        )
        assertEquals(setOf("10", "11", "12", "20"), Moodle.selesaiPerCm(mapOf(1L to satu, 2L to respons(st(20)))))
        assertEquals(emptySet<String>(), Moodle.selesaiPerCm(emptyMap()))
        assertEquals(emptySet<String>(), Moodle.selesaiPerCm(mapOf(1L to buildJsonObject {})))
    }
}
