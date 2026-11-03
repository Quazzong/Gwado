package io.github.quazzong.gwado.ingest

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.sumber.HasilSumber
import io.github.quazzong.gwado.sumber.Kelas
import io.github.quazzong.gwado.sumber.Kontrak
import io.github.quazzong.gwado.sumber.Tugas
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test

/** Query dump yang sama dengan buat_golden_ingest.py (kolom subset Gwado, tanpa id INTEGER). */
private val DUMP = mapOf(
    "courses" to "SELECT source_id, external_id, nama, shortname, kode_mk, kategori, mulai, selesai, " +
        "dibuat, diikuti, alasan_diikuti FROM courses ORDER BY source_id, external_id",
    "tasks" to "SELECT t.id, t.source_id, t.external_id, c.external_id AS course_external_id, t.jenis, " +
        "t.link_id, t.judul, t.deskripsi, t.deadline, t.url, t.status_kumpul_situs, t.first_seen, " +
        "t.last_seen, t.content_hash, t.status_situs, t.jumlah_absen, t.asal, t.duplikat_dari, " +
        "t.kode_mk_manual FROM tasks t LEFT JOIN courses c ON c.id = t.course_id ORDER BY t.id",
    "task_changes" to "SELECT task_id, field, nilai_lama, nilai_baru, waktu, oleh FROM task_changes " +
        "ORDER BY waktu, task_id, field",
    "source_runs" to "SELECT source_id, fetch_id, mulai, selesai, status, jumlah_tugas, error_singkat " +
        "FROM source_runs ORDER BY mulai, source_id, fetch_id",
)

private fun JsonObject.teks(k: String): String? = getValue(k).jsonPrimitive.contentOrNull

internal fun keIsi(o: JsonObject): Isi {
    val e = o["error"]?.takeUnless { it is JsonNull }?.jsonObject
    return Isi(
        fetchId = o.teks("fetch_id")!!,
        sumber = o.teks("sumber")!!,
        mulai = o.teks("mulai")!!,
        selesai = o.teks("selesai")!!,
        error = e?.let { ErrorFetch(it.teks("jenis")!!, it.teks("pesan")!!) },
        hasil = HasilSumber(
            o.getValue("courses").jsonArray.map { it.jsonObject }.map { c ->
                Kelas(
                    externalId = c.teks("external_id")!!, nama = c.teks("nama")!!,
                    shortname = c.teks("shortname"), kodeMk = c.teks("kode_mk"),
                    kategori = c.teks("kategori"), mulai = c.teks("mulai"),
                    selesai = c.teks("selesai"), dibuat = c.teks("dibuat"),
                )
            },
            o.getValue("tasks").jsonArray.map { it.jsonObject }.map { t ->
                Tugas(
                    externalId = t.teks("external_id")!!, courseExternalId = t.teks("course_external_id")!!,
                    jenis = t.teks("jenis")!!, linkId = t.teks("link_id"), judul = t.teks("judul")!!,
                    deskripsi = t.teks("deskripsi")!!, deadline = t.teks("deadline"),
                    url = t.teks("url"), statusKumpulSitus = t.teks("status_kumpul_situs"),
                )
            },
        ),
    )
}

private fun keJson(v: Any?): JsonElement = when (v) {
    null -> JsonNull
    is String -> JsonPrimitive(v)
    is Long -> JsonPrimitive(v)
    else -> error("tipe tak terduga dari database: $v")
}

/** Bandingkan per baris, supaya pesan gagal menunjuk skenario, langkah, tabel, dan barisnya. */
private fun cocokDb(tempat: String, harap: JsonObject, db: Db) {
    for ((tabel, sql) in DUMP) {
        val h = harap.getValue(tabel).jsonArray
        val d = db.semua(sql).map { baris -> JsonObject(baris.mapValues { keJson(it.value) }) }
        assertEquals("$tempat: jumlah $tabel", h.size, d.size)
        for (i in h.indices) assertEquals("$tempat: $tabel[$i]", h[i], d[i])
    }
}

class IngestGoldenTest {
    private val golden = Kontrak.muat("golden/ingest/ingest.json")

    @Test
    fun konstantaSamaDenganSerper() {
        val k = golden.getValue("konstanta").jsonObject
        assertEquals(k.getValue("ambang_hilang").jsonPrimitive.long, AMBANG_HILANG.toLong())
        val contoh = Tugas("assign:1", "10", "assign", null, "x", "", null, null, null)
        assertEquals(
            k.getValue("field_hash").jsonArray.map { it.jsonPrimitive.content }.sorted(),
            fieldHash(contoh).keys.sorted(),
        )
    }

    @Test
    fun semuaSkenarioSamaDenganGolden() {
        for (s in golden.getValue("skenario").jsonArray.map { it.jsonObject }) {
            val nama = s.teks("nama")!!
            Db.buka(":memory:").use { db ->
                db.migrasi()
                s.getValue("langkah").jsonArray.map { it.jsonObject }.forEachIndexed { i, l ->
                    val sql = l["sql"]
                    if (sql != null) {
                        db.exec(sql.jsonPrimitive.content)
                        return@forEachIndexed
                    }
                    val tempat = "$nama langkah $i"
                    val hasil = db.proses(keIsi(l.getValue("isi").jsonObject))
                    val harap = l.getValue("hasil").jsonObject
                    assertEquals("$tempat: status", harap.teks("status"), hasil.status)
                    assertEquals("$tempat: pesan", harap.teks("pesan"), hasil.pesan)
                    cocokDb(tempat, l.getValue("db").jsonObject, db)
                }
            }
        }
    }
}
