package io.github.quazzong.gwado.ui.komponen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Warna

/** Gradasi aksen untuk bentuk kue (lencana, ikon selesai, radio). */
fun Warna.gradasiAksen(): Brush =
    Brush.verticalGradient(0f to aksenTerang, 0.6f to aksen, 1f to aksenGelap)

/**
 * Ikon bentuk per layanan: Brone kue, Eling semanggi, Classroom segitiga,
 * manual (sumber null) pil, layanan lain bunga.
 */
@Composable
fun IkonLayanan(sumber: String?, modifier: Modifier = Modifier, ukuran: Dp = 14.dp) {
    val w = GwadoTema.warna
    Canvas(modifier.size(ukuran)) {
        val s = size.minDimension / 18f
        val gaya = Stroke(1.8f * s, join = StrokeJoin.Round)
        val path = when (sumber) {
            null -> null
            "Brone" -> pathGelombang(Offset(9 * s, 9 * s), 6.5f * s, 9, 0.13f)
            "Eling" -> pathGelombang(Offset(9 * s, 9 * s), 6f * s, 4, 0.3f)
            "Google Classroom" -> pathGelombang(Offset(9 * s, 9.8f * s), 6.6f * s, 3, 0.2f)
            else -> pathGelombang(Offset(9 * s, 9 * s), 6.4f * s, 6, 0.18f)
        }
        if (path != null) {
            drawPath(path, w.ikonLayanan)
            drawPath(path, w.garis, style = gaya)
        } else {
            val kiriAtas = Offset(2 * s, 5.5f * s)
            val ukuranPil = Size(14 * s, 7 * s)
            val sudut = CornerRadius(3.5f * s)
            drawRoundRect(w.ikonLayanan, kiriAtas, ukuranPil, sudut)
            drawRoundRect(w.garis, kiriAtas, ukuranPil, sudut, style = gaya)
        }
    }
}

/** Kue bergradasi aksen dengan centang, penanda tugas selesai. */
@Composable
fun IkonSelesai(modifier: Modifier = Modifier, ukuran: Dp = 26.dp) {
    val w = GwadoTema.warna
    val isi = w.gradasiAksen()
    Canvas(modifier.size(ukuran).semantics { contentDescription = "Selesai" }) {
        val s = size.minDimension / 24f
        val kue = pathGelombang(Offset(12 * s, 12 * s), 9.5f * s, 9, 0.1f)
        drawPath(kue, isi)
        drawPath(kue, w.garis, style = Stroke(2 * s))
        val centang = Path().apply {
            moveTo(7.6f * s, 12.4f * s)
            relativeLineTo(3 * s, 3 * s)
            relativeLineTo(5.6f * s, -6.6f * s)
        }
        drawPath(
            centang,
            w.diAtasAksen,
            style = Stroke(2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
