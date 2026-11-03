package io.github.quazzong.gwado.waktu

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class WaktuTest {
    private fun golden(nama: String): List<JsonObject> {
        val teks = javaClass.getResource("/kontrak/golden/waktu/$nama.json")!!.readText()
        return Json.parseToJsonElement(teks).jsonArray.map { it.jsonObject }
    }

    private fun JsonObject.teks(kunci: String) = getValue(kunci).jsonPrimitive.content

    @Test
    fun sisaChipSamaDenganGolden() {
        val kasus = golden("sisa")
        assertTrue(kasus.isNotEmpty())
        val salah = kasus.mapNotNull { k ->
            val hasil = sisaChip(dariDb(k.teks("deadline")), dariDb(k.teks("sekarang")))
            val harap = Chip(k.teks("label"), Makna.valueOf(k.teks("makna").uppercase(Locale.ROOT)))
            if (hasil == harap) null else "${k.teks("sekarang")} -> ${k.teks("deadline")}: $hasil, harusnya $harap"
        }
        assertEquals(emptyList<String>(), salah)
    }

    @Test
    fun formatTanggalSamaDenganGolden() {
        val kasus = golden("tanggal")
        assertTrue(kasus.isNotEmpty())
        val salah = kasus.mapNotNull { k ->
            val t = dariDb(k.teks("waktu"))
            val hasil = formatTanggal(t) to formatTanggal(t, jam = false)
            val harap = k.teks("jam") to k.teks("tanpa_jam")
            if (hasil == harap) null else "${k.teks("waktu")}: $hasil, harusnya $harap"
        }
        assertEquals(emptyList<String>(), salah)
    }
}
