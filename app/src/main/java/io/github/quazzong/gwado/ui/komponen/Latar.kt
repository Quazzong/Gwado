package io.github.quazzong.gwado.ui.komponen

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.tampilan.Tekstur
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Warna
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/** Tekstur yang dipilih pengguna; disediakan MainActivity dari preferences. */
val LocalTekstur = staticCompositionLocalOf { Tekstur.BAWAAN }

/** Satu bentuk latar acak: posisi relatif (0..1) terhadap layar, ukuran dalam dp. */
private class BentukLatar(
    val x: Float,
    val y: Float,
    val r: Float,
    val n: Int,
    val a: Float,
    val putar: Float,
    val pink: Boolean,
    val pil: Boolean,
)

/** [n, a] untuk kue, semanggi, bunga, segitiga bulat (DESAIN §6). */
private val JENIS_BENTUK = listOf(9 to 0.11f, 4 to 0.29f, 6 to 0.18f, 3 to 0.16f)

/** Diacak sekali tiap proses aplikasi dibuka, tanpa seed tersimpan (DESAIN §7). */
private val BENTUK_ACAK: List<BentukLatar> by lazy {
    val r = Random.Default
    List(11) {
        val (n, a) = JENIS_BENTUK[r.nextInt(JENIS_BENTUK.size)]
        BentukLatar(
            x = r.nextFloat(), y = r.nextFloat(), r = 24f + r.nextFloat() * 46f, n = n, a = a,
            putar = r.nextFloat() * 360f, pink = r.nextFloat() < 0.3f, pil = r.nextFloat() < 0.12f,
        )
    }
}

/** Ubin berulang satu titik atau satu sudut kotak, dipakai sebagai kuas yang diulang ke seluruh layar. */
private fun kuasUbin(jarak: Int, gambar: Canvas.(Paint) -> Unit, warna: Color): ShaderBrush {
    val ubin = ImageBitmap(jarak, jarak)
    Canvas(ubin).gambar(Paint().apply { color = warna; isAntiAlias = true })
    return ShaderBrush(ImageShader(ubin, TileMode.Repeated, TileMode.Repeated))
}

/**
 * Gambar tekstur [t] di belakang isi. [skalaBentuk] mengecilkan bentuk acak untuk ubin pratinjau.
 * Pola dibuat sekali per ukuran (drawWithCache), jadi menggulir tidak membuatnya ulang.
 */
fun Modifier.tekstur(t: Tekstur, w: Warna, skalaBentuk: Float = 1f): Modifier = drawWithCache {
    when (t) {
        Tekstur.POLOS -> onDrawBehind {}
        Tekstur.TITIK -> {
            val jarak = max(1, 12.dp.toPx().roundToInt())
            val kuas = kuasUbin(jarak, { p -> drawCircle(Offset(jarak / 2f, jarak / 2f), 1.3.dp.toPx(), p) }, w.pola)
            onDrawBehind { drawRect(kuas) }
        }
        Tekstur.KOTAK -> {
            val jarak = max(1, 14.dp.toPx().roundToInt())
            val tebal = 1.dp.toPx()
            val kuas = kuasUbin(jarak, { p ->
                drawRect(0f, 0f, jarak.toFloat(), tebal, p)
                drawRect(0f, tebal, tebal, jarak.toFloat(), p)
            }, w.pola)
            onDrawBehind { drawRect(kuas) }
        }
        Tekstur.BENTUK -> {
            val gambar = BENTUK_ACAK.map { s ->
                val pusat = Offset(s.x * size.width, s.y * size.height)
                val r = (s.r * skalaBentuk).dp.toPx()
                Triple(s, pusat, if (s.pil) null else pathGelombang(pusat, r, s.n, s.a))
            }
            onDrawBehind {
                for ((s, pusat, jalur) in gambar) {
                    val warna = if (s.pink) w.bentukLatarPink else w.bentukLatar
                    rotate(s.putar, pusat) {
                        if (jalur != null) {
                            drawPath(jalur, warna)
                        } else {
                            val r = (s.r * skalaBentuk).dp.toPx()
                            drawRoundRect(
                                warna,
                                topLeft = Offset(pusat.x - r * 0.95f, pusat.y - r * 0.4f),
                                size = Size(r * 1.9f, r * 0.8f),
                                cornerRadius = CornerRadius(r * 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Latar layar penuh: warna latar + tekstur pilihan pengguna. Tidak pernah dipakai di belakang teks kartu. */
@Composable
fun Modifier.latarLayar(): Modifier {
    val w = GwadoTema.warna
    return background(w.latar).tekstur(LocalTekstur.current, w)
}
