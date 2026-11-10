package io.github.quazzong.gwado.fetch

import io.github.quazzong.gwado.sumber.Kontrak
import io.github.quazzong.gwado.sumber.Moodle
import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

/** Kontrak golden/fetch/moodle.json. */
class FetchMoodleTest {
    private val g = Kontrak.muat("golden/fetch/moodle.json")
    private val situs = g.getValue("situs").jsonPrimitive.content
    private val token = g.getValue("token").jsonPrimitive.content     // karangan
    private val fungsi = g.getValue("fungsi").jsonPrimitive.content

    /** Nilai masukan golden → nilai Kotlin (angka jadi Long, daftar jadi List). */
    private fun nilai(e: JsonElement): Any = when (e) {
        is JsonArray -> e.map { nilai(it) }
        else -> e.jsonPrimitive.let { it.longOrNull ?: it.content }
    }

    /** Sama dengan _hasil di skrip golden: {"ok": true} atau {"jenis", "pesan"}. */
    private fun hasil(panggil: () -> Unit): JsonObject = try {
        panggil()
        buildJsonObject { put("ok", true) }
    } catch (e: FetchError) {
        buildJsonObject { put("jenis", e.jenis); put("pesan", e.pesan) }
    }

    @Test
    fun konstanta() {
        val k = g.getValue("konstanta").jsonObject
        assertEquals(k.getValue("timeout").jsonPrimitive.int, TIMEOUT)
        assertEquals(k.getValue("jeda_ulang").jsonArray.map { it.jsonPrimitive.int }, JEDA_ULANG)
        assertEquals(k.getValue("jendela_detail_detik").jsonPrimitive.long, Moodle.JENDELA_DETAIL.seconds)
    }

    @Test
    fun parameterUrlDanBadan() {
        g.getValue("param").jsonArray.forEachIndexed { i, el ->
            val c = el.jsonObject
            val masukan = c.getValue("masukan").jsonArray.map {
                it.jsonArray[0].jsonPrimitive.content to nilai(it.jsonArray[1])
            }
            val harap = c.getValue("pasangan").jsonArray.map {
                it.jsonArray[0].jsonPrimitive.content to it.jsonArray[1].jsonPrimitive.content
            }
            assertEquals("param[$i] pasangan", harap, paramMoodle(masukan))

            var dapatUrl: String? = null
            var dapatBadan: String? = null
            val klien = KlienMoodle(situs, token) { u, d, _ ->
                dapatUrl = u
                dapatBadan = d?.decodeToString()
                JsonObject(emptyMap())
            }
            klien.panggil(fungsi, *masukan.toTypedArray())
            assertEquals("param[$i] url", c.getValue("url").jsonPrimitive.content, dapatUrl)
            assertEquals("param[$i] badan", c.getValue("badan").jsonPrimitive.content, dapatBadan)
        }
    }

    @Test
    fun klasifikasiJawabanMoodle() {
        g.getValue("jawaban").jsonArray.forEachIndexed { i, el ->
            val c = el.jsonObject
            val j = c.getValue("jawaban")
            val klien = KlienMoodle(situs, token) { _, _, _ -> j }
            assertEquals("jawaban[$i]", c.getValue("hasil"), hasil { klien.panggil(fungsi) })
        }
    }

    @Test
    fun klasifikasiHttp() {
        g.getValue("http").jsonArray.forEachIndexed { i, el ->
            val c = el.jsonObject
            val kode = c.getValue("kode").jsonPrimitive.int
            val klien = KlienMoodle(situs, token) { _, _, _ -> throw HttpGagal(kode, null) }
            assertEquals("http[$i] $kode", c.getValue("hasil"), hasil { klien.panggil(fungsi) })
        }
    }

    @Test
    fun perluDetail() {
        val p = g.getValue("perlu_detail").jsonObject
        val sekarang = Instant.parse(p.getValue("sekarang").jsonPrimitive.content)
        p.getValue("kasus").jsonArray.forEachIndexed { i, el ->
            val c = el.jsonObject
            val due = c.getValue("duedate").jsonPrimitive.longOrNull
            assertEquals(
                "perlu_detail[$i] duedate=$due",
                c.getValue("hasil").jsonPrimitive.boolean,
                Moodle.perluDetail(due, sekarang),
            )
        }
    }
}
