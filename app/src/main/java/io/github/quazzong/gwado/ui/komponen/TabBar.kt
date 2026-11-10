package io.github.quazzong.gwado.ui.komponen

import io.github.quazzong.gwado.ui.tema.gtween
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.ui.tema.Gerak
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
    val lompat = remember { Animatable(0f) }
    var pertama by remember { mutableStateOf(true) }
    LaunchedEffect(aktif) {
        if (aktif) {
            letup.snapTo(0f)
            launch { letup.animateTo(1f, gtween(Gerak.LETUP, easing = Gerak.Letup)) }
            // Ikon tab yang baru dipilih melompat sekali (bukan saat layar pertama dibuka).
            if (!pertama) {
                lompat.animateTo(1f, gtween(Gerak.LOMPAT_IKON / 3, easing = Gerak.Keluar))
                lompat.animateTo(0f, gtween(Gerak.LOMPAT_IKON * 2 / 3, easing = Gerak.Pegas))
            }
        } else {
            letup.snapTo(0f)
            lompat.snapTo(0f)
        }
        pertama = false
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
                tint = if (aktif) w.diAtasAksen else w.tintaRedup,
                modifier = Modifier.size(21.dp).graphicsLayer {
                    translationY = -6.dp.toPx() * lompat.value
                    scaleX = 1f + 0.3f * lompat.value
                    scaleY = 1f + 0.3f * lompat.value
                },
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
