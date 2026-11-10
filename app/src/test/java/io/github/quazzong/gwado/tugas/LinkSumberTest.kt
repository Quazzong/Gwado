package io.github.quazzong.gwado.tugas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Penolakan tambahan linkSumber di luar golden: URL yang dibaca browser berbeda dari pecahUrl. */
class LinkSumberTest {
    private val domain = listOf("brone.ub.ac.id", "classroom.google.com")

    @Test
    fun garisMiringTerbalikDitolak() {
        assertNull(linkSumber("https://lain.example\\@brone.ub.ac.id/x", domain))
        assertNull(linkSumber("https://brone.ub.ac.id\\x", domain))
    }

    @Test
    fun spasiDanKarakterKontrolDitolak() {
        assertNull(linkSumber("https://brone.ub.ac.id/a b", domain))
        assertNull(linkSumber(" https://brone.ub.ac.id/x", domain))
        assertNull(linkSumber("https://bro\tne.ub.ac.id/x", domain))
        assertNull(linkSumber("https://brone.ub.ac.id/x\n", domain))
        assertNull(linkSumber("https://brone.ub.ac.id/\u007f", domain))
    }

    @Test
    fun linkBiasaTetapLolos() {
        val url = "https://brone.ub.ac.id/mod/assign/view.php?id=1"
        assertEquals(url, linkSumber(url, domain))
        assertEquals("https://classroom.google.com/c/abc%20d", linkSumber("https://classroom.google.com/c/abc%20d", domain))
    }
}
