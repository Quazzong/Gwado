package io.github.quazzong.gwado.penjadwal

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JadwalTest {
    /** Waktu WIB ditulis biasa, diubah ke Instant. */
    private fun wib(teks: String): Instant = Instant.parse("${teks}+07:00")

    @Test
    fun jadwalTerakhirTepatPadaJamnyaSudahDihitung() {
        assertEquals(wib("2026-11-06T10:00:00"), jadwalTerakhir(wib("2026-11-06T10:00:00")))
        assertEquals(wib("2026-11-06T05:00:00"), jadwalTerakhir(wib("2026-11-06T09:59:59")))
        assertEquals(wib("2026-11-06T20:00:00"), jadwalTerakhir(wib("2026-11-06T23:30:00")))
    }

    @Test
    fun sebelumJamLimaMemakaiJadwalKemarinMalam() {
        assertEquals(wib("2026-11-05T20:00:00"), jadwalTerakhir(wib("2026-11-06T04:59:00")))
        assertEquals(wib("2026-12-31T20:00:00"), jadwalTerakhir(wib("2027-01-01T00:10:00")))
    }

    @Test
    fun perluAmbilKalauBelumAdaRunSejakJadwalTerakhir() {
        val sekarang = wib("2026-11-06T12:30:00")
        assertTrue(perluAmbil(null, sekarang))
        assertTrue(perluAmbil(wib("2026-11-06T09:59:00"), sekarang))
        assertFalse(perluAmbil(wib("2026-11-06T10:00:00"), sekarang))
        assertFalse(perluAmbil(wib("2026-11-06T10:05:00"), sekarang))
    }

    @Test
    fun runSetelahJadwalTidakDiulangSampaiJadwalBerikutnya() {
        val run = wib("2026-11-06T15:03:00")
        assertFalse(perluAmbil(run, wib("2026-11-06T19:59:00")))
        assertTrue(perluAmbil(run, wib("2026-11-06T20:00:00")))
    }
}
