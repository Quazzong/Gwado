package io.github.quazzong.gwado.beranda

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.tugas.tambahManual
import io.github.quazzong.gwado.tugas.tandai
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SelesaiMingguTest {
    @Test
    fun hanyaDicentangDenganDeadlineMingguIni() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            val dibuat = Instant.parse("2026-10-01T00:00:00Z")
            fun tugas(judul: String, deadline: String?, centang: Boolean): String {
                val id = db.tambahManual(judul, deadline?.let { Instant.parse(it) }, null, dibuat)
                if (centang) db.tandai(id, true, dibuat)
                return id
            }
            val kamis = tugas("Kamis", "2026-11-05T10:00:00Z", centang = true)
            val minggu = tugas("Minggu malam", "2026-11-08T16:59:00Z", centang = true)       // Minggu 23.59 WIB
            tugas("Senin depan", "2026-11-08T17:00:00Z", centang = true)                      // Senin 00.00 WIB
            tugas("Minggu lalu", "2026-10-28T10:00:00Z", centang = true)
            tugas("Belum dicentang", "2026-11-05T10:00:00Z", centang = false)
            tugas("Tanpa deadline", null, centang = true)

            val hasil = selesaiMinggu(db.daftarTugas(semua = true), LocalDate.parse("2026-11-04"))
            assertEquals(setOf(kamis, minggu), hasil.map { it.id }.toSet())
        }
    }
}
