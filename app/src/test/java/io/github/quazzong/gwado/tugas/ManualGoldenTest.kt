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

class ManualGoldenTest {
    private val golden = Kontrak.muat("golden/manual/manual.json")

    private fun teks(e: JsonElement?): String? = if (e == null || e is JsonNull) null else e.jsonPrimitive.content

    private fun nilai(v: Any?, norm: (String) -> String): JsonElement = when (v) {
        null -> JsonNull
        is String -> JsonPrimitive(norm(v))
        is Long -> JsonPrimitive(v)
        is Int -> JsonPrimitive(v)
        is Double -> JsonPrimitive(v)
        else -> JsonPrimitive(v.toString())
    }

    private fun Db.dump(sql: String, norm: (String) -> String): List<JsonObject> =
        semua(sql).map { r -> JsonObject(r.mapValues { nilai(it.value, norm) }) }

    private fun BarisTugas.keJson(norm: (String) -> String) = JsonObject(
        mapOf(
            "id" to nilai(id, norm), "jenis" to nilai(jenis, norm), "judul" to nilai(judul, norm),
            "deadline" to nilai(deadline, norm), "asal" to nilai(asal, norm),
            "status_situs" to nilai(statusSitus, norm), "status_kumpul_situs" to nilai(statusKumpulSitus, norm),
            "matkul" to nilai(matkul, norm), "sumber" to nilai(sumber, norm), "status" to nilai(status, norm),
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
                val ids = linkedMapOf<String, String>()
                val isiId = { v: String -> ids[v] ?: v }
                val hasil = mutableListOf<JsonElement>()
                for (l in s.getValue("langkah").jsonArray.map { it.jsonObject }) {
                    if ("sql" in l) {
                        db.exec(l.getValue("sql").jsonPrimitive.content)
                        continue
                    }
                    if ("isi" in l) {
                        db.proses(keIsi(l.getValue("isi").jsonObject))
                        continue
                    }
                    val sekarang = teks(l["waktu"])?.let(::dariDb)
                    try {
                        when {
                            "tambah" in l -> {
                                val a = l.getValue("tambah").jsonObject
                                val id = db.tambahManual(
                                    teks(a["judul"]), teks(a["deadline"])?.let(::dariDb), teks(a["deskripsi"]),
                                    sekarang!!, teks(a["kode_mk"]),
                                )
                                val kunci = "$" + (ids.size + 1)
                                ids[kunci] = id
                                hasil += JsonObject(mapOf("id" to JsonPrimitive(kunci)))
                            }
                            "ubah" in l -> {
                                val p = l.getValue("perubahan").jsonObject.mapValues { (k, v) ->
                                    val t = teks(v)
                                    if (k == "deadline" && t != null) dariDb(t) else t
                                }
                                val berubah = db.ubahManual(isiId(l.getValue("ubah").jsonPrimitive.content), p, sekarang!!)
                                hasil += JsonObject(mapOf("berubah" to JsonArray(berubah.map { JsonPrimitive(it) })))
                            }
                            "hapus" in l -> {
                                val judul = db.hapusManual(isiId(l.getValue("hapus").jsonPrimitive.content))
                                hasil += JsonObject(mapOf("judul" to JsonPrimitive(judul)))
                            }
                            "tandai" in l -> {
                                val ok = db.tandai(
                                    isiId(l.getValue("tandai").jsonPrimitive.content),
                                    l.getValue("selesai").jsonPrimitive.boolean,
                                    sekarang!!,
                                )
                                hasil += JsonObject(mapOf("tandai" to JsonPrimitive(ok)))
                            }
                        }
                    } catch (e: TugasGalat) {
                        hasil += JsonObject(mapOf("galat" to JsonPrimitive(true), "kolom" to JsonPrimitive(e.kolom)))
                    }
                }
                val balik = ids.entries.associate { (k, v) -> v to k }
                val norm = { v: String -> balik[v] ?: v }
                assertEquals("$nama: hasil", s.getValue("hasil"), JsonArray(hasil))
                assertEquals(
                    "$nama: tasks", s.getValue("tasks"),
                    JsonArray(
                        db.dump(
                            "SELECT id, jenis, judul, deskripsi, deadline, first_seen, asal, kode_mk_manual, " +
                                "duplikat_dari FROM tasks WHERE asal = 'manual'", norm,
                        ).sortedBy { it.getValue("id").jsonPrimitive.content },
                    ),
                )
                assertEquals(
                    "$nama: task_changes", s.getValue("task_changes"),
                    JsonArray(
                        db.dump(
                            "SELECT task_id, field, nilai_lama, nilai_baru, waktu, oleh FROM task_changes " +
                                "WHERE oleh = 'aku' ORDER BY id", norm,
                        ),
                    ),
                )
                assertEquals(
                    "$nama: task_user_state", s.getValue("task_user_state"),
                    JsonArray(
                        db.dump("SELECT task_id, status, done_at, catatan FROM task_user_state", norm)
                            .sortedBy { it.getValue("task_id").jsonPrimitive.content },
                    ),
                )
                assertEquals(
                    "$nama: daftar_semua", s.getValue("daftar_semua"),
                    JsonArray(db.daftarTugas(semua = true).map { it.keJson(norm) }),
                )
            }
        }
    }
}
