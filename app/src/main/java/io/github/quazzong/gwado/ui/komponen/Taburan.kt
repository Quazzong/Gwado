package io.github.quazzong.gwado.ui.komponen

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.ui.tema.GwadoTema
import kotlin.math.ceil

/** Bilangan acak semu Park–Miller: benih sama selalu menghasilkan urutan yang sama. */
private class Acak(benih: Int) {
    private var s = (benih.toLong() and 0x7fffffffL).coerceAtLeast(1L)
    fun berikut(): Float {
        s = s * 16807 % 2147483647
        return (s.toDouble() / 2147483647).toFloat()
    }
}

private val JENIS = listOf(9 to 0.12f, 4 to 0.28f, 6 to 0.2f, 3 to 0.18f)

/** Satu bentuk taburan dalam satuan bidang 320 × tinggi: path dan posisi x pusatnya. */
private class BentukTabur(val path: Path, val x: Float)

/**
 * Taburan bentuk kecil di strip judul, memudar ke kiri mulai dari pecahan [mulai] lebar.
 * Tata letak dalam satuan bidang 320 × [tinggi] yang menutupi kotak dan rata kanan.
 * Path dibuat sekali (satuan bidang); tiap gambar hanya diskalakan, jadi murah digambar ulang
 * saat ukurannya berubah (misalnya kepala detail yang sedang melebar). Pudar ke kiri dihitung
 * per bentuk dari posisinya, tanpa lapisan offscreen.
 */
@Composable
fun Taburan(benih: Int, modifier: Modifier = Modifier, tinggi: Float = 110f, mulai: Float = 0.25f) {
    val warna = GwadoTema.warna.taburan
    val bentuk = remember(benih, tinggi) {
        val r = Acak(benih)
        buildList {
            for (gx in 0 until 9) {
                for (gy in 0 until ceil(tinggi / 24f).toInt()) {
                    val (n, a) = JENIS[(r.berikut() * 4).toInt().coerceAtMost(3)]
                    val x = gx * 36 + 12 + r.berikut() * 14
                    val y = gy * 24 + 8 + r.berikut() * 10
                    val rr = 3.5f + r.berikut() * 3.2f
                    val putar = r.berikut() * 6.28f
                    add(BentukTabur(pathGelombang(Offset(x, y), rr, n, a, putar), x))
                }
            }
        }
    }
    Canvas(modifier) {
        val u = 1.dp.toPx()
        val sk = maxOf(size.width / (320f * u), size.height / (tinggi * u)) * u
        val ox = size.width - 320f * sk
        val oy = (size.height - tinggi * sk) / 2
        translate(ox, oy) {
            scale(sk, sk, Offset.Zero) {
                for (b in bentuk) {
                    val fx = (ox + b.x * sk) / size.width
                    val alpha = ((fx - mulai) / 0.5f).coerceIn(0f, 1f)
                    if (alpha > 0f) drawPath(b.path, warna, alpha = alpha)
                }
            }
        }
    }
}
