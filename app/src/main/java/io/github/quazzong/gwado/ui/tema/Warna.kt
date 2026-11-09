package io.github.quazzong.gwado.ui.tema

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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

val WarnaTerang = Warna(
    latar = Color(0xFFFBEFF4),
    kartu = Color(0xFFFFFFFF),
    ubin = Color(0xFFF1ECFA),
    tinta = Color(0xFF3A1D36),
    tintaRedup = Color(0xFF7A5872),
    garis = Color(0xFF4A2545),
    bayangan = Color(0xFF4A2545),
    garisHalus = Color(0xFFF1E4EE),
    kilau = Color.White.copy(alpha = 0.95f),
    aksen = Color(0xFFF7A8D8),
    aksenTerang = Color(0xFFFFCDEA),
    aksenGelap = Color(0xFFEA8BC5),
    diAtasAksen = Color(0xFF3A1D36),
    lavender = Color(0xFFAEA5D6),
    plum = Color(0xFF4A2545),
    diAtasPlum = Color(0xFFFBEFF4),
    strip = listOf(0f to Color(0xFFFAB9DF), 0.45f to Color(0xFFF7A8D8), 1f to Color(0xFFDDB4EC)),
    stiker = Color(0xFF4A2545),
    garisStiker = Color(0xFFFFFFFF),
    taburan = Color.White.copy(alpha = 0.62f),
    pola = Color(0xFF4A2545).copy(alpha = 0.15f),
    galat = Color(0xFFC8303B),
    minggu = Color(0xFFC8303B),
    luarBulan = Color(0xFF9A8296),
    ikonLayanan = Color(0xFFF1ECFA),
    latarLewat = Color(0xFFFFE4E7),
    latarMepet = Color(0xFFFFF4D3),
)

val WarnaGelap = Warna(
    latar = Color(0xFF1C1220),
    kartu = Color(0xFF2A1B30),
    ubin = Color(0xFF33233B),
    tinta = Color(0xFFF6E9F2),
    tintaRedup = Color(0xFFC4ABBF),
    garis = Color(0xFFEBC2E0),
    bayangan = Color(0xFFEBC2E0),
    garisHalus = Color(0xFF3A2842),
    kilau = Color.White.copy(alpha = 0.09f),
    aksen = Color(0xFFF7A8D8),
    aksenTerang = Color(0xFFFFCDEA),
    aksenGelap = Color(0xFFDE84BC),
    diAtasAksen = Color(0xFF2A1B30),
    lavender = Color(0xFF7D72B8),
    plum = Color(0xFFEBC2E0),
    diAtasPlum = Color(0xFF2A1B30),
    strip = listOf(0f to Color(0xFF7A3A6F), 0.5f to Color(0xFF5A2C5E), 1f to Color(0xFF3E2A5C)),
    stiker = Color(0xFFFBD3EC),
    garisStiker = Color(0xFF2A1B30),
    taburan = Color(0xFFF7A8D8).copy(alpha = 0.40f),
    pola = Color(0xFFEBC2E0).copy(alpha = 0.12f),
    galat = Color(0xFFFF8A8F),
    minggu = Color(0xFFFF8A8F),
    luarBulan = Color(0xFF9A84A0),
    ikonLayanan = Color(0xFF3A2842),
    latarLewat = Color(0xFF3D1B26),
    latarMepet = Color(0xFF3A2F1C),
)

/** Warna chip status, sama di tema terang dan gelap. */
object WarnaStatus {
    val lewat = listOf(Color(0xFFFFA3AA), Color(0xFFFF6B76))
    val diAtasLewat = Color(0xFF3A0F18)
    val mepet = listOf(Color(0xFFFFE79A), Color(0xFFFFD45C))
    val diAtasMepet = Color(0xFF3A2A00)
}
