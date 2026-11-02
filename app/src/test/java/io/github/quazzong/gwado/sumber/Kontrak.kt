package io.github.quazzong.gwado.sumber

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals

/** Pembantu test: membaca kontrak dari src/test/resources/kontrak. */
object Kontrak {
    fun file(path: String): File =
        File(requireNotNull(javaClass.getResource("/kontrak/$path")) { "tidak ada: kontrak/$path" }.toURI())

    fun muat(path: String): JsonObject = Json.parseToJsonElement(file(path).readText()).jsonObject

    /** HasilSumber → JSON dengan nama kunci yang sama dengan golden (format inbox SERPER). */
    fun keJson(h: HasilSumber): JsonObject = buildJsonObject {
        putJsonArray("courses") {
            h.kelas.forEach { k ->
                addJsonObject {
                    put("external_id", k.externalId)
                    put("nama", k.nama)
                    put("shortname", k.shortname)
                    put("kode_mk", k.kodeMk)
                    put("kategori", k.kategori)
                    put("mulai", k.mulai)
                    put("selesai", k.selesai)
                    put("dibuat", k.dibuat)
                }
            }
        }
        putJsonArray("tasks") {
            h.tugas.forEach { t ->
                addJsonObject {
                    put("external_id", t.externalId)
                    put("course_external_id", t.courseExternalId)
                    put("jenis", t.jenis)
                    put("link_id", t.linkId)
                    put("judul", t.judul)
                    put("deskripsi", t.deskripsi)
                    put("deadline", t.deadline)
                    put("url", t.url)
                    put("status_kumpul_situs", t.statusKumpulSitus)
                }
            }
        }
    }

    /** Bandingkan per kelas dan per tugas, supaya pesan gagalnya menunjuk item yang salah. */
    fun cocokGolden(nama: String, hasil: HasilSumber) {
        val harap = muat("golden/parser/$nama.json")
        val dapat = keJson(hasil)
        for (bagian in listOf("courses", "tasks")) {
            val h = harap.getValue(bagian).jsonArray
            val d = dapat.getValue(bagian).jsonArray
            assertEquals("$nama: jumlah $bagian", h.size, d.size)
            for (i in h.indices) assertEquals("$nama: $bagian[$i]", h[i], d[i])
        }
    }
}
