package io.github.quazzong.gwado.fetch

import java.time.Instant
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AkhirJendelaTest {
    /** "2026-11-04 23:30:00" dibaca sebagai jam WIB. */
    private fun wib(teks: String): Instant = OffsetDateTime.parse(teks.replace(" ", "T") + "+07:00").toInstant()

    @Test
    fun sebelumTengahMalamSelesaiBesokPagi() {
        // Rabu 23.30 WIB → Kamis 02.00 WIB
        assertEquals(wib("2026-11-05 02:00:00"), akhirJendela("brone", wib("2026-11-04 23:30:00")))
    }

    @Test
    fun setelahTengahMalamSelesaiHariItu() {
        assertEquals(wib("2026-11-05 02:00:00"), akhirJendela("brone", wib("2026-11-05 01:59:59")))
    }

    @Test
    fun diLuarJendelaAtauSumberLain() {
        assertNull(akhirJendela("brone", wib("2026-11-05 02:00:00")))
        assertNull(akhirJendela("brone", wib("2026-11-04 22:59:59")))
        assertNull(akhirJendela("eling", wib("2026-11-04 23:30:00")))
    }
}
