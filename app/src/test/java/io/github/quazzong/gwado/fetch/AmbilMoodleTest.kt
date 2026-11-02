package io.github.quazzong.gwado.fetch

import io.github.quazzong.gwado.sumber.Kontrak
import io.github.quazzong.gwado.sumber.Moodle
import java.net.URLDecoder
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private val TOKEN = "f".repeat(32)                       // karangan
private val SEKARANG = Instant.parse("2026-10-19T05:00:00Z")
private const val BRONE = "https://brone.ub.ac.id"
private val ERROR_BIASA = Json.parseToJsonElement(
    """{"exception": "x", "errorcode": "nopermission", "message": "x"}""",
)

private fun uraiForm(badan: String): Map<String, String> =
    badan.split("&").filter { it.isNotEmpty() }.associate { bagian ->
        val kv = bagian.split("=", limit = 2)
        URLDecoder.decode(kv[0], "UTF-8") to URLDecoder.decode(kv.getOrElse(1) { "" }, "UTF-8")
    }

private fun assignFixture(situs: String): List<JsonObject> =
    Kontrak.muat("fixtures/$situs/assignments.json").getValue("courses").jsonArray
        .flatMap { it.jsonObject.getValue("assignments").jsonArray }
        .map { it.jsonObject }

private fun JsonObject.due(): Long? = this["duedate"]?.jsonPrimitive?.longOrNull

/** Seperti MoodlePalsu di test_fetch.py, tapi menjawab dari fixture karangan kontrak. */
private class MoodlePalsu(
    val situs: String,
    val tokenBenar: String = TOKEN,
    val gagalStatus: String? = null,      // null | "lain" | "jaringan"
    val tanpaKelas: Boolean = false,
) : Minta {
    val panggilan = mutableListOf<Pair<String, Map<String, String>>>()

    override fun invoke(url: String, data: ByteArray?, headers: Map<String, String>): JsonElement {
        assertFalse("token tidak boleh di URL", TOKEN in url)
        val p = uraiForm(requireNotNull(data) { "Moodle harus POST" }.decodeToString())
        panggilan += url to p
        if (p["wstoken"] != tokenBenar) {
            return Json.parseToJsonElement(
                """{"exception": "moodle_exception", "errorcode": "invalidtoken", "message": "x"}""",
            )
        }
        val d = "fixtures/$situs"
        return when (val f = p["wsfunction"]) {
            "core_course_get_enrolled_courses_by_timeline_classification" ->
                if (tanpaKelas) Json.parseToJsonElement("""{"courses": []}""") else Kontrak.muat("$d/courses_all.json")
            "mod_assign_get_assignments" -> Kontrak.muat("$d/assignments.json")
            "mod_quiz_get_quizzes_by_courses" -> Kontrak.muat("$d/quizzes.json")
            "mod_assign_get_submission_status" -> when (gagalStatus) {
                "lain" -> ERROR_BIASA
                "jaringan" -> throw FetchError("jaringan", "tidak tersambung setelah 3 percobaan (IOException)")
                else -> {
                    val nama = "$d/submission_status_${p.getValue("assignid")}.json"
                    // tidak ada di fixture → error biasa → status "tidak diketahui", sama seperti golden
                    if (javaClass.getResource("/kontrak/$nama") != null) Kontrak.muat(nama) else ERROR_BIASA
                }
            }
            else -> error("fungsi tak terduga: $f")
        }
    }

    fun fungsi(nama: String) = panggilan.map { it.second }.filter { it["wsfunction"] == nama }
}

class AmbilMoodleTest {
    /** sekarang = deadline paling awal → semua assign masuk jendela → hasil harus sama dengan golden parser. */
    private fun cekUtuh(situs: String) {
        val semua = assignFixture(situs)
        val awal = semua.mapNotNull { it.due() }.filter { it > 0 }.minOrNull()
        val sekarang = if (awal != null) Instant.ofEpochSecond(awal) else SEKARANG
        val alamat = "https://$situs.ub.ac.id"
        val palsu = MoodlePalsu(situs)

        val hasil = ambilMoodle(alamat, TOKEN, sekarang, palsu)

        Kontrak.cocokGolden("moodle_$situs", hasil)
        assertTrue(palsu.panggilan.all { (u, _) -> u == "$alamat/webservice/rest/server.php" })
        assertTrue("courseids[0]" in palsu.fungsi("mod_assign_get_assignments").single())
        assertEquals(semua.size, palsu.fungsi("mod_assign_get_submission_status").size)
    }

    @Test fun utuhBroneSamaDenganGolden() = cekUtuh("brone")

    @Test fun utuhElingSamaDenganGolden() = cekUtuh("eling")

    @Test
    fun statusHanyaDalamJendela() {
        val semua = assignFixture("brone")
        val sekarang = Instant.ofEpochSecond(semua.mapNotNull { it.due() }.max()).plus(Duration.ofDays(31))
        val palsu = MoodlePalsu("brone")

        ambilMoodle(BRONE, TOKEN, sekarang, palsu)

        val diminta = palsu.fungsi("mod_assign_get_submission_status").map { it.getValue("assignid").toLong() }
        val harus = semua.filter { Moodle.perluDetail(it.due(), sekarang) }.map { it.getValue("id").jsonPrimitive.long }
        assertEquals(harus.sorted(), diminta.sorted())
        assertTrue("jendela harus menyaring sebagian assign", diminta.size < semua.size)
    }

    @Test
    fun statusGagalBiasaTidakMenggagalkanRun() {
        val hasil = ambilMoodle(BRONE, TOKEN, SEKARANG, MoodlePalsu("brone", gagalStatus = "lain"))
        assertTrue(hasil.tugas.isNotEmpty())
        assertTrue(hasil.tugas.filter { it.jenis == "assign" }.all { it.statusKumpulSitus == null })
    }

    @Test
    fun statusGagalJaringanMenggagalkanRun() {
        val e = assertThrows(FetchError::class.java) {
            ambilMoodle(BRONE, TOKEN, SEKARANG, MoodlePalsu("brone", gagalStatus = "jaringan"))
        }
        assertEquals("jaringan", e.jenis)
    }

    @Test
    fun tokenDitolak() {
        val e = assertThrows(FetchError::class.java) {
            ambilMoodle(BRONE, "salah", SEKARANG, MoodlePalsu("brone"))
        }
        assertEquals("token_ditolak", e.jenis)
    }

    @Test
    fun tanpaKelasHanyaSatuPanggilan() {
        val palsu = MoodlePalsu("brone", tanpaKelas = true)
        val hasil = ambilMoodle(BRONE, TOKEN, SEKARANG, palsu)
        assertEquals(0, hasil.kelas.size)
        assertEquals(0, hasil.tugas.size)
        assertEquals(1, palsu.panggilan.size)
    }
}
