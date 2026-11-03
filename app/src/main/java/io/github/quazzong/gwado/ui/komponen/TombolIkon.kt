package io.github.quazzong.gwado.ui.komponen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Jarak

/** Tombol plum persegi berisi ikon saja (kembali, buka di situs). label dibacakan pembaca layar. */
@Composable
fun TombolPlumIkon(@DrawableRes ikon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Kotak(
        modifier.size(Jarak.sentuh + Jarak.bayangan),
        bentuk = Bentuk.tombol,
        latar = SolidColor(w.plum),
        kilau = Kilau.Atas(2.dp, Color.White.copy(alpha = 0.18f)),
        padding = PaddingValues(0.dp),
        rataIsi = Alignment.Center,
        onClick = onClick,
    ) {
        Icon(painterResource(ikon), contentDescription = label, tint = w.diAtasPlum, modifier = Modifier.size(20.dp))
    }
}
