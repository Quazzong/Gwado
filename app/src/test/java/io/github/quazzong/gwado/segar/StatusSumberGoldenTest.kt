package io.github.quazzong.gwado.segar

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.sumber.Kontrak
import io.github.quazzong.gwado.waktu.dariDb
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusSumberGoldenTest {
    private val golden = Kontrak.muat("golden/segar/status.json")

    private fun SumberTerakhir.keJson() = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id), "nama" to JsonPrimitive(nama), "status" to JsonPrimitive(status),
            "selesai" to JsonPrimitive(selesai), "error_singkat" to JsonPrimitive(errorSingkat),
            "sukses_terakhir" to JsonPrimitive(suksesTerakhir),
        ),
    )

    private fun StatusSumber.keJson() = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id), "nama" to JsonPrimitive(nama), "warna" to JsonPrimitive(tingkat.kode),
            "label" to JsonPrimitive(label), "pesan" to JsonPrimitive(pesan),
        ),
    )

    private fun Banner?.keJson(): JsonElement = this?.let { b ->
        JsonObject(mapOf("warna" to JsonPrimitive(b.tingkat.kode), "pesan" to JsonArray(b.pesan.map { JsonPrimitive(it) })))
    } ?: JsonNull

    @Test
    fun semuaSkenarioSamaDenganGolden() {
        val skenario = golden.getValue("skenario").jsonArray.map { it.jsonObject }
        assertTrue(skenario.isNotEmpty())
        for (s in skenario) {
            val nama = s.getValue("nama").jsonPrimitive.content
            val sekarang = dariDb(s.getValue("sekarang").jsonPrimitive.content)
            Db.buka(":memory:").use { db ->
                db.migrasi()
                for (l in s.getValue("langkah").jsonArray.map { it.jsonObject }) db.exec(l.getValue("sql").jsonPrimitive.content)
                val baris = db.sumberTerakhir()
                assertEquals("$nama: sumber_terakhir", s.getValue("sumber_terakhir").jsonArray, baris.map { it.keJson() })
                val status = statusSumber(baris, sekarang)
                assertEquals("$nama: status", s.getValue("status").jsonArray, status.map { it.keJson() })
                assertEquals("$nama: banner", s.getValue("banner"), banner(status).keJson())
            }
        }
    }
}
