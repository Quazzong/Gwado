package io.github.quazzong.gwado.beranda

import io.github.quazzong.gwado.tugas.BarisTugas
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BeresTest {
    private fun t(
        id: String,
        deadline: String?,
        status: String = "todo",
        kumpul: String? = null,
        situs: String = "aktif",
    ) = BarisTugas(
        id = id, jenis = "assign", judul = id, deadline = deadline, asal = "sumber",
        statusSitus = situs, statusKumpulSitus = kumpul, matkul = null, sumber = "Brone", status = status,
    )

    private val daftar = listOf(
        t("dicentang", "2026-11-05T10:00:00Z", status = "done"),
        t("terkumpul", "2026-11-06T10:00:00Z", kumpul = "terkumpul"),
        t("terkumpul-hilang", "2026-11-06T10:00:00Z", kumpul = "terkumpul", situs = "hilang"),
        t("terbuka", "2026-11-05T10:00:00Z"),
        t("lama", "2026-09-01T10:00:00Z", status = "done"),
        t("tanpa", null, kumpul = "terkumpul"),
    )

    @Test
    fun selesaiMingguIniTermasukTerkumpul() {
        val hasil = selesaiMinggu(daftar, LocalDate.parse("2026-11-04")).map { it.id }.toSet()
        assertEquals(setOf("dicentang", "terkumpul"), hasil)
    }

    @Test
    fun saringLayananDanKata() {
        val aps = t("BAB 8 - ERD", "2026-10-20T10:00:00Z", kumpul = "terkumpul").copy(matkul = "Praktikum APS", sumber = "Google Classroom")
        val manual = t("Catatan APS", null, status = "done").copy(sumber = null)
        val lain = t("Tugas 05", "2026-10-21T10:00:00Z", status = "done")
        val d = listOf(aps, manual, lain)
        assertEquals(listOf(aps, manual), saring(d, "  aps ", null))
        assertEquals(listOf(aps), saring(d, "aps", "Google Classroom"))
        assertEquals(listOf(manual), saring(d, "", "Manual"))
        assertEquals(d, saring(d, "", null))
    }

    @Test
    fun semuaSelesaiTerbaruDuluTanpaDeadlineTerakhir() {
        assertEquals(listOf("terkumpul", "dicentang", "lama", "tanpa"), semuaSelesai(daftar).map { it.id })
    }
}
