package io.github.quazzong.gwado.segar

import io.github.quazzong.gwado.sumber.Kontrak
import io.github.quazzong.gwado.waktu.dariDb
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegarTest {
    @Test
    fun jamSingkatSamaDenganGolden() {
        val kasus = Kontrak.muat("golden/segar/jam.json").getValue("kasus").jsonArray.map { it.jsonObject }
        assertTrue(kasus.isNotEmpty())
        for (k in kasus) {
            val w = k.getValue("waktu").jsonPrimitive.content
            val n = k.getValue("sekarang").jsonPrimitive.content
            assertEquals("$w sekarang $n", k.getValue("label").jsonPrimitive.content, jamSingkat(dariDb(w), dariDb(n)))
        }
    }
}
