package io.github.quazzong.gwado.fetch

import io.github.quazzong.gwado.sumber.Classroom
import io.github.quazzong.gwado.sumber.Kontrak
import java.net.URLDecoder
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val AKSES_PALSU = "ya29.karangan"
private val KURSUS = Kontrak.muat("fixtures/classroom/courses.json").getValue("courses").jsonArray.map { it.jsonObject }

private fun uraiQuery(q: String): Map<String, String> =
    q.split("&").filter { it.isNotEmpty() }.associate { bagian ->
        val kv = bagian.split("=", limit = 2)
        URLDecoder.decode(kv[0], "UTF-8") to URLDecoder.decode(kv.getOrElse(1) { "" }, "UTF-8")
    }

/** Waktu acuan kelas (updateTime, lalu creationTime), seperti perlu_detail. */
private fun waktuKelas(k: JsonObject): Instant? =
    listOf("updateTime", "creationTime")
        .firstNotNullOfOrNull { (k[it] as? JsonPrimitive)?.contentOrNull?.ifEmpty { null } }
        ?.let { OffsetDateTime.parse(it).toInstant() }

/** Seperti GooglePalsu di test_fetch.py, tapi dari fixture karangan kontrak. Daftar kelas dua halaman. */
private class GooglePalsu(val kelasGagal: String? = null, val kodeGagal: Int = 403) : Minta {
    val diminta = mutableListOf<String>()
    private val halaman = listOf(JsonArray(KURSUS.take(2)), JsonArray(KURSUS.drop(2)))

    override fun invoke(url: String, data: ByteArray?, headers: Map<String, String>): JsonElement {
        assertNull("Classroom harus GET", data)
        assertEquals(mapOf("Authorization" to "Bearer $AKSES_PALSU"), headers)
        assertFalse("akses tidak boleh di URL", AKSES_PALSU in url)
        val path = url.removePrefix(API_CLASSROOM).substringBefore("?")
        val q = uraiQuery(url.substringAfter("?", ""))
        diminta += path
        if (path == "courses") {
            val i = q["pageToken"]?.toInt() ?: 0
            return buildJsonObject {
                put("courses", halaman[i])
                if (i + 1 < halaman.size) put("nextPageToken", (i + 1).toString())
            }
        }
        val kelas = path.split("/")[1]
        if (kelas == kelasGagal) throw HttpGagal(kodeGagal, null)
        val awalan = if (path.endsWith("courseWork")) "coursework_" else "submissions_"
        val nama = "fixtures/classroom/$awalan$kelas.json"
        return if (javaClass.getResource("/kontrak/$nama") != null) Kontrak.muat(nama) else JsonObject(emptyMap())
    }
}

class AmbilClassroomTest {
    /** Waktu paling awal di fixture → semua kelas masuk jendela. */
    private val semuaMasuk = KURSUS.mapNotNull { waktuKelas(it) }.minOrNull() ?: Instant.parse("2026-10-19T05:00:00Z")

    @Test
    fun utuhSamaDenganGoldenParser() {
        val palsu = GooglePalsu()
        val hasil = ambilClassroom(AKSES_PALSU, semuaMasuk, palsu)

        val harap = Kontrak.muat("golden/parser/classroom.json")
        val dapat = Kontrak.keJson(hasil)
        assertEquals("courses (urutan dari API)", harap.getValue("courses"), dapat.getValue("courses"))
        // golden parser urut nama file, fetch urut kelas dari API: bandingkan isi tugas setelah diurutkan
        assertEquals(
            "tasks",
            harap.getValue("tasks").jsonArray.sortedBy { kunciTugas(it) },
            dapat.getValue("tasks").jsonArray.sortedBy { kunciTugas(it) },
        )
        assertEquals(2, palsu.diminta.count { it == "courses" })
        assertEquals(2 + 2 * KURSUS.size, palsu.diminta.size)
    }

    @Test
    fun hanyaKelasDalamJendela() {
        val akhir = KURSUS.mapNotNull { waktuKelas(it) }.max()
        val sekarang = akhir.plus(Duration.ofDays(366))
        val palsu = GooglePalsu()
        val hasil = ambilClassroom(AKSES_PALSU, sekarang, palsu)

        val dalam = KURSUS.count { Classroom.perluDetail(it, sekarang) }
        assertEquals(2 + 2 * dalam, palsu.diminta.size)
        assertTrue("jendela harus menyaring sebagian kelas", dalam < KURSUS.size)
        assertEquals("semua kelas tetap tersimpan", KURSUS.size, hasil.kelas.size)
    }

    @Test
    fun satuKelasGagalSeluruhRunGagal() {
        val id = KURSUS.first().getValue("id").jsonPrimitive.content
        val e = assertThrows(FetchError::class.java) {
            ambilClassroom(AKSES_PALSU, semuaMasuk, GooglePalsu(kelasGagal = id))
        }
        assertEquals("lain", e.jenis)
    }

    @Test
    fun aksesDitolak401() {
        val id = KURSUS.first().getValue("id").jsonPrimitive.content
        val e = assertThrows(FetchError::class.java) {
            ambilClassroom(AKSES_PALSU, semuaMasuk, GooglePalsu(kelasGagal = id, kodeGagal = 401))
        }
        assertEquals("token_ditolak", e.jenis)
    }
}

/** Kunci urut tugas yang tidak bergantung pada urutan kunci JSON. */
private fun kunciTugas(t: JsonElement): String =
    t.jsonObject.getValue("course_external_id").toString() + "/" + t.jsonObject.getValue("external_id").toString()
