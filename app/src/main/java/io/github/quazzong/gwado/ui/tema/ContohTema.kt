package io.github.quazzong.gwado.ui.tema

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Pratinjau token warna dan gaya teks di Android Studio. */
@Preview(name = "Terang", showBackground = true)
@Preview(name = "Gelap", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContohTema() = GwadoTema {
    val w = GwadoTema.warna
    Column(
        Modifier.background(w.latar).padding(Jarak.tepiLayar),
        verticalArrangement = Arrangement.spacedBy(Jarak.s8),
    ) {
        Text("Senin", style = Huruf.judulLayar, color = w.tinta)
        Text("November", style = Huruf.judulStrip, color = w.tinta)
        Text("Laporan praktikum modul 4", style = Huruf.judulKartu, color = w.tinta)
        Text("Brone · ABC60001 · 23.59", style = Huruf.meta, color = w.tintaRedup)
        Text("MENDESAK", style = Huruf.label, color = w.tintaRedup)
        Text("Ambil sekarang", style = Huruf.tombol, color = w.tinta)
        Row(horizontalArrangement = Arrangement.spacedBy(Jarak.s4)) {
            listOf(w.aksen, w.plum, w.ubin, w.lavender, w.latarLewat, w.latarMepet).forEach {
                Box(Modifier.size(28.dp).background(it, Bentuk.chip).border(Jarak.garis, w.garis, Bentuk.chip))
            }
        }
    }
}
