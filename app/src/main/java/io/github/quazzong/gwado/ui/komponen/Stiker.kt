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

/** Ekspresi maskot kue; gambarnya vector drawable `stiker_*`. */
enum class Ekspresi(@param:DrawableRes val gambar: Int) {
    NETRAL(R.drawable.stiker_netral),
    MELAMBAI(R.drawable.stiker_melambai),
    SANTAI(R.drawable.stiker_santai),
    BERSORAK(R.drawable.stiker_bersorak),
    BINGUNG(R.drawable.stiker_bingung),
    TIDUR(R.drawable.stiker_tidur),
}

/** Stiker maskot sebagai hiasan: tidak dibacakan pembaca layar, maknanya sudah ada di teks di sebelahnya. */
@Composable
fun Stiker(ekspresi: Ekspresi, modifier: Modifier = Modifier, ukuran: Dp = 72.dp) {
    Image(painterResource(ekspresi.gambar), contentDescription = null, modifier = modifier.size(ukuran))
}
