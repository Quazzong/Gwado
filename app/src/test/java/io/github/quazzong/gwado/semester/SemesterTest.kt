package io.github.quazzong.gwado.semester

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.ingest.IngestError
import io.github.quazzong.gwado.ingest.aturanSemester
import io.github.quazzong.gwado.sumber.Kontrak
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SemesterTest {

    @Test
    fun tebakanSamaDenganGolden() {
        // tebak.json berisi daftar (bukan objek), jadi dibaca langsung tanpa Kontrak.muat
        val golden = Json.parseToJsonElement(Kontrak.file("golden/semester/tebak.json").readText()).jsonArray
        for (k in golden.map { it.jsonObject }) {
            val tgl = k.getValue("tanggal").jsonPrimitive.content
            val harap = k.getValue("hasil").jsonObject.mapValues { it.value.jsonPrimitive.content }
            assertEquals(tgl, harap, tebakSemester(LocalDate.parse(tgl)))
        }
    }

    @Test
    fun hariWibBergantiJam17Utc() {
        assertEquals(LocalDate.of(2026, 7, 31), tanggalWib(Instant.parse("2026-07-31T16:59:59Z")))
        assertEquals(LocalDate.of(2026, 8, 1), tanggalWib(Instant.parse("2026-07-31T17:00:00Z")))
    }

    @Test
    fun periksaIsian() {
        val baik = tebakSemester(LocalDate.of(2026, 10, 20))
        assertNull(periksaSemester(baik))
        assertNotNull(periksaSemester(baik + ("semester_kode" to "  ")))
        assertNotNull(periksaSemester(baik + ("semester_mulai" to "2026-8-1")))
    }

    @Test
    fun simpanBacaDanDipakaiIngest() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            assertNull(db.semesterTersimpan())
            assertThrows(IngestError::class.java) { db.aturanSemester() }

            val s = tebakSemester(LocalDate.of(2026, 10, 20))
            db.simpanSemester(s)
            assertEquals(s, db.semesterTersimpan())
            assertEquals(s, db.aturanSemester())

            assertEquals(emptyMap<String, String>(), db.ringkasanMatkul())
            db.exec("INSERT INTO courses (source_id, external_id, nama, diikuti) VALUES " +
                "('brone', '1', 'A', 1), ('brone', '2', 'B', 0), ('brone', '3', 'C', 0)")
            assertEquals(mapOf("brone" to "1 diikuti · 2 dilewati"), db.ringkasanMatkul())
        }
    }
}
