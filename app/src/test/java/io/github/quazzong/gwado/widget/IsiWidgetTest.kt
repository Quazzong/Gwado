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
        // Samsung Note 9, 4×1: lapor 130dp, digambar ±112dp → 3 baris.
        assertEquals(TataTerdekat(3, rinci = false, progres = false), tataTerdekat(130f, 12, adaProgres = true))
        // 4×2: lapor 288dp → 8 baris.
        assertEquals(8, tataTerdekat(288f, 12, adaProgres = true).baris)
    }

    @Test
    fun tugasSedikitMemakaiBarisRinciLaluProgres() {
        assertEquals(TataTerdekat(5, rinci = true, progres = false), tataTerdekat(288f, 5, adaProgres = true))
        assertEquals(TataTerdekat(2, rinci = true, progres = true), tataTerdekat(288f, 2, adaProgres = true))
        assertEquals(TataTerdekat(2, rinci = true, progres = false), tataTerdekat(288f, 2, adaProgres = false))
        assertEquals(TataTerdekat(2, rinci = false, progres = false), tataTerdekat(130f, 2, adaProgres = true))
    }
}
