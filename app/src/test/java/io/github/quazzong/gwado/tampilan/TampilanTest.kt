package io.github.quazzong.gwado.tampilan

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.migrasi
import org.junit.Assert.assertEquals
import org.junit.Test

class TampilanTest {
    @Test
    fun bawaanTitikLaluTersimpan() {
        Db.buka(":memory:").use { db ->
            db.migrasi()
            assertEquals(Tekstur.TITIK, db.tekstur())
            db.simpanTekstur(Tekstur.BENTUK)
            assertEquals(Tekstur.BENTUK, db.tekstur())
            db.simpanTekstur(Tekstur.POLOS)
            assertEquals(Tekstur.POLOS, db.tekstur())
        }
    }

    @Test
    fun kodeRusakJadiBawaan() {
        assertEquals(Tekstur.TITIK, Tekstur.dariKode("garis-miring"))
        assertEquals(Tekstur.TITIK, Tekstur.dariKode(null))
        assertEquals(Tekstur.KOTAK, Tekstur.dariKode("kotak"))
    }
}
