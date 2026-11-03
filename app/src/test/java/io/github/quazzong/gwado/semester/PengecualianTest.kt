package io.github.quazzong.gwado.semester

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.ingest.Isi
import io.github.quazzong.gwado.ingest.proses
import io.github.quazzong.gwado.sumber.HasilSumber
import io.github.quazzong.gwado.sumber.Kelas
import io.github.quazzong.gwado.sumber.Tugas
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private fun kelas(id: String, kategori: String) = Kelas(id, "MK $id", null, null, kategori, null, null, null)

private fun tugas(n: Int, kelas: String) = Tugas("assign:$n", kelas, "assign", "$n", "Tugas $n", "", null, null, null)

/** Run ke-n: kelas 10 semester ini, kelas 20 semester lalu, masing-masing satu tugas. */
private fun isi(n: Int) = Isi(
    fetchId = "brone-uji-$n",
    sumber = "brone",
    mulai = "2026-10-2${n}T05:00:00Z",
    selesai = "2026-10-2${n}T05:00:05Z",
    hasil = HasilSumber(
        listOf(kelas("10", "Ganjil 2026/2027"), kelas("20", "Genap 2025/2026")),
        listOf(tugas(1, "10"), tugas(2, "20")),
    ),
)

class PengecualianTest {

    @Test
    fun ikutiKelasLamaLaluKembaliOtomatis() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            db.simpanSemester(tebakSemester(LocalDate.of(2026, 10, 20)))
            db.proses(isi(0))
            assertEquals(listOf("10" to true, "20" to false), db.daftarMatkul("brone").map { it.externalId to it.diikuti })

            db.aturPengecualian("brone", "20", "ikuti")
            assertEquals("ikuti", db.daftarMatkul("brone").single { it.externalId == "20" }.aksi)
            db.proses(isi(1))
            val m = db.daftarMatkul("brone").single { it.externalId == "20" }
            assertTrue(m.diikuti)
            assertEquals("pengecualian manual: ikuti", m.alasan)

            db.aturPengecualian("brone", "20", null)
            assertNull(db.daftarMatkul("brone").single { it.externalId == "20" }.aksi)
            db.proses(isi(2))
            assertFalse(db.daftarMatkul("brone").single { it.externalId == "20" }.diikuti)
        }
    }

    @Test
    fun aksiTakDikenalDitolak() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            assertThrows(IllegalArgumentException::class.java) { db.aturPengecualian("brone", "1", "hapus") }
        }
    }
}
