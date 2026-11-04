package io.github.quazzong.gwado.layanan

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import io.github.quazzong.gwado.segar.sumberTerakhir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SumberTest {
    @Test
    fun matikanLaluNyalakanLagi() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            val awal = db.sumberTerakhir().map { it.id }
            assertTrue("brone" in awal)

            db.aturAktif("brone", false)
            assertEquals(awal - "brone", db.sumberTerakhir().map { it.id })

            db.aturAktif("brone", true)
            assertEquals(awal, db.sumberTerakhir().map { it.id })
        }
    }
}
