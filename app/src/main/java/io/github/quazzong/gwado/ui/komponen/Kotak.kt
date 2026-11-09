package io.github.quazzong.gwado.ui.komponen

import io.github.quazzong.gwado.ui.tema.gtween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.ui.tema.Gerak
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Jarak

/** Garis terang di dalam garis luar. */
sealed interface Kilau {
    /** Pita 2dp mengelilingi sisi dalam. */
    data class Cincin(val warna: Color) : Kilau

    /** Pita di sisi atas saja, setebal `tebal`. */
    data class Atas(val tebal: Dp, val warna: Color) : Kilau
}

/** Outline bentuk yang diperkecil `masuk` px di tiap sisi, sebagai Path di posisi aslinya. */
internal fun Shape.pathMasuk(ukuran: Size, masuk: Float, arah: LayoutDirection, density: Density): Path {
    val dalam = Size(
        (ukuran.width - 2 * masuk).coerceAtLeast(0f),
        (ukuran.height - 2 * masuk).coerceAtLeast(0f),
    )
    return Path().apply {
        addOutline(createOutline(dalam, arah, density))
        translate(Offset(masuk, masuk))
    }
}

/**
 * Permukaan dasar: isi, garis 2dp (bisa putus-putus), kilau, dan bayangan keras.
 * Kalau bisa diketuk, permukaan bergeser ke arah bayangan selama ditekan.
 * Ruang bayangan sudah termasuk dalam ukuran komponen (kanan dan bawah). bayanganKedua
 * digambar di bawah bayangan utama (misalnya lavender di header).
 */
@Composable
fun Kotak(
    modifier: Modifier = Modifier,
    bentuk: Shape = Bentuk.kartu,
    latar: Brush = SolidColor(GwadoTema.warna.kartu),
    garis: Color = GwadoTema.warna.garis,
    putus: Boolean = false,
    kilau: Kilau? = Kilau.Cincin(GwadoTema.warna.kilau),
    bayangan: Dp = Jarak.bayangan,
    warnaBayangan: Color = GwadoTema.warna.bayangan,
    bayanganKedua: Dp = 0.dp,
    warnaBayanganKedua: Color = GwadoTema.warna.lavender,
    padding: PaddingValues = PaddingValues(Jarak.dalamKartu),
    rataIsi: Alignment = Alignment.TopStart,
    onClick: (() -> Unit)? = null,
    peran: Role = Role.Button,
    isi: @Composable BoxScope.() -> Unit,
) {
    val sumber = remember { MutableInteractionSource() }
    val ditekan by sumber.collectIsPressedAsState()
    val geser by animateDpAsState(
        targetValue = if (ditekan && onClick != null) bayangan else 0.dp,
        animationSpec = gtween(Gerak.TEKAN),
        label = "geser",
    )
    val klik = if (onClick != null) {
        Modifier.clickable(interactionSource = sumber, indication = null, role = peran, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(klik)
            .padding(end = maxOf(bayangan, bayanganKedua), bottom = maxOf(bayangan, bayanganKedua))
            .drawWithCache {
                val p = bentuk.pathMasuk(size, 0f, layoutDirection, this)
                val b = bayangan.toPx()
                val b2 = bayanganKedua.toPx()
                onDrawBehind {
                    if (b2 > 0f) translate(b2, b2) { drawPath(p, warnaBayanganKedua) }
                    if (b > 0f) translate(b, b) { drawPath(p, warnaBayangan) }
                }
            }
            .graphicsLayer {
                translationX = geser.toPx()
                translationY = geser.toPx()
            }
            .drawWithCache {
                val g = Jarak.garis.toPx()
                val luar = bentuk.pathMasuk(size, 0f, layoutDirection, this)
                val tengahGaris = bentuk.pathMasuk(size, g / 2, layoutDirection, this)
                // Kilau cincin = garis selebar g di tengah pita g..2g. Satu bentuk bergaris
                // jauh lebih ringan digambar GPU daripada selisih dua bentuk.
                val cincin = if (kilau is Kilau.Cincin) bentuk.pathMasuk(size, g * 1.5f, layoutDirection, this) else null
                val gayaKilau = Stroke(width = g)
                val pita = when (kilau) {
                    is Kilau.Cincin -> null
                    is Kilau.Atas -> Path.combine(
                        PathOperation.Difference,
                        bentuk.pathMasuk(size, g, layoutDirection, this),
                        bentuk.pathMasuk(size, g, layoutDirection, this)
                            .apply { translate(Offset(0f, kilau.tebal.toPx())) },
                    )
                    null -> null
                }
                val warnaKilau = when (kilau) {
                    is Kilau.Cincin -> kilau.warna
                    is Kilau.Atas -> kilau.warna
                    null -> Color.Transparent
                }
                val gaya = Stroke(
                    width = g,
                    pathEffect = if (putus) PathEffect.dashPathEffect(floatArrayOf(g * 3, g * 2)) else null,
                )
                onDrawBehind {
                    drawPath(luar, latar)
                    if (cincin != null) drawPath(cincin, warnaKilau, style = gayaKilau)
                    if (pita != null) drawPath(pita, warnaKilau)
                    drawPath(tengahGaris, garis, style = gaya)
                }
            }
            .padding(padding),
        contentAlignment = rataIsi,
        content = isi,
    )
}
