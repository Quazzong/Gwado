package io.github.quazzong.gwado.ui.komponen

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Nunito
import io.github.quazzong.gwado.ui.tema.WarnaStatus

/** Satu tab: kunci, label, ikon, dan lencana opsional (angka atau "!"). */
data class ItemTab<T>(
    val kunci: T,
    val label: String,
    @DrawableRes val ikon: Int,
    val lencana: String? = null,
)

/** Tab bar bawah. Tab aktif ditandai kue yang meletup di belakang ikon. */
@Composable
fun <T> TabBar(item: List<ItemTab<T>>, aktif: T, onPilih: (T) -> Unit, modifier: Modifier = Modifier) {
    Kotak(modifier.fillMaxWidth(), bentuk = Bentuk.tabBar, padding = PaddingValues(4.dp)) {
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            item.forEach { t ->
                SatuTab(t, t.kunci == aktif, { onPilih(t.kunci) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun <T> SatuTab(t: ItemTab<T>, aktif: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val w = GwadoTema.warna
    val isiKue = w.gradasiAksen()
    val letup = remember { Animatable(if (aktif) 1f else 0f) }
    LaunchedEffect(aktif) {
        if (aktif) {
            letup.snapTo(0f)
            letup.animateTo(1f, tween(260, easing = CubicBezierEasing(0.34f, 1.6f, 0.64f, 1f)))
        } else {
            letup.snapTo(0f)
        }
    }
    Column(
        modifier
            .height(54.dp)
            .selectable(
                selected = aktif,
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(40.dp, 32.dp), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .requiredSize(42.dp)
                    .graphicsLayer {
                        scaleX = letup.value
                        scaleY = letup.value
                        rotationZ = -60f * (1f - letup.value)
                    },
            ) {
                val s = size.minDimension / 42f
                val kue = pathGelombang(Offset(21 * s, 21 * s), 16 * s, 9, 0.1f)
                translate(2 * s, 2 * s) { drawPath(kue, w.bayangan) }
                drawPath(kue, isiKue)
                drawPath(kue, w.garis, style = Stroke(2 * s))
            }
            Icon(
                painterResource(t.ikon),
                contentDescription = null,
                tint = if (aktif) Color(0xFF3A1D36) else w.tintaRedup,
                modifier = Modifier.size(21.dp),
            )
            t.lencana?.let {
                LencanaTab(it, Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp))
            }
        }
        Text(
            t.label,
            style = Huruf.meta.copy(fontSize = 11.sp, fontWeight = FontWeight.W800),
            color = if (aktif) w.tinta else w.tintaRedup,
        )
    }
}

/** Lencana merah kecil di pojok ikon tab. */
@Composable
private fun LencanaTab(teks: String, modifier: Modifier = Modifier) {
    Kotak(
        modifier.defaultMinSize(minWidth = 16.dp, minHeight = 16.dp),
        bentuk = RoundedCornerShape(8.dp),
        latar = Brush.verticalGradient(WarnaStatus.lewat),
        kilau = null,
        bayangan = 0.dp,
        padding = PaddingValues(horizontal = 4.dp),
        rataIsi = Alignment.Center,
    ) {
        Text(
            teks,
            style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.W800, fontSize = 10.sp, lineHeight = 12.sp),
            color = WarnaStatus.diAtasLewat,
        )
    }
}
