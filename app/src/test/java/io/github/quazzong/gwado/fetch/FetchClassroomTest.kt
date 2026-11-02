package io.github.quazzong.gwado.fetch

import io.github.quazzong.gwado.sumber.Classroom
import io.github.quazzong.gwado.sumber.Kontrak
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
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kontrak golden/fetch/classroom.json (dibuat serper-app tests/buat_golden_fetch_classroom.py). */
class FetchClassroomTest {
    private val g = Kontrak.muat("golden/fetch/classroom.json")
    private val akses = g.getValue("akses").jsonPrimitive.content      // karangan

    private fun hasil(panggil: () -> Unit): JsonObject = try {
        panggil()
        buildJsonObject { put("ok", true) }
    } catch (e: FetchError) {
        buildJsonObject { put("jenis", e.jenis); put("pesan", e.pesan) }
    }

    @Test
    fun konstanta() {
        val k = g.getValue("konstanta").jsonObject
        assertEquals(k.getValue("jendela_kelas_detik").jsonPrimitive.long, Classroom.JENDELA_KELAS.seconds)
        assertEquals(k.getValue("api_classroom").jsonPrimitive.content, API_CLASSROOM)
    }

    @Test
    fun daftarBerhalaman() {
        g.getValue("daftar").jsonArray.forEachIndexed { i, el ->
            val c = el.jsonObject
            val sisa = ArrayDeque<JsonElement>(c.getValue("halaman").jsonArray)
            val dapat = mutableListOf<JsonObject>()
            val klien = KlienGoogle(akses) { url, data, headers ->
                assertNull("Classroom harus GET", data)
                dapat += buildJsonObject {
                    put("url", url)
                    putJsonObject("headers") { headers.forEach { (k, v) -> put(k, v) } }
                }
                sisa.removeFirst()
            }
            val param = c.getValue("param").jsonArray.map {
                it.jsonArray[0].jsonPrimitive.content to it.jsonArray[1].jsonPrimitive.content
            }
            val isi = klien.daftar(
                c.getValue("path").jsonPrimitive.content,
                c.getValue("kunci").jsonPrimitive.content,
                *param.toTypedArray(),
            )
            assertEquals("daftar[$i] permintaan", c.getValue("permintaan"), JsonArray(dapat))
            assertEquals("daftar[$i] hasil", c.getValue("hasil"), JsonArray(isi))
            assertTrue("daftar[$i] semua halaman terpakai", sisa.isEmpty())
        }
    }

    @Test
    fun klasifikasiHttp() {
        for (el in g.getValue("http").jsonArray) {
            val c = el.jsonObject
            val kode = c.getValue("kode").jsonPrimitive.int
            val klien = KlienGoogle(akses) { _, _, _ -> throw HttpGagal(kode, null) }
            assertEquals("http $kode", c.getValue("hasil"), hasil { klien.daftar("courses", "courses", "studentId" to "me") })
        }
    }

    @Test
    fun perluDetailKelas() {
        val p = g.getValue("perlu_detail").jsonObject
        val sekarang = Instant.parse(p.getValue("sekarang").jsonPrimitive.content)
        p.getValue("kasus").jsonArray.forEachIndexed { i, el ->
            val c = el.jsonObject
            val kelas = c.getValue("kelas").jsonObject
            assertEquals(
                "perlu_detail[$i] $kelas",
                c.getValue("hasil").jsonPrimitive.boolean,
                Classroom.perluDetail(kelas, sekarang),
            )
        }
    }
}
