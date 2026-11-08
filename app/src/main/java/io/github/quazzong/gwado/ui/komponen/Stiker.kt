package io.github.quazzong.gwado.ui.komponen

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
 * menurut mode gelap sistem (`values-night`), karena widget selalu ikut tema sistem.
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
    Image(painterResource(gambar), contentDescription = null, modifier = modifier.size(ukuran))
}
