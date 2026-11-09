package io.github.quazzong.gwado.ui.komponen

import io.github.quazzong.gwado.ui.tema.gtween
import io.github.quazzong.gwado.ui.tema.Gerak
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.ui.tema.GwadoTema

/**
 * Ekspresi maskot kue; gambarnya vector drawable `stiker_*`. Versi `_gelap` memakai garis dan
 * hiasan terang supaya terlihat di latar gelap. [widget] = alias yang dipilih launcher sendiri
 * menurut mode gelap sistem (`values-night`), dipakai widget saat tema Gwado ikut sistem.
 */
enum class Ekspresi(
    @param:DrawableRes val gambar: Int,
    @param:DrawableRes val gambarGelap: Int,
    @param:DrawableRes val widget: Int,
) {
    NETRAL(R.drawable.stiker_netral, R.drawable.stiker_netral_gelap, R.drawable.widget_stiker_netral),
    MELAMBAI(R.drawable.stiker_melambai, R.drawable.stiker_melambai_gelap, R.drawable.widget_stiker_melambai),
    SANTAI(R.drawable.stiker_santai, R.drawable.stiker_santai_gelap, R.drawable.widget_stiker_santai),
    BERSORAK(R.drawable.stiker_bersorak, R.drawable.stiker_bersorak_gelap, R.drawable.widget_stiker_bersorak),
    BINGUNG(R.drawable.stiker_bingung, R.drawable.stiker_bingung_gelap, R.drawable.widget_stiker_bingung),
    TIDUR(R.drawable.stiker_tidur, R.drawable.stiker_tidur_gelap, R.drawable.widget_stiker_tidur),
}

/** Stiker maskot sebagai hiasan: tidak dibacakan pembaca layar, maknanya sudah ada di teks di sebelahnya. */
@Composable
fun Stiker(ekspresi: Ekspresi, modifier: Modifier = Modifier, ukuran: Dp = 72.dp) {
    val gambar = if (GwadoTema.gelap) ekspresi.gambarGelap else ekspresi.gambar
    // Meletup sekali setiap kali muncul (skala 0,8 → 1 + memudar masuk); tidak bergoyang sesudahnya.
    val muncul = remember { Animatable(0f) }
    LaunchedEffect(Unit) { muncul.animateTo(1f, gtween(Gerak.LETUP, easing = Gerak.Letup)) }
    Image(
        painterResource(gambar),
        contentDescription = null,
        modifier = modifier.size(ukuran).graphicsLayer {
            val v = muncul.value
            scaleX = 0.8f + 0.2f * v
            scaleY = 0.8f + 0.2f * v
            alpha = v.coerceIn(0f, 1f)
        },
    )
}
