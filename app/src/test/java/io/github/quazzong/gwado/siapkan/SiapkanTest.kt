package io.github.quazzong.gwado.siapkan

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiapkanTest {
    @Test
    fun tandaSiapTersimpan() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            assertFalse(db.siapSelesai())
            db.tandaiSiap()
            db.tandaiSiap()                       // diulang tidak galat
            assertTrue(db.siapSelesai())
        }
    }

    @Test
    fun kapanPerluSiapkan() {
        assertTrue(perluSiapkan(selesai = false, adaSemester = false, adaLayanan = false))
        assertTrue(perluSiapkan(selesai = false, adaSemester = true, adaLayanan = false))
        assertTrue(perluSiapkan(selesai = false, adaSemester = false, adaLayanan = true))
        assertFalse(perluSiapkan(selesai = false, adaSemester = true, adaLayanan = true))
        assertFalse(perluSiapkan(selesai = true, adaSemester = false, adaLayanan = false))
    }

    @Test
    fun pesanKurangSiap() {
        assertEquals("Tambah minimal satu layanan dulu.", kurangSiap(adaLayanan = false, adaSemester = false))
        assertEquals("Konfirmasi semester dulu.", kurangSiap(adaLayanan = true, adaSemester = false))
        assertNull(kurangSiap(adaLayanan = true, adaSemester = true))
    }
}
