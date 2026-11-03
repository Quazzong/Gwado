package io.github.quazzong.gwado.ui.komponen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak

/** Pratinjau komponen dasar di Android Studio. */
@Preview(name = "Terang", showBackground = true, widthDp = 360)
@Preview(name = "Gelap", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContohKomponen() = GwadoTema {
    val w = GwadoTema.warna
    Column(
        Modifier.background(w.latar).padding(Jarak.tepiLayar),
        verticalArrangement = Arrangement.spacedBy(Jarak.s12),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Jarak.s8)) {
            Chip("lewat 2 hari", jenis = JenisChip.LEWAT)
            Chip("3 jam", jenis = JenisChip.MEPET)
            Chip("5 hari")
            Chip("tanpa deadline", jenis = JenisChip.PUTUS)
        }
        Kotak(Modifier.fillMaxWidth(), latar = SolidColor(w.latarMepet), onClick = {}) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Laporan praktikum modul 4", style = Huruf.judulKartu, color = w.tinta)
                    Text("Brone · ABC60001", style = Huruf.meta, color = w.tintaRedup)
                }
                Chip("3 jam", jenis = JenisChip.MEPET)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Jarak.s8), verticalAlignment = Alignment.CenterVertically) {
            TombolPermen("Ambil sekarang", {}, Modifier.weight(1f))
            TombolPlum("+", {})
        }
        TombolGaris("Ubah", {})
        Kotak(
            Modifier.size(68.dp),
            bentuk = Motif.kue,
            latar = Brush.verticalGradient(0f to w.aksenTerang, 0.6f to w.aksen, 1f to w.aksenGelap),
            padding = PaddingValues(0.dp),
            rataIsi = Alignment.Center,
        ) {
            Text("4", style = Huruf.angkaUbin, color = Color(0xFF3A1D36))
        }
    }
}
