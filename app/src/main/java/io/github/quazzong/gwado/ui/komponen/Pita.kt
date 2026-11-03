package io.github.quazzong.gwado.ui.komponen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak

/** Kotak kuning satu kalimat di atas konten, untuk keadaan yang perlu diketahui. */
@Composable
fun Pita(teks: String, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Kotak(
        modifier.fillMaxWidth(),
        bentuk = Bentuk.kartu,
        latar = SolidColor(w.latarMepet),
        kilau = null,
        bayangan = Jarak.bayanganKecil,
        padding = PaddingValues(Jarak.s12),
    ) {
        Text(teks, style = Huruf.isi, color = w.tinta)
    }
}
