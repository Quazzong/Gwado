package io.github.quazzong.gwado.beranda

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.tugas.BarisTugas
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RingkasanTest {
    private val rabuMulai = LocalDate.parse("2026-08-26")

    @Test
    fun mingguDihitungSeninSampaiMinggu() {
        assertNull(mingguKe(rabuMulai, LocalDate.parse("2026-08-25")))
        assertEquals(1, mingguKe(rabuMulai, rabuMulai))
        assertEquals(1, mingguKe(rabuMulai, LocalDate.parse("2026-08-30")))   // Minggu
        assertEquals(2, mingguKe(rabuMulai, LocalDate.parse("2026-08-31")))   // Senin
        assertEquals(11, mingguKe(rabuMulai, LocalDate.parse("2026-11-03")))
    }

    private fun t(id: String, deadline: String?, status: String = "todo", kumpul: String? = null, situs: String = "aktif") =
        BarisTugas(id, "assign", id, deadline, "sumber", situs, kumpul, null, "Brone", status)

    @Test
    fun progresHanyaMingguIniMenurutTanggalWib() {
        val selasa = LocalDate.parse("2026-11-03")          // minggu Sen 2 – Min 8 Nov
        val daftar = listOf(
            t("selesai", "2026-11-04T16:59:00Z", status = "done"),
            t("terkumpul", "2026-11-05T16:59:00Z", kumpul = "terkumpul"),
            t("belum", "2026-11-06T16:59:00Z"),
            t("draf", "2026-11-06T16:59:00Z", kumpul = "draf"),
            t("min2359", "2026-11-08T16:59:00Z"),            // Minggu 8 Nov 23.59 WIB: ikut
            t("sen0000", "2026-11-01T17:00:00Z"),            // Senin 2 Nov 00.00 WIB: ikut
            t("senDepan", "2026-11-08T17:00:00Z"),           // Senin 9 Nov 00.00 WIB: tidak
            t("minLalu", "2026-11-01T16:59:00Z"),            // Minggu 1 Nov 23.59 WIB: tidak
            t("tanpa", null),
            t("hilang", "2026-11-04T16:59:00Z", situs = "hilang"),
            t("hilangSelesai", "2026-11-04T16:59:00Z", status = "done", situs = "hilang"),
        )
        assertEquals(Progres(selesai = 3, total = 7), progresMinggu(daftar, selasa))
        assertEquals(Progres(0, 0), progresMinggu(emptyList(), selasa))
    }

    @Test
    fun kuliahMulaiDisimpanDanDihapus() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            assertNull(db.kuliahMulai())
            db.simpanKuliahMulai(rabuMulai)
            assertEquals(rabuMulai, db.kuliahMulai())
            db.simpanKuliahMulai(LocalDate.parse("2026-09-01"))
            assertEquals(LocalDate.parse("2026-09-01"), db.kuliahMulai())
            db.exec("UPDATE preferences SET nilai = 'rusak' WHERE kunci = ?", KUNCI_KULIAH_MULAI)
            assertNull(db.kuliahMulai())
            db.simpanKuliahMulai(null)
            assertNull(db.kuliahMulai())
        }
    }
}
