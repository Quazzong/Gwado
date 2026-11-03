package io.github.quazzong.gwado.ui.komponen

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Bentuk bergelombang r(t) = r * (1 + a * cos(n * t)), diputar -90 derajat
 * supaya lekuk pertama ada di atas.
 */
fun pathGelombang(
    pusat: Offset,
    r: Float,
    n: Int,
    a: Float,
    putar: Float = -PI.toFloat() / 2,
    langkah: Int = 72,
): Path = Path().apply {
    for (i in 0..langkah) {
        val t = i.toFloat() / langkah * 2 * PI.toFloat()
        val rr = r * (1 + a * cos(n * t))
        val x = pusat.x + rr * cos(t + putar)
        val y = pusat.y + rr * sin(t + putar)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Shape dari pathGelombang, diskalakan supaya lekuk terluar menyentuh tepi kotak. */
class BentukGelombang(private val n: Int, private val a: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = min(size.width, size.height) / 2f / (1 + a)
        return Outline.Generic(pathGelombang(Offset(size.width / 2, size.height / 2), r, n, a))
    }
}

/** Motif bentuk khas aplikasi. */
object Motif {
    val kue = BentukGelombang(9, 0.11f)
    val semanggi = BentukGelombang(4, 0.29f)
    val bunga = BentukGelombang(6, 0.18f)
    val segitiga = BentukGelombang(3, 0.16f)
    val pil = RoundedCornerShape(50)
}
