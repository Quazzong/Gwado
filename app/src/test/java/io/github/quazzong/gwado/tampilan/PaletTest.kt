package io.github.quazzong.gwado.tampilan

import kotlin.math.hypot
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletTest {
    /** Selisih warna OKLab × 100 (≈ ΔE; di bawah ±2 tidak terbedakan mata). */
    private fun selisih(a: Int, b: Int): Double {
        val x = keLch(a)
        val y = keLch(b)
        val ax = x.c * kotlin.math.cos(Math.toRadians(x.h))
        val bx = x.c * kotlin.math.sin(Math.toRadians(x.h))
        val ay = y.c * kotlin.math.cos(Math.toRadians(y.h))
        val by = y.c * kotlin.math.sin(Math.toRadians(y.h))
        return hypot(x.l - y.l, hypot(ax - ay, bx - by)) * 100
    }

    private fun gagal(t: Token) = ujiKontras(t).filterNot { it.lolos }.map { "${it.uji.label} ${"%.2f".format(it.rasio)}" }

    @Test
    fun benihPermenLewatResepSamaDenganPaletPermen() {
        val (terang, gelap) = paletDariBenih(PERMEN_TERANG.getValue("aksen"))
        for ((hasil, asli) in listOf(terang to PERMEN_TERANG, gelap to PERMEN_GELAP)) {
            // Latar kartu lewat/mepet sengaja dibaurkan dengan latar tema.
            for (k in asli.keys - setOf("latarLewat", "latarMepet")) {
                val d = selisih(hasil.getValue(k), asli.getValue(k))
                assertTrue("$k selisih $d", d < 1.5)
            }
        }
    }

    @Test
    fun semuaContohLolosKontras() {
        for (c in CONTOH_WARNA) {
            val (terang, gelap) = paletDariBenih(c.benih)
            assertEquals("${c.label} terang", emptyList<String>(), gagal(terang))
            assertEquals("${c.label} gelap", emptyList<String>(), gagal(gelap))
            assertEquals("${c.label} aksen = benih persis", c.benih, terang.getValue("aksen"))
        }
        assertEquals(emptyList<String>(), gagal(PERMEN_TERANG))
        assertEquals(emptyList<String>(), gagal(PERMEN_GELAP))
    }

    @Test
    fun benihBebasAcakLolosKontras() {
        val acak = Random(20261110)
        repeat(600) {
            val benih = dariLch(0.4 + acak.nextDouble() * 0.57, acak.nextDouble() * 0.2, acak.nextDouble() * 360)
            val (terang, gelap) = paletDariBenih(benih)
            assertEquals("benih %06X terang".format(benih and 0xFFFFFF), emptyList<String>(), gagal(terang))
            assertEquals("benih %06X gelap".format(benih and 0xFFFFFF), emptyList<String>(), gagal(gelap))
        }
    }

    @Test
    fun temaGelapMempertahankanWarnaGelapPermen() {
        val (_, gelap) = paletDariBenih(CONTOH_WARNA.first { it.kode == "mint" }.benih)
        for (k in listOf("latar", "kartu", "ubin", "garisHalus", "tinta", "tintaRedup", "latarLewat", "latarMepet")) {
            assertEquals(k, PERMEN_GELAP.getValue(k), gelap.getValue(k))
        }
    }

    @Test
    fun nadaGelapKuningMemakaiPlumPermen() {
        // Lemon: rona kuning, teks dan garis tema terang kembali ke plum Permen.
        val (terang, _) = paletDariBenih(CONTOH_WARNA.first { it.kode == "lemon" }.benih)
        assertEquals(PERMEN_TERANG.getValue("tinta"), terang.getValue("tinta"))
        assertEquals(PERMEN_TERANG.getValue("garis"), terang.getValue("garis"))
        // Langit: rona biru, teks ikut biru (bukan plum).
        val (langit, _) = paletDariBenih(CONTOH_WARNA.first { it.kode == "langit" }.benih)
        assertTrue(langit.getValue("tinta") != PERMEN_TERANG.getValue("tinta"))
    }

    @Test
    fun lanjutanMemakaiKelompokDanMemperingatkanKontras() {
        val g = kelompokDari(PERMEN_TERANG) + mapOf("tinta" to 0xFF8899AA.toInt())
        val t = turunkan(g, gelap = false)
        assertEquals(0xFF8899AA.toInt(), t.getValue("tinta"))
        val buruk = ujiKontras(t).filterNot { it.lolos }.map { it.uji.label }
        assertTrue(buruk.toString(), "Teks di latar" in buruk && "Teks di kartu" in buruk)
    }

    @Test
    fun pilihanPermenMemakaiPaletBawaanPersis() {
        assertEquals(PERMEN_TERANG to PERMEN_GELAP, PilihanWarna().palet())
    }

    @Test
    fun kodeKelompokDanLchBolakBalik() {
        val g = kelompokDari(PERMEN_GELAP)
        assertEquals(g, dariKodeKelompok(kodeKelompok(g)))
        assertEquals(emptyMap<String, Int>(), dariKodeKelompok("rusak;aksen=ZZZZZZ;asing=FFFFFF"))
        val w = Lch(0.8, 0.1, 250.0)
        val balik = dariKodeLch(kodeLch(w))!!
        assertEquals(w.l, balik.l, 1e-3); assertEquals(w.c, balik.c, 1e-4); assertEquals(w.h, balik.h, 0.1)
        assertEquals(null, dariKodeLch("a,b"))
    }
}
