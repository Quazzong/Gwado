package io.github.quazzong.gwado.fetch

import io.github.quazzong.gwado.sumber.HasilSumber
import io.github.quazzong.gwado.sumber.Kontrak
import java.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test

/** Kontrak golden/fetch/penahan.json. */
class PenahanTest {
    private val g = Kontrak.muat("golden/fetch/penahan.json")

    private fun JsonObject.s(k: String): String? = (this[k] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.t(k: String): Instant = Instant.parse(getValue(k).jsonPrimitive.content)
    private fun kasus(bagian: String) = g.getValue(bagian).jsonArray.map { it.jsonObject }

    private class CatatanPalsu : CatatanSukses {
        val isi = mutableMapOf<String, String>()
        override fun baca(sumber: String) = isi[sumber]
        override fun tulis(sumber: String, waktuTeks: String) {
            isi[sumber] = waktuTeks
        }
    }

    @Test
    fun konstanta() {
        val k = g.getValue("konstanta").jsonObject
        assertEquals(k.getValue("cooldown_detik").jsonPrimitive.long, COOLDOWN.seconds)
        val harap = k.getValue("jendela_hindari_wib").jsonObject
            .mapValues { (_, v) -> v.jsonArray.map { it.jsonPrimitive.int } }
        assertEquals(harap, JENDELA_HINDARI.mapValues { (_, v) -> listOf(v.first, v.second) })
    }

    @Test
    fun jendelaHindari() {
        for (c in kasus("jendela")) {
            assertEquals(
                "${c.s("sumber")} ${c.s("wib")} WIB",
                c.getValue("hasil").jsonPrimitive.boolean,
                diJendelaHindari(c.s("sumber")!!, c.t("sekarang")),
            )
        }
    }

    @Test
    fun cooldown() {
        kasus("cooldown").forEachIndexed { i, c ->
            assertEquals(
                "cooldown[$i] status=${c.s("status")}",
                c.s("hasil"),
                masihCooldown(c.s("status"), c.t("sekarang"))?.let(::iso),
            )
        }
    }

    @Test
    fun jobUrutanPenahanDanCatatan() {
        kasus("job").forEachIndexed { i, c ->
            val sumber = c.s("sumber")!!
            val sekarang = c.t("sekarang")
            val label = "job[$i] $sumber ${c.s("sekarang")} ${c.s("hasil_run")}"
            val catatan = CatatanPalsu()
            c.s("status_sebelum")?.let { catatan.isi[sumber] = it }
            var dihubungi = false

            val hasil = try {
                job(sumber, catatan, { sekarang }) {
                    dihubungi = true
                    if (c.s("hasil_run") == "success") HasilSumber(emptyList(), emptyList())
                    else throw FetchError("jaringan", "karangan")
                }
            } catch (e: FetchError) {
                null
            }

            assertEquals(label, c.s("tertahan"), (hasil as? HasilJob.Tertahan)?.sebab?.kode)
            assertEquals(label, c.getValue("dihubungi").jsonPrimitive.boolean, dihubungi)
            assertEquals(label, c.s("status_sesudah")?.trim(), catatan.isi[sumber]?.trim())
        }
    }
}
