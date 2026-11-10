package io.github.quazzong.gwado.widget

import io.github.quazzong.gwado.beranda.Progres
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.waktu.Makna
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsiWidgetTest {
    private val sekarang = Instant.parse("2026-11-06T10:00:00+07:00")

    private fun tugas(id: String, deadline: String?, kumpul: String? = null, matkul: String? = "Basis Data") =
        BarisTugas(id, "assign", "Judul $id", deadline, "sumber", "aktif", kumpul, matkul, "Brone", "todo")

    @Test
    fun mendesakDuluLaluSisanyaDibatasi() {
        val daftar = listOf(
            tugas("lewat", "2026-11-05T16:59:00Z"),
            tugas("besok", "2026-11-07T16:59:00Z"),
            tugas("lima", "2026-11-11T16:59:00Z"),
            tugas("tujuh", "2026-11-13T16:59:00Z"),
            tugas("kosong", null, matkul = null),
        )
        val isi = isiWidget(daftar, sekarang, null)
        assertEquals(2, isi.mendesak)
        assertEquals(5, isi.jumlah)
        assertEquals(listOf("lewat", "besok", "lima"), isi.terdekat.map { it.id })
        assertEquals(Makna.LEWAT, isi.terdekat[0].chip?.makna)
        assertEquals("besok", isi.terdekat[1].chip?.label)
        assertEquals("Basis Data · Sab 7 Nov 23.59", isi.terdekat[1].keterangan)
        assertNull(isi.diambil)
        assertEquals("Tugas manual", isiWidget(daftar, sekarang, null, maks = 5).terdekat[4].keterangan)
    }

    @Test
    fun tugasTerkumpulDiSitusTidakIkut() {
        val daftar = listOf(tugas("terkumpul", "2026-11-06T16:59:00Z", kumpul = "terkumpul"), tugas("kosong", null))
        val isi = isiWidget(daftar, sekarang, null)
        assertEquals(0, isi.mendesak)
        assertEquals(1, isi.jumlah)
        assertNull(isi.terdekat[0].chip)
    }

    @Test
    fun labelDiambilDanProgresKosong() {
        val isi = isiWidget(emptyList(), sekarang, Instant.parse("2026-11-06T09:02:00+07:00"), progres = Progres(0, 0))
        assertEquals("09.02", isi.diambil)
        assertNull(isi.progres)
    }

    @Test
    fun pesanKosongMembedakanBelumDiambilDanSemuaBeres() {
        val belum = isiWidget(emptyList(), sekarang, null)
        val beres = isiWidget(emptyList(), sekarang, Instant.parse("2026-11-06T09:02:00+07:00"))
        assertEquals("Belum ada tugas", pesanKosong(belum, singkat = true))
        assertEquals("Belum ada tugas. Tarik beranda Gwado untuk mengambil.", pesanKosong(belum, singkat = false))
        assertEquals("Semua beres", pesanKosong(beres, singkat = true))
        assertEquals("Semua tugas beres", pesanKosong(beres, singkat = false))
    }

    @Test
    fun tugasBanyakSemuaBarisRingkasTanpaTerpotong() {
        // Samsung Note 9, 4×1: lapor 130dp (WQHD) atau 129dp (FHD+), digambar ±114dp → 3 baris.
        assertEquals(TataTerdekat(3, rinci = false, progres = false), tataTerdekat(130f, 12, adaProgres = true))
        assertEquals(3, tataTerdekat(129f, 12, adaProgres = true).baris)
        // 4×2: lapor 288dp, digambar ±238dp → 9 baris (±233dp).
        assertEquals(9, tataTerdekat(288f, 12, adaProgres = true).baris)
    }

    @Test
    fun tugasSedikitMemakaiBarisRinciLaluProgres() {
        assertEquals(TataTerdekat(5, rinci = true, progres = false), tataTerdekat(288f, 5, adaProgres = true))
        assertEquals(TataTerdekat(2, rinci = true, progres = true), tataTerdekat(288f, 2, adaProgres = true))
        assertEquals(TataTerdekat(2, rinci = true, progres = false), tataTerdekat(288f, 2, adaProgres = false))
        assertEquals(TataTerdekat(2, rinci = false, progres = false), tataTerdekat(130f, 2, adaProgres = true))
    }

    @Test
    fun angkaSempitMengecilSupayaChipMuat() {
        // Kartu tinggi: angka penuh 48sp.
        assertEquals(ANGKA_MAKS, ukuranAngkaSempit(110f, 200f, 1, adaChip = true), 0.01f)
        // 1×1 rendah: angka + chip harus muat di tinggi isi kartu.
        val ukuran = ukuranAngkaSempit(80f, 90f, 1, adaChip = true)
        assert(ukuran < ANGKA_MAKS)
        assert(ukuran * TINGGI_BARIS + TINGGI_CHIP <= 90f * KOREKSI_TINGGI - PADDING_TEGAK + 0.01f)
        // Tanpa chip ada ruang lebih untuk angka.
        assert(ukuranAngkaSempit(80f, 90f, 1, adaChip = false) > ukuran)
    }

    @Test
    fun angkaSempitIkutLebarDanUkuranHuruf() {
        // Dua angka di kartu sempit dibatasi lebarnya.
        val dua = ukuranAngkaSempit(60f, 200f, 2, adaChip = true)
        assert(dua * LEBAR_DIGIT * 2 <= 60f - PADDING_DATAR + 0.01f)
        // Huruf sistem diperbesar: ukuran sp dikecilkan supaya hasil di layar tetap muat.
        assert(ukuranAngkaSempit(80f, 90f, 1, adaChip = true, skalaHuruf = 1.3f) < ukuranAngkaSempit(80f, 90f, 1, adaChip = true))
        // Tidak pernah lebih kecil dari batas bawah.
        assertEquals(ANGKA_MIN, ukuranAngkaSempit(40f, 40f, 3, adaChip = true), 0.01f)
    }

    @Test
    fun angkaTinggiMengisiRuangDenganBatas() {
        // 4×2 Note 9 (lapor 361×288): angka sampai batas atas.
        assertEquals(ANGKA_TINGGI_MAKS, ukuranAngkaTinggi(361f, 288f, 1), 0.01f)
        // Tinggi pas ambang tata letak tinggi: tidak lebih kecil dari angka lama 40sp.
        assertEquals(40f, ukuranAngkaTinggi(361f, 150f, 1), 0.01f)
        // Di antaranya angka mengikuti tinggi.
        val sedang = ukuranAngkaTinggi(361f, 220f, 1)
        assert(sedang > 40f && sedang < ANGKA_TINGGI_MAKS)
        // 2×2 dengan tiga angka dibatasi lebar.
        val tiga = ukuranAngkaTinggi(170f, 288f, 3)
        assert(tiga * LEBAR_DIGIT * 3 <= 170f - PADDING_DATAR + 0.01f)
        // Huruf sistem diperbesar: angka mengecil.
        assert(ukuranAngkaTinggi(361f, 220f, 1, skalaHuruf = 1.3f) < sedang)
    }
}
