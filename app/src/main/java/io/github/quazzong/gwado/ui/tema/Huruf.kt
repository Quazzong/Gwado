package io.github.quazzong.gwado.ui.tema

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.R

/** Satu ketebalan dari file variable font: sumbu wght diatur sesuai berat. */
@OptIn(ExperimentalTextApi::class)
private fun berat(resId: Int, nilai: Int) = Font(
    resId,
    weight = FontWeight(nilai),
    variationSettings = FontVariation.Settings(FontVariation.weight(nilai)),
)

val Fredoka = FontFamily(berat(R.font.fredoka, 600), berat(R.font.fredoka, 700))
val Nunito = FontFamily(berat(R.font.nunito, 600), berat(R.font.nunito, 700), berat(R.font.nunito, 800))

/** Gaya teks per peran. Ukuran dalam sp supaya ikut ukuran huruf sistem. */
object Huruf {
    val judulLayar = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W700, fontSize = 32.sp)
    val judulStrip = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W700, fontSize = 24.sp)
    val judulDetail = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W700, fontSize = 22.sp)
    val angkaUbin = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W700, fontSize = 20.sp)
    val judulBagian = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W700, fontSize = 18.sp)
    val tombol = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W600, fontSize = 16.sp)
    val kolomJudul = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.W800, fontSize = 18.sp)
    val judulKartu = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.W800, fontSize = 15.sp)
    val isi = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.W600, fontSize = 15.sp, lineHeight = 1.5.em)
    val meta = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.W700, fontSize = 12.sp)
    val label = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.W800, fontSize = 11.sp, letterSpacing = 0.08.em)
    val chip = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.W800, fontSize = 11.sp, letterSpacing = 0.04.em)
}
