package io.github.quazzong.gwado.ui.komponen

import io.github.quazzong.gwado.ui.tema.gtween
import io.github.quazzong.gwado.ui.tema.Gerak
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.getValue
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.Fredoka
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import kotlin.math.sqrt

/**
 * Judul ala stiker: isi warna stiker, garis tepi tebal, dan bayangan keras di bawahnya.
 * Hanya untuk judul layar, bukan judul tugas.
 */
@Composable
fun TeksStiker(teks: String, modifier: Modifier = Modifier, ukuran: TextUnit = 34.sp, garis: Dp = 2.4.dp) {
    val w = GwadoTema.warna
    val tebal = with(LocalDensity.current) { garis.toPx() } * 2
    val dasar = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W700, fontSize = ukuran, lineHeight = ukuran)
    val tepi = dasar.copy(drawStyle = Stroke(tebal, join = StrokeJoin.Round))
    val gx = if (garis > 2.3.dp) 3.dp else 2.dp
    val gy = if (garis > 2.3.dp) 4.dp else 3.dp
    Box(modifier) {
        Text(teks, style = dasar, color = w.bayangan, modifier = Modifier.offset(gx, gy).clearAndSetSemantics {})
        Text(teks, style = tepi, color = w.garisStiker, modifier = Modifier.clearAndSetSemantics {})
        Text(teks, style = dasar, color = w.stiker)
    }
}

/** Batang progres bergaris-garis permen. pecahan 0..1. */
@Composable
fun BatangProgres(pecahan: Float, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    // Isi memanjang/memendek ke nilai baru (tanpa gerak saat pertama tampil).
    val p by animateFloatAsState(pecahan.coerceIn(0f, 1f), gtween(Gerak.PINDAH, easing = Gerak.Pindah), label = "progres")
    Canvas(
        modifier
            .fillMaxWidth()
            .height(12.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) },
    ) {
        val g = 2.dp.toPx()
        val r = CornerRadius(7.dp.toPx())
        drawRoundRect(w.ubin, cornerRadius = r)
        val lebar = size.width * p
        if (lebar > 0f) {
            val langkah = 10.dp.toPx() / sqrt(2f)
            val garisGaris = Brush.linearGradient(
                0f to w.aksen, 0.5f to w.aksen, 0.5f to w.aksenTerang, 1f to w.aksenTerang,
                start = Offset.Zero, end = Offset(langkah, langkah), tileMode = TileMode.Repeated,
            )
            clipRect(right = lebar) {
                drawRoundRect(garisGaris, cornerRadius = r)
                drawRect(Color.White.copy(alpha = 0.55f), topLeft = Offset(0f, g), size = Size(lebar, g))
            }
            if (lebar < size.width - g) drawRect(w.garis, topLeft = Offset(lebar - g / 2, 0f), size = Size(g, size.height))
        }
        drawRoundRect(
            w.garis,
            topLeft = Offset(g / 2, g / 2),
            size = Size(size.width - g, size.height - g),
            cornerRadius = CornerRadius(r.x - g / 2),
            style = Stroke(g),
        )
    }
}

/** Ubin statistik: label kapital kecil + angka. Angka panjang mengecil supaya muat satu baris. */
@Composable
fun Ubin(label: String, nilai: String, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Kotak(
        modifier,
        bentuk = Bentuk.ubin,
        latar = SolidColor(w.ubin),
        bayangan = Jarak.bayanganKecil,
        padding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Column {
            Text(label.uppercase(), style = Huruf.label, color = w.tintaRedup, maxLines = 1)
            // Angka berganti: yang lama bergulir keluar, yang baru masuk dari bawah (naik) atau dari atas (turun).
            AnimatedContent(
                targetState = nilai,
                modifier = Modifier.clipToBounds(),
                transitionSpec = {
                    val naik = (targetState.toIntOrNull() ?: 0) >= (initialState.toIntOrNull() ?: 0)
                    val gerak = gtween<IntOffset>(Gerak.PINDAH, easing = Gerak.Pindah)
                    (slideInVertically(gerak) { if (naik) it else -it } + fadeIn(gtween(Gerak.PINDAH))) togetherWith
                        (slideOutVertically(gerak) { if (naik) -it else it } + fadeOut(gtween(Gerak.PINDAH))) using
                        SizeTransform(clip = false)
                },
                label = "angka-ubin",
            ) { angka ->
                BasicText(
                    angka,
                    style = Huruf.angkaUbin.copy(color = w.tinta),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 20.sp),
                )
            }
        }
    }
}

/** Pilihan bersegmen dalam satu kotak bergaris; segmen aktif bergradasi aksen. */
@Composable
fun <T> Segmen(pilihan: List<Pair<T, String>>, aktif: T, onPilih: (T) -> Unit, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Kotak(
        modifier.fillMaxWidth(),
        bentuk = Bentuk.ubin,
        kilau = null,
        bayangan = 0.dp,
        padding = PaddingValues(Jarak.garis),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(8.dp))
                .selectableGroup(),
        ) {
            pilihan.forEachIndexed { i, (kunci, label) ->
                if (i > 0) Box(Modifier.width(Jarak.garis).fillMaxHeight().background(w.garis))
                val on = kunci == aktif
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .then(
                            if (on) {
                                Modifier
                                    .background(Brush.verticalGradient(0f to w.aksenTerang, 0.6f to w.aksen, 1f to w.aksenGelap))
                                    .drawBehind { drawRect(Color.White.copy(alpha = 0.6f), size = Size(size.width, 2.dp.toPx())) }
                            } else {
                                Modifier
                            },
                        )
                        .selectable(selected = on, role = Role.RadioButton, onClick = { onPilih(kunci) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = Huruf.meta.copy(fontSize = 11.sp, fontWeight = FontWeight.W800),
                        color = if (on) w.diAtasAksen else w.tinta,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
