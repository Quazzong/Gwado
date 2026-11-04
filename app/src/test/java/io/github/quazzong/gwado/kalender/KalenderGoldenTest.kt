package io.github.quazzong.gwado.kalender

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.sumber.Kontrak
import java.io.File
import java.time.LocalDate
import java.util.IdentityHashMap
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KalenderGoldenTest {
    private fun berkas(folder: String): List<String> =
        File(javaClass.classLoader!!.getResource("kontrak/golden/$folder")!!.toURI())
            .listFiles()!!.map { it.name }.filter { it.endsWith(".json") }.sorted()

    private fun JsonElement.keNilai(): Any? {
        if (this is JsonNull) return null
        val p = jsonPrimitive
        if (p.isString) return p.content
        return p.longOrNull ?: p.doubleOrNull ?: p.booleanOrNull?.let { if (it) 1L else 0L }
    }

    private fun teks(o: JsonObject, k: String): String? = o[k]?.let { if (it is JsonNull) null else it.jsonPrimitive.content }

    private fun Kejadian.keJson() = JsonObject(
        mapOf(
            "sumber" to JsonPrimitive(sumber), "ref" to JsonPrimitive(ref), "tanggal_asli" to JsonPrimitive(tanggalAsli),
            "status" to JsonPrimitive(status), "seharian" to JsonPrimitive(seharian), "mulai" to JsonPrimitive(mulai),
            "selesai" to JsonPrimitive(selesai), "judul" to JsonPrimitive(judul), "lokasi" to JsonPrimitive(lokasi),
            "kalender" to JsonPrimitive(kalender), "warna" to JsonPrimitive(warna), "peran" to JsonPrimitive(peran),
            "kode_mk" to JsonPrimitive(kodeMk), "pindah_ke" to JsonPrimitive(pindahKe),
        ),
    )

    @Test
    fun kejadianSamaDenganGolden() {
        val daftar = berkas("kalender")
        assertTrue(daftar.size >= 3)
        for (f in daftar) {
            val kasus = Kontrak.muat("golden/kalender/$f")
            Db.buka(":memory:").use { db ->
                db.migrasi()
                for (bagian in kasus.getValue("isi").jsonArray.map { it.jsonObject }) {
                    val tabel = bagian.getValue("tabel").jsonPrimitive.content
                    for (baris in bagian.getValue("baris").jsonArray.map { it.jsonObject }) {
                        db.exec(
                            "INSERT INTO $tabel (${baris.keys.joinToString()}) VALUES (${baris.keys.joinToString { "?" }})",
                            *baris.values.map { it.keNilai() }.toTypedArray(),
                        )
                    }
                }
                val hasil = db.kejadian(
                    LocalDate.parse(kasus.getValue("dari").jsonPrimitive.content),
                    LocalDate.parse(kasus.getValue("sampai").jsonPrimitive.content),
                )
                assertEquals(f, kasus.getValue("hasil"), JsonArray(hasil.map { it.keJson() }))
            }
        }
    }

    @Test
    fun perHariSamaDenganGolden() {
        val daftar = berkas("per_hari")
        assertTrue(daftar.isNotEmpty())
        for (f in daftar) {
            val kasus = Kontrak.muat("golden/per_hari/$f")
            // Python menyalin kolom masukan apa adanya: keluaran = objek masukan asli + kolom tambahan.
            val asli = IdentityHashMap<Kejadian, JsonObject>()
            val masuk = kasus.getValue("kejadian").jsonArray.map { e ->
                val o = e.jsonObject
                Kejadian(
                    sumber = teks(o, "sumber")!!, ref = teks(o, "ref") ?: "", tanggalAsli = teks(o, "tanggal_asli") ?: "",
                    status = teks(o, "status") ?: "normal", seharian = o["seharian"]?.jsonPrimitive?.boolean ?: false,
                    mulai = teks(o, "mulai")!!, selesai = teks(o, "selesai")!!, judul = teks(o, "judul") ?: "",
                    lokasi = teks(o, "lokasi"), kalender = teks(o, "kalender"), warna = o["warna"]?.jsonPrimitive?.intOrNull,
                    peran = teks(o, "peran"), kodeMk = teks(o, "kode_mk"), pindahKe = teks(o, "pindah_ke"),
                ).also { asli[it] = o }
            }
            fun tambah(k: Kejadian, lain: Map<String, JsonElement>) = JsonObject(asli.getValue(k) + lain)
            val hasil = perHari(
                masuk,
                LocalDate.parse(kasus.getValue("dari").jsonPrimitive.content),
                LocalDate.parse(kasus.getValue("sampai").jsonPrimitive.content),
            ).map { h ->
                JsonObject(
                    mapOf(
                        "tanggal" to JsonPrimitive(h.tanggal.toString()),
                        "seharian" to JsonArray(h.seharian.map { tambah(it, emptyMap()) }),
                        "deadline" to JsonArray(h.deadline.map { tambah(it.kejadian, mapOf("menit" to JsonPrimitive(it.menit))) }),
                        "berjam" to JsonArray(
                            h.berjam.map {
                                tambah(
                                    it.kejadian,
                                    mapOf(
                                        "menit_mulai" to JsonPrimitive(it.menitMulai), "menit_selesai" to JsonPrimitive(it.menitSelesai),
                                        "dari_kemarin" to JsonPrimitive(it.dariKemarin), "ke_besok" to JsonPrimitive(it.keBesok),
                                        "lajur" to JsonPrimitive(it.lajur), "jumlah_lajur" to JsonPrimitive(it.jumlahLajur),
                                    ),
                                )
                            },
                        ),
                    ),
                )
            }
            assertEquals(f, kasus.getValue("hasil"), JsonArray(hasil))
        }
    }

    @Test
    fun rentangDitolak() {
        assertThrows(KalenderGalat::class.java) { perHari(emptyList(), LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-01")) }
        assertThrows(KalenderGalat::class.java) { perHari(emptyList(), LocalDate.parse("2026-01-01"), LocalDate.parse("2027-01-02")) }
        assertEquals(366, perHari(emptyList(), LocalDate.parse("2026-01-01"), LocalDate.parse("2027-01-01")).size)
    }
}
