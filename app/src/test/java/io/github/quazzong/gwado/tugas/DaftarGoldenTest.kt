package io.github.quazzong.gwado.tugas

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.ingest.keIsi
import io.github.quazzong.gwado.ingest.proses
import io.github.quazzong.gwado.sumber.Kontrak
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DaftarGoldenTest {
    private val golden = Kontrak.muat("golden/daftar/daftar.json")

    private fun BarisTugas.keJson() = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id), "jenis" to JsonPrimitive(jenis), "judul" to JsonPrimitive(judul),
            "deadline" to JsonPrimitive(deadline), "asal" to JsonPrimitive(asal),
            "status_situs" to JsonPrimitive(statusSitus),
            "status_kumpul_situs" to JsonPrimitive(statusKumpulSitus),
            "matkul" to JsonPrimitive(matkul), "sumber" to JsonPrimitive(sumber),
            "status" to JsonPrimitive(status),
        ),
    )

    @Test
    fun semuaSkenarioSamaDenganGolden() {
        val skenario = golden.getValue("skenario").jsonArray.map { it.jsonObject }
        assertTrue(skenario.isNotEmpty())
        for (s in skenario) {
            val nama = s.getValue("nama").jsonPrimitive.content
            Db.buka(":memory:").use { db ->
                db.migrasi()
                for (l in s.getValue("langkah").jsonArray.map { it.jsonObject }) {
                    val sql = l["sql"]
                    if (sql != null) db.exec(sql.jsonPrimitive.content) else db.proses(keIsi(l.getValue("isi").jsonObject))
                }
                assertEquals("$nama: daftar", s.getValue("daftar").jsonArray, db.daftarTugas().map { it.keJson() })
                assertEquals("$nama: semua", s.getValue("daftar_semua").jsonArray,
                    db.daftarTugas(semua = true).map { it.keJson() })
            }
        }
    }
}
