package io.github.quazzong.gwado.ui.komponen

import io.github.quazzong.gwado.ui.tema.gtween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.ui.tema.Gerak
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.WarnaStatus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sepuluh bentuk kecil meletup keluar dari tengah lalu memudar (520 ms) setiap kali
 * [pemicu] bertambah. Tidak menangkap sentuhan; diam kalau animasi sistem dimatikan.
 */
@Composable
fun Letupan(pemicu: Int, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    val maju = remember { Animatable(1f) }
    LaunchedEffect(pemicu) {
        if (pemicu > 0) {
            maju.snapTo(0f)
            maju.animateTo(1f, gtween(Gerak.RAYAKAN, easing = FastOutSlowInEasing))
        }
    }
    val warna = listOf(w.aksen, w.lavender, WarnaStatus.mepet[1], w.aksenTerang)
    val bentuk = listOf(9 to 0.12f, 4 to 0.28f, 6 to 0.2f, 3 to 0.18f)
    Canvas(modifier) {
        val p = maju.value
        if (p >= 1f) return@Canvas
        val acak = Random(pemicu)
        val alpha = 1f - p
        repeat(10) { i ->
            val sudut = i / 10f * 2 * PI.toFloat() + acak.nextFloat() * 0.5f
            val jarak = (40 + acak.nextFloat() * 30).dp.toPx() * p
            val r = (4 + acak.nextFloat() * 3).dp.toPx() * (1 - 0.4f * p)
            val (n, a) = bentuk[i % 4]
            val path = pathGelombang(
                Offset(center.x + cos(sudut) * jarak, center.y + sin(sudut) * jarak), r, n, a, putar = p * 3f + i,
            )
            drawPath(path, warna[i % 4].copy(alpha = alpha))
            drawPath(path, w.garis.copy(alpha = alpha), style = Stroke(1.5.dp.toPx()))
        }
    }
}
