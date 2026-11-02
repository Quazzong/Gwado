package io.github.quazzong.gwado.fetch

import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Meniru test_minta_* di serper-app tests/test_fetch.py: HTTP palsu, tanpa jaringan. */
class MintaTest {
    private fun json(s: String) = Json.parseToJsonElement(s)
    private val diam: (String) -> Unit = {}

    @Test
    fun ulangSaatJaringanPutus() {
        val jeda = mutableListOf<Int>()
        val catatan = mutableListOf<String>()
        var coba = 0
        val hasil = minta(
            "https://x",
            buka = { _, _, _ ->
                coba++
                if (coba < 3) throw IOException("putus")
                JawabanHttp(200, """{"ok": 1}""")
            },
            tidur = { jeda.add(it) },
            catat = { catatan.add(it) },
        )
        assertEquals(json("""{"ok": 1}"""), hasil)
        assertEquals(listOf(10, 20), jeda)
        assertEquals(
            listOf(
                "percobaan 1 gagal (IOException), ulang dalam 10 s",
                "percobaan 2 gagal (IOException), ulang dalam 20 s",
            ),
            catatan,
        )
    }

    @Test
    fun menyerahSetelahTigaKali() {
        var coba = 0
        val e = assertThrows(FetchError::class.java) {
            minta("https://x", buka = { _, _, _ -> coba++; throw SocketTimeoutException() }, tidur = {}, catat = diam)
        }
        assertEquals("jaringan", e.jenis)
        assertTrue(e.pesan, "3 percobaan" in e.pesan)
        assertEquals(3, coba)
    }

    @Test
    fun http4xxTidakDiulang() {
        var coba = 0
        val e = assertThrows(HttpGagal::class.java) {
            minta(
                "https://x",
                buka = { _, _, _ -> coba++; JawabanHttp(403, """{"error": "forbidden"}""") },
                tidur = {},
                catat = diam,
            )
        }
        assertEquals(403, e.kode)
        assertEquals(json("""{"error": "forbidden"}"""), e.isi)
        assertEquals(1, coba)
    }

    @Test
    fun http5xxDiulangLaluSukses() {
        val kode = ArrayDeque(listOf(503, 200))
        val jeda = mutableListOf<Int>()
        val hasil = minta(
            "https://x",
            buka = { _, _, _ -> JawabanHttp(kode.removeFirst(), "[1]") },
            tidur = { jeda.add(it) },
            catat = diam,
        )
        assertEquals(json("[1]"), hasil)
        assertEquals(listOf(10), jeda)
    }

    @Test
    fun http5xxTerusJadiHttpGagalJaringan() {
        var coba = 0
        val jeda = mutableListOf<Int>()
        val e = assertThrows(HttpGagal::class.java) {
            minta(
                "https://x",
                buka = { _, _, _ -> coba++; JawabanHttp(503, "bukan json") },
                tidur = { jeda.add(it) },
                catat = diam,
            )
        }
        assertEquals(503, e.kode)
        assertNull(e.isi)
        assertEquals(3, coba)
        assertEquals(listOf(10, 20), jeda)
        assertEquals("jaringan", umum(e).jenis)
    }

    @Test
    fun jawabanBukanJson() {
        for (badan in listOf("<html>", "<html><body>Maintenance</body></html>", "", "abc")) {
            val e = assertThrows(FetchError::class.java) {
                minta("https://x", buka = { _, _, _ -> JawabanHttp(200, badan) }, tidur = {}, catat = diam)
            }
            assertEquals(badan, "lain", e.jenis)
            assertEquals(badan, "jawaban bukan JSON", e.pesan)
        }
    }
}
