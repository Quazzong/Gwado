package io.github.quazzong.gwado.ui.komponen

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * Taburan bentuk kecil di strip judul, memudar ke kiri mulai dari pecahan [mulai] lebar.
 * Tata letak dalam satuan bidang 320 × [tinggi] yang menutupi kotak dan rata kanan.
 */
@Composable
fun Taburan(benih: Int, modifier: Modifier = Modifier, tinggi: Float = 110f, mulai: Float = 0.25f) {
    val warna = GwadoTema.warna.taburan
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val u = 1.dp.toPx()
        val sk = maxOf(size.width / (320f * u), size.height / (tinggi * u)) * u
        val ox = size.width - 320f * sk
        val oy = (size.height - tinggi * sk) / 2
        val r = Acak(benih)
        for (gx in 0 until 9) {
            for (gy in 0 until ceil(tinggi / 24f).toInt()) {
                val (n, a) = JENIS[(r.berikut() * 4).toInt().coerceAtMost(3)]
                val x = gx * 36 + 12 + r.berikut() * 14
                val y = gy * 24 + 8 + r.berikut() * 10
                val rr = 3.5f + r.berikut() * 3.2f
                val putar = r.berikut() * 6.28f
                drawPath(pathGelombang(Offset(ox + x * sk, oy + y * sk), rr * sk, n, a, putar), warna)
            }
        }
        drawRect(
            Brush.horizontalGradient(mulai to Color.Transparent, (mulai + 0.5f).coerceAtMost(1f) to Color.Black),
            blendMode = BlendMode.DstIn,
        )
    }
}
