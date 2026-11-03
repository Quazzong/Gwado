package io.github.quazzong.gwado.tugas

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.ingest.keIsi
import io.github.quazzong.gwado.ingest.proses
import io.github.quazzong.gwado.sumber.Kontrak
import io.github.quazzong.gwado.waktu.dariDb
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailGoldenTest {
    private val golden = Kontrak.muat("golden/tugas/tugas.json")

    private fun DetailTugas.keJson() = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id), "jenis" to JsonPrimitive(jenis), "judul" to JsonPrimitive(judul),
            "deskripsi" to JsonPrimitive(deskripsi), "deadline" to JsonPrimitive(deadline),
            "url" to JsonPrimitive(url), "asal" to JsonPrimitive(asal),
            "status_situs" to JsonPrimitive(statusSitus),
            "status_kumpul_situs" to JsonPrimitive(statusKumpulSitus),
            "first_seen" to JsonPrimitive(firstSeen), "kode_mk_manual" to JsonPrimitive(kodeMkManual),
            "matkul" to JsonPrimitive(matkul), "kode_mk" to JsonPrimitive(kodeMk),
            "sumber" to JsonPrimitive(sumber), "status" to JsonPrimitive(status),
            "done_at" to JsonPrimitive(doneAt), "catatan" to JsonPrimitive(catatan),
        ),
    )

    private fun BarisRiwayat.keJson() = JsonObject(
        mapOf(
            "waktu" to JsonPrimitive(waktu), "label" to JsonPrimitive(label),
            "dari" to JsonPrimitive(dari), "ke" to JsonPrimitive(ke),
            "oleh" to JsonPrimitive(oleh), "kuning" to JsonPrimitive(kuning),
        ),
    )

    private fun JsonElement?.namaMk(): Map<String, String>? =
        if (this == null || this is JsonNull) null
        else jsonObject.mapValues { it.value.jsonPrimitive.content }

    @Test
    fun semuaSkenarioSamaDenganGolden() {
        val skenario = golden.getValue("skenario").jsonArray.map { it.jsonObject }
        assertTrue(skenario.isNotEmpty())
        for (s in skenario) {
            val nama = s.getValue("nama").jsonPrimitive.content
            Db.buka(":memory:").use { db ->
                db.migrasi()
                val hasilTandai = mutableListOf<Boolean>()
                for (l in s.getValue("langkah").jsonArray.map { it.jsonObject }) {
                    when {
                        "sql" in l -> db.exec(l.getValue("sql").jsonPrimitive.content)
                        "tandai" in l -> hasilTandai += db.tandai(
                            l.getValue("tandai").jsonPrimitive.content,
                            l.getValue("selesai").jsonPrimitive.boolean,
                            dariDb(l.getValue("waktu").jsonPrimitive.content),
                        )
                        else -> db.proses(keIsi(l.getValue("isi").jsonObject))
                    }
                }
                assertEquals("$nama: hasil tandai", s.getValue("hasil_tandai"),
                    JsonArray(hasilTandai.map { JsonPrimitive(it) }))
                val periksa = s.getValue("periksa").jsonArray.map { it.jsonObject }
                assertTrue(periksa.isNotEmpty())
                for (p in periksa) {
                    val id = p.getValue("id").jsonPrimitive.content
                    val namaMk = p["nama_mk"].namaMk()
                    assertEquals("$nama $id: tugas", p.getValue("tugas"), db.ambilTugas(id)?.keJson() ?: JsonNull)
                    assertEquals("$nama $id: riwayat", p.getValue("riwayat"),
                        db.riwayatTampil(id, namaMk)?.let { r -> JsonArray(r.map { it.keJson() }) } ?: JsonNull)
                }
            }
        }
    }

    @Test
    fun linkSamaDenganGolden() {
        val link = golden.getValue("link").jsonObject
        val domain = link.getValue("domain").jsonArray.map { it.jsonPrimitive.content }
        val kasus = link.getValue("kasus").jsonArray.map { it.jsonObject }
        assertTrue(kasus.isNotEmpty())
        for (k in kasus) {
            val url = k.getValue("url").let { if (it is JsonNull) null else it.jsonPrimitive.content }
            assertEquals("link $url", k.getValue("hasil"), JsonPrimitive(linkSumber(url, domain)))
        }
    }
}
