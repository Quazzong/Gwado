package io.github.quazzong.gwado.fetch

import io.github.quazzong.gwado.sumber.HasilSumber
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Aturan di luar golden penahan: manual hanya kena jeda pendek + jendela hindari. */
class PenahanManualTest {
    private val sukses = Instant.parse("2026-10-21T03:00:05Z")             // 10.00.05 WIB
    private val statusSukses = iso(sukses)

    private class CatatanPalsu(awal: String?) : CatatanSukses {
        var isi: String? = awal
        override fun baca(sumber: String) = isi
        override fun tulis(sumber: String, waktuTeks: String) {
            isi = waktuTeks
        }
    }

    private fun jalankan(manual: Boolean, sekarang: Instant, sumber: String = "brone"): Pair<HasilJob, Boolean> {
        var dihubungi = false
        val h = job(sumber, CatatanPalsu(statusSukses), { sekarang }, manual) {
            dihubungi = true
            HasilSumber(emptyList(), emptyList())
        }
        return h to dihubungi
    }

    @Test
    fun manualLewatJedaBolehWalauMasihCooldownOtomatis() {
        val sekarang = sukses.plus(JEDA_MANUAL)                          // tepat 1 menit kemudian
        val (h, dihubungi) = jalankan(manual = true, sekarang)
        assertTrue(h is HasilJob.Selesai)
        assertTrue(dihubungi)
        val (hOto, dihubungiOto) = jalankan(manual = false, sekarang)
        assertEquals(SebabTertahan.COOLDOWN, (hOto as HasilJob.Tertahan).sebab)
        assertEquals(false, dihubungiOto)
    }

    @Test
    fun manualDalamJedaTertahanDenganBolehLagiTepat() {
        val (h, dihubungi) = jalankan(manual = true, sukses.plusSeconds(30))
        val t = h as HasilJob.Tertahan
        assertEquals(SebabTertahan.COOLDOWN, t.sebab)
        assertEquals(sukses.plus(JEDA_MANUAL), t.bolehLagi)
        assertEquals(false, dihubungi)
    }

    @Test
    fun otomatisBolehLagiSetelahCooldownPenuh() {
        val t = jalankan(manual = false, sukses.plusSeconds(60)).first as HasilJob.Tertahan
        assertEquals(sukses.plus(COOLDOWN), t.bolehLagi)
    }

    @Test
    fun manualTetapKenaJendelaHindari() {
        val tengahMalamWib = Instant.parse("2026-10-21T17:30:00Z")      // 00.30 WIB
        val (h, dihubungi) = jalankan(manual = true, tengahMalamWib)
        val t = h as HasilJob.Tertahan
        assertEquals(SebabTertahan.JENDELA_HINDARI, t.sebab)
        assertNull(t.bolehLagi)
        assertEquals(false, dihubungi)
    }

    @Test
    fun manualSuksesTetapDicatatUntukPenjadwal() {
        val catatan = CatatanPalsu(null)
        val sekarang = Instant.parse("2026-10-21T05:00:00Z")
        job("eling", catatan, { sekarang }, manual = true) { HasilSumber(emptyList(), emptyList()) }
        assertEquals(iso(sekarang), catatan.isi)
        assertTrue(tertahan("eling", catatan.isi, sekarang.plusSeconds(600), manual = false) != null)
    }
}
