package io.github.quazzong.gwado.tugas

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class MassalTest {
    @Test
    fun centangSemuaSekaliSaja() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            val dulu = Instant.parse("2026-09-01T10:00:00Z")
            val sekarang = Instant.parse("2026-11-04T12:00:00Z")
            val a = db.tambahManual("Tugas A", dulu, null, dulu)
            val b = db.tambahManual("Tugas B", dulu, null, dulu)

            assertEquals(2, db.tandaiSemua(listOf(a, b), sekarang))
            assertEquals(0, db.tandaiSemua(listOf(a, b), sekarang))      // sudah selesai: tidak berubah
            assertEquals("done", db.ambilTugas(a)?.status)
            assertEquals(
                1L,
                db.satu("SELECT count(*) AS n FROM task_changes WHERE task_id = ? AND field = 'status'", b)!!["n"],
            )
        }
    }
}
