package io.github.quazzong.gwado.ui.tema

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.quazzong.gwado.tampilan.PERMEN_GELAP
import io.github.quazzong.gwado.tampilan.PERMEN_TERANG
import io.github.quazzong.gwado.tampilan.POSISI_STRIP_GELAP
import io.github.quazzong.gwado.tampilan.POSISI_STRIP_TERANG
import io.github.quazzong.gwado.tampilan.Token

/** Token warna untuk satu tema (terang atau gelap). */
@Immutable
data class Warna(
    val latar: Color,
    val kartu: Color,
    val ubin: Color,
    val tinta: Color,
    val tintaRedup: Color,
    val garis: Color,
    val bayangan: Color,
    val garisHalus: Color,
    val kilau: Color,
    val aksen: Color,
    val aksenTerang: Color,
    val aksenGelap: Color,
    val diAtasAksen: Color,
    val lavender: Color,
    val plum: Color,
    val diAtasPlum: Color,
    /** Titik gradasi strip judul (posisi 0..1 ke warna), arah 115 derajat. */
    val strip: List<Pair<Float, Color>>,
    val stiker: Color,
    val garisStiker: Color,
    val taburan: Color,
    val pola: Color,
    val galat: Color,
    val minggu: Color,
    val luarBulan: Color,
    val ikonLayanan: Color,
    val latarLewat: Color,
    val latarMepet: Color,
)

/** Token tampilan dari palet ARGB (Permen, Simpel, atau Lanjutan). */
fun warnaDari(t: Token, gelap: Boolean): Warna {
    fun c(k: String) = Color(t.getValue(k))
    val posisi = if (gelap) POSISI_STRIP_GELAP else POSISI_STRIP_TERANG
    return Warna(
        latar = c("latar"), kartu = c("kartu"), ubin = c("ubin"), tinta = c("tinta"), tintaRedup = c("tintaRedup"),
        garis = c("garis"), bayangan = c("bayangan"), garisHalus = c("garisHalus"), kilau = c("kilau"),
        aksen = c("aksen"), aksenTerang = c("aksenTerang"), aksenGelap = c("aksenGelap"), diAtasAksen = c("diAtasAksen"),
        lavender = c("lavender"), plum = c("plum"), diAtasPlum = c("diAtasPlum"),
        strip = posisi.mapIndexed { i, pos -> pos to c("strip$i") },
        stiker = c("stiker"), garisStiker = c("garisStiker"), taburan = c("taburan"), pola = c("pola"),
        galat = c("galat"), minggu = c("minggu"), luarBulan = c("luarBulan"), ikonLayanan = c("ikonLayanan"),
        latarLewat = c("latarLewat"), latarMepet = c("latarMepet"),
    )
}

/** Palet bawaan Permen (DESAIN §2.1). */
val WarnaTerang = warnaDari(PERMEN_TERANG, gelap = false)
val WarnaGelap = warnaDari(PERMEN_GELAP, gelap = true)

/** Warna chip status, sama di tema terang dan gelap. */
object WarnaStatus {
    val lewat = listOf(Color(0xFFFFA3AA), Color(0xFFFF6B76))
    val diAtasLewat = Color(0xFF3A0F18)
    val mepet = listOf(Color(0xFFFFE79A), Color(0xFFFFD45C))
    val diAtasMepet = Color(0xFF3A2A00)
}
