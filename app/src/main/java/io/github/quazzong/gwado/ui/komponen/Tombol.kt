package io.github.quazzong.gwado.ui.komponen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak

/**
 * Tombol utama bergradasi aksen. Satu per layar. lepas = versi tanpa isi warna untuk aksi
 * kebalikan (misalnya Batalkan selesai), tetap berbayangan.
 */
@Composable
fun TombolPermen(teks: String, onClick: () -> Unit, modifier: Modifier = Modifier, lepas: Boolean = false) {
    val w = GwadoTema.warna
    Kotak(
        modifier = modifier.defaultMinSize(minHeight = Jarak.sentuh + Jarak.bayangan),
        bentuk = Bentuk.tombol,
        latar = if (lepas) SolidColor(w.kartu)
        else Brush.verticalGradient(0f to w.aksenTerang, 0.55f to w.aksen, 1f to w.aksenGelap),
        kilau = if (lepas) null else Kilau.Atas(2.dp, Color.White.copy(alpha = 0.7f)),
        padding = PaddingValues(horizontal = Jarak.s16, vertical = Jarak.s8),
        rataIsi = Alignment.Center,
        onClick = onClick,
    ) {
        Text(teks, style = Huruf.tombol, color = if (lepas) w.tinta else w.diAtasAksen)
    }
}

/** Tombol kedua berisi plum, untuk +, kembali, buka di situs, login. */
@Composable
fun TombolPlum(teks: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Kotak(
        modifier = modifier.defaultMinSize(
            minWidth = Jarak.sentuh + Jarak.bayangan,
            minHeight = Jarak.sentuh + Jarak.bayangan,
        ),
        bentuk = Bentuk.tombol,
        latar = SolidColor(w.plum),
        kilau = Kilau.Atas(2.dp, Color.White.copy(alpha = 0.18f)),
        padding = PaddingValues(horizontal = Jarak.s12, vertical = Jarak.s8),
        rataIsi = Alignment.Center,
        onClick = onClick,
    ) {
        Text(teks, style = Huruf.tombol, color = w.diAtasPlum)
    }
}

/** Tombol bergaris tanpa isi warna, untuk Ubah, Lewati, menu. ikon opsional di kiri teks. */
@Composable
fun TombolGaris(teks: String, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes ikon: Int? = null) {
    val w = GwadoTema.warna
    Kotak(
        modifier = modifier.defaultMinSize(minHeight = Jarak.sentuh + Jarak.bayanganKecil),
        bentuk = Bentuk.tombol,
        kilau = null,
        bayangan = Jarak.bayanganKecil,
        padding = PaddingValues(horizontal = Jarak.s12, vertical = Jarak.s8),
        rataIsi = Alignment.Center,
        onClick = onClick,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (ikon != null) Icon(painterResource(ikon), contentDescription = null, tint = w.tinta, modifier = Modifier.size(16.dp))
            Text(teks, style = Huruf.meta.copy(fontWeight = FontWeight.W800), color = w.tinta)
        }
    }
}
