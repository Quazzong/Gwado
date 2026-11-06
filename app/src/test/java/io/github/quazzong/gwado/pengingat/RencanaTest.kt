package io.github.quazzong.gwado.pengingat

import io.github.quazzong.gwado.tugas.BarisTugas
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RencanaTest {
    /** Senin 9 Nov 2026 23.59 WIB. */
    private val senin = "2026-11-09T16:59:00Z"

    private fun tugas(
        id: String,
        deadline: String?,
        kumpul: String? = null,
        status: String = "todo",
        matkul: String? = "Basis Data",
    ) = BarisTugas(id, "assign", "Judul $id", deadline, "sumber", "aktif", kumpul, matkul, "Brone", status)

    private fun t(teks: String) = Instant.parse(teks)

    @Test
    fun empatJenisPadaWaktunya() {
        val semua = semuaPengingat(listOf(tugas("a", senin)))
        assertEquals(
            listOf(
                Jenis.H3 to t("2026-11-06T16:59:00Z"),
                Jenis.PAGI to t("2026-11-09T00:00:00Z"),     // 07.00 WIB
                Jenis.H1 to t("2026-11-08T16:59:00Z"),
                Jenis.ALARM to t("2026-11-09T13:59:00Z"),
            ).sortedBy { it.second },
            semua.map { it.jenis to it.waktu },
        )
        assertEquals(setOf("tenggat:${t(senin).epochSecond}", "pagi:2026-11-09"), semua.map { it.tag }.toSet())
    }

    @Test
    fun deadlineDiMenitSamaDigabung() {
        val daftar = listOf(                                    // urut deadline, seperti daftarTugas
            tugas("c", "2026-11-09T15:00:00Z"),                 // 22.00 WIB
            tugas("a", senin),
            tugas("b", "2026-11-09T16:59:30Z"),                 // detik beda, menit sama
        )
        val h3 = semuaPengingat(daftar).filter { it.jenis == Jenis.H3 }
        assertEquals(listOf(listOf("c"), listOf("a", "b")), h3.map { p -> p.tugas.map { it.id } })
        val pagi = semuaPengingat(daftar).single { it.jenis == Jenis.PAGI }
        assertEquals(listOf("c", "a", "b"), pagi.tugas.map { it.id })
    }

    @Test
    fun tugasBeresDanTanpaDeadlineTidakDiingatkan() {
        val daftar = listOf(
            tugas("kumpul", senin, kumpul = "terkumpul"),
            tugas("selesai", senin, kumpul = "selesai"),
            tugas("centang", senin, status = "done"),
            tugas("kosong", null),
        )
        assertTrue(semuaPengingat(daftar).isEmpty())
    }

    @Test
    fun pagiHanyaDeadlineSetelahJamTujuh() {
        val daftar = listOf(tugas("subuh", "2026-11-08T22:00:00Z"), tugas("malam", senin))
        // "subuh" = Senin 05.00 WIB: sudah lewat saat ringkasan pagi
        val pagi = semuaPengingat(daftar).filter { it.jenis == Jenis.PAGI }
        assertEquals(listOf(listOf("malam")), pagi.map { p -> p.tugas.map { it.id } })
    }

    @Test
    fun yangTibaDitampilkanSekali() {
        val daftar = listOf(tugas("a", senin))
        val h1 = t("2026-11-08T16:59:00Z")
        val tempo = jatuhTempo(daftar, h1.plusSeconds(120), emptySet())
        assertEquals(listOf(Jenis.H1), tempo.tampil.map { it.jenis })
        // H-3 dua hari lalu sudah lewat toleransi, jadi penandanya tidak perlu disimpan.
        assertEquals(setOf("h1:${t(senin).epochSecond}"), tempo.terkirim)
        assertTrue(jatuhTempo(daftar, h1.plusSeconds(600), tempo.terkirim).tampil.isEmpty())
    }

    @Test
    fun setelahHpMatiHanyaYangMasihBerlaku() {
        // Deadline Senin 02.00 WIB: H-1 Minggu 02.00, alarm Minggu 23.00, tanpa ringkasan pagi.
        // HP mati dari Sabtu sampai Minggu 23.30: hanya alarm yang tampil.
        val dl = "2026-11-08T19:00:00Z"
        val sekarang = t("2026-11-08T16:30:00Z")
        val tempo = jatuhTempo(listOf(tugas("a", dl)), sekarang, emptySet())
        assertEquals(listOf(Jenis.ALARM), tempo.tampil.map { it.jenis })
    }

    @Test
    fun yangLewatToleransiAtauDeadlineLewatDilewati() {
        val daftar = listOf(tugas("a", senin))
        // Tugas baru masuk 3 jam setelah waktu H-3-nya: tidak menyusul.
        assertTrue(jatuhTempo(daftar, t("2026-11-06T19:59:00Z"), emptySet()).tampil.isEmpty())
        // Deadline sudah lewat: alarm yang tertunda tidak muncul.
        assertTrue(jatuhTempo(daftar, t(senin).plusSeconds(60), emptySet()).tampil.isEmpty())
    }

    @Test
    fun penandaLamaDibuang() {
        val lama = setOf("h3:${t(senin).epochSecond}", "pagi:2026-11-01", "aneh")
        val tempo = jatuhTempo(emptyList(), t("2026-11-08T00:00:00Z"), lama)
        assertTrue(tempo.terkirim.isEmpty())
    }

    @Test
    fun berikutnyaPalingAwal() {
        val daftar = listOf(tugas("a", senin), tugas("b", "2026-11-12T16:59:00Z"))
        assertEquals(t("2026-11-08T16:59:00Z"), berikutnya(daftar, t("2026-11-07T00:00:00Z"))?.waktu)
        assertNull(berikutnya(daftar, t("2026-11-12T16:00:00Z")))
    }

    @Test
    fun anggotaTagMengikutiDataSekarang() {
        val tag = "tenggat:${t(senin).epochSecond}"
        val daftar = listOf(tugas("a", senin), tugas("b", senin))
        assertEquals(listOf("a", "b"), anggota(tag, daftar).map { it.id })
        val setelahCentang = listOf(tugas("a", senin, status = "done"), tugas("b", senin))
        assertEquals(listOf("b"), anggota(tag, setelahCentang).map { it.id })
        assertTrue(anggota("pagi:2026-11-09", listOf(tugas("b", senin, kumpul = "terkumpul"))).isEmpty())
    }

    @Test
    fun teksNotifikasi() {
        val satu = semuaPengingat(listOf(tugas("a", senin))).single { it.jenis == Jenis.H1 }
        assertEquals(TeksPengingat("Judul a", "Deadline besok, Sen 9 Nov 23.59 · Basis Data", listOf("Deadline besok, Sen 9 Nov 23.59 · Basis Data")), teks(satu))

        val dua = semuaPengingat(listOf(tugas("a", senin), tugas("b", senin, matkul = null)))
        val h3 = teks(dua.single { it.jenis == Jenis.H3 })
        assertEquals("2 tugas deadline 3 hari lagi", h3.judul)
        assertEquals("Sen 9 Nov 23.59: Judul a, Judul b", h3.isi)
        assertEquals(listOf("Judul a · Basis Data", "Judul b"), h3.baris)
        assertEquals("3 jam lagi", teks(dua.single { it.jenis == Jenis.ALARM }).judul.substringAfter("deadline "))

        val pagi = teks(dua.single { it.jenis == Jenis.PAGI })
        assertEquals("2 tugas deadline hari ini", pagi.judul)
        assertEquals(listOf("23.59 Judul a", "23.59 Judul b"), pagi.baris)
    }

    @Test
    fun waktuDariPenanda() {
        assertEquals(t("2026-11-06T16:59:00Z"), waktuKunci("h3:${t(senin).epochSecond}"))
        assertEquals(t(senin).minus(Duration.ofHours(3)), waktuKunci("alarm:${t(senin).epochSecond}"))
        assertEquals(t("2026-11-09T00:00:00Z"), waktuKunci("pagi:2026-11-09"))
        assertNull(waktuKunci("aneh"))
    }
}
