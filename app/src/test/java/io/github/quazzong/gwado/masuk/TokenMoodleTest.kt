package io.github.quazzong.gwado.masuk

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Semua nilai karangan.
private const val SITUS = "https://moodle.example.com"
private const val PASSPORT = "11111111-2222-3333-4444-555555555555"
private const val TOKEN = "0123456789abcdef0123456789abcdef"
private const val PRIVAT = "privat-karangan"

private fun redirect(isi: String) =
    "moodlemobile://token=" + Base64.getEncoder().encodeToString(isi.toByteArray())

private val TANDA = md5(SITUS + PASSPORT)

class TokenMoodleTest {
    private fun berhasil(h: HasilLogin) = h as? HasilLogin.Berhasil ?: error("harusnya Berhasil, dapat $h")
    private fun gagal(h: HasilLogin) = assertTrue("harusnya Gagal, dapat $h", h is HasilLogin.Gagal)

    @Test
    fun md5SesuaiNilaiBaku() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5(""))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", md5("abc"))
    }

    @Test
    fun alamatLoginTanpaGarisMiringGanda() {
        assertEquals(
            "$SITUS/admin/tool/mobile/launch.php?service=moodle_mobile_app&passport=$PASSPORT&urlscheme=moodlemobile",
            alamatLogin("$SITUS/", PASSPORT),
        )
    }

    @Test
    fun berhasilDenganPrivateToken() {
        val b = berhasil(bacaRedirect(redirect("$TANDA:::$TOKEN:::$PRIVAT"), SITUS, PASSPORT))
        assertEquals(TOKEN, b.token)
        assertEquals(PRIVAT, b.privateToken)
    }

    @Test
    fun berhasilTanpaPrivateToken() {
        val b = berhasil(bacaRedirect(redirect("$TANDA:::$TOKEN"), SITUS, PASSPORT))
        assertEquals(TOKEN, b.token)
        assertNull(b.privateToken)
    }

    @Test
    fun situsDenganGarisMiringTetapCocok() {
        berhasil(bacaRedirect(redirect("$TANDA:::$TOKEN"), "$SITUS/", PASSPORT))
    }

    @Test
    fun alamatTempelanDenganSpasiDanBarisBaru() {
        val r = redirect("$TANDA:::$TOKEN:::$PRIVAT")
        berhasil(bacaRedirect("  " + r.substring(0, 40) + "\n" + r.substring(40) + "\n", SITUS, PASSPORT))
    }

    @Test
    fun passportLainDitolak() = gagal(bacaRedirect(redirect("$TANDA:::$TOKEN"), SITUS, "passport-lain"))

    @Test
    fun situsLainDitolak() = gagal(bacaRedirect(redirect("$TANDA:::$TOKEN"), "https://lain.example.com", PASSPORT))

    @Test
    fun tanpaPassportDitolak() = gagal(bacaRedirect(redirect("$TANDA:::$TOKEN"), SITUS, ""))

    @Test
    fun tanpaTokenDitolak() {
        gagal(bacaRedirect("moodlemobile://", SITUS, PASSPORT))
        gagal(bacaRedirect(redirect("$TANDA:::"), SITUS, PASSPORT))
        gagal(bacaRedirect("moodlemobile://token=%%%", SITUS, PASSPORT))
    }

    @Test
    fun toStringTidakMemuatToken() {
        val teks = bacaRedirect(redirect("$TANDA:::$TOKEN:::$PRIVAT"), SITUS, PASSPORT).toString()
        assertFalse(teks, TOKEN in teks)
        assertFalse(teks, PRIVAT in teks)
    }
}
