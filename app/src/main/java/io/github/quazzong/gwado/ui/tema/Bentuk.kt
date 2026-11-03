package io.github.quazzong.gwado.ui.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Radius sudut per jenis komponen. */
object Bentuk {
    val chip = RoundedCornerShape(8.dp)
    val ubin = RoundedCornerShape(10.dp)
    val tombol = RoundedCornerShape(12.dp)
    val kartu = RoundedCornerShape(14.dp)
    val header = RoundedCornerShape(16.dp)
    val tabBar = RoundedCornerShape(18.dp)
    val lembarBawah = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val slotStiker = RoundedCornerShape(20.dp)
}

/** Jarak, tebal garis, bayangan keras, dan target sentuh. */
object Jarak {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val tepiLayar = 16.dp
    val dalamKartu = 12.dp
    val garis = 2.dp
    val bayangan = 3.dp
    val bayanganKecil = 2.dp
    val sentuh = 48.dp
}
