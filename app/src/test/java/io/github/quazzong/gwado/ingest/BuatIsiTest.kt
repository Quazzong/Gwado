package io.github.quazzong.gwado.ingest

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.semester.simpanSemester
import io.github.quazzong.gwado.semester.tebakSemester
import java.time.LocalDate
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.fetch.FetchError
import io.github.quazzong.gwado.tugas.daftarTugas
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val MULAI = Instant.parse("2026-10-20T05:00:02.789Z")
private val SELESAI = Instant.parse("2026-10-20T05:00:07.123Z")

class BuatIsiTest {

    @Test
    fun fetchIdSepertiSerper() {
        val id = buatFetchId("brone", MULAI)
        assertTrue(id, Regex("^brone-20261020T050002Z-[0-9a-f]{6}$").matches(id))
    }

    @Test
    fun isiGagalDipotongDanWaktuKeDetik() {
        val isi = isiGagal("eling", MULAI, SELESAI, FetchError("lain", "x".repeat(400)))
        assertEquals(300, isi.error!!.pesan.length)
        assertEquals("2026-10-20T05:00:02Z", isi.mulai)
        assertEquals("2026-10-20T05:00:07Z", isi.selesai)
    }

    @Test
    fun runGagalTercatatDanDaftarKosong() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            db.simpanSemester(tebakSemester(LocalDate.of(2026, 10, 20)))
            val h = db.proses(isiGagal("brone", MULAI, SELESAI, FetchError("token_ditolak", "invalidtoken")))
            assertEquals("failed", h.status)
            assertEquals(
                "token_ditolak: invalidtoken",
                db.satu("SELECT error_singkat FROM source_runs")!!["error_singkat"],
            )
            assertEquals(0, db.daftarTugas().size)
        }
    }
}
