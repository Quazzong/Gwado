package io.github.quazzong.gwado.ui.komponen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.ui.tema.Fredoka
import io.github.quazzong.gwado.ui.tema.GwadoTema

/**
 * Kue bergerigi berisi teks (misalnya tanggal "4" di atas "Nov"). Versi putus-putus
 * tanpa isi warna dan tanpa bayangan, untuk tugas tanpa deadline.
 */
@Composable
fun LencanaKue(
    atas: String,
    bawah: String?,
    modifier: Modifier = Modifier,
    ukuran: Dp = 68.dp,
    putus: Boolean = false,
) {
    val w = GwadoTema.warna
    val isi = w.gradasiAksen()
    val warnaTeks = if (putus) w.tinta else Color(0xFF3A1D36)
    Box(modifier.size(ukuran)) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension / 70f
            val kue = pathGelombang(Offset(33 * s, 33 * s), 28.5f * s, 9, 0.09f)
            if (putus) {
                drawPath(kue, w.kartu)
                drawPath(
                    kue, w.garis,
                    style = Stroke(2.2f * s, join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5 * s, 3.5f * s))),
                )
            } else {
                translate(3 * s, 3 * s) { drawPath(kue, w.bayangan) }
                drawPath(kue, isi)
                drawPath(kue, w.garis, style = Stroke(2.2f * s, join = StrokeJoin.Round))
                drawPath(
                    pathGelombang(Offset(33 * s, 33 * s), 23f * s, 9, 0.09f),
                    Color.White.copy(alpha = 0.7f),
                    style = Stroke(1.5f * s),
                )
            }
        }
        Column(
            // Sedikit di atas pusat kue: jarak di atas angka dan di bawah bulan sama.
            Modifier.align(Alignment.Center).offset(x = (-2).dp, y = (-3.5).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(atas, style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W700, fontSize = 24.sp, lineHeight = 26.sp), color = warnaTeks)
            if (bawah != null) {
                Text(bawah, style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W600, fontSize = 11.sp, lineHeight = 12.sp), color = warnaTeks)
            }
        }
    }
}
