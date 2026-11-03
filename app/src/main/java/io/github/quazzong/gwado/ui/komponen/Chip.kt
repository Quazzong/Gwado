package io.github.quazzong.gwado.ui.komponen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.Fredoka
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.WarnaStatus

enum class JenisChip { LEWAT, MEPET, NETRAL, REDUP, PUTUS, AKTIF, PUTIH }

/** Label kecil kapital bergaris. Jenis berwarna memakai gradasi dan kilau atas. */
@Composable
fun Chip(teks: String, modifier: Modifier = Modifier, jenis: JenisChip = JenisChip.NETRAL) {
    val w = GwadoTema.warna
    val bening = SolidColor(Color.Transparent)
    val (latar, warnaTeks) = when (jenis) {
        JenisChip.LEWAT -> Brush.verticalGradient(WarnaStatus.lewat) to WarnaStatus.diAtasLewat
        JenisChip.MEPET -> Brush.verticalGradient(WarnaStatus.mepet) to WarnaStatus.diAtasMepet
        JenisChip.AKTIF -> Brush.verticalGradient(listOf(w.aksenTerang, w.aksen)) to w.diAtasAksen
        JenisChip.NETRAL -> bening to w.tinta
        JenisChip.PUTIH -> SolidColor(w.kartu) to w.tinta
        JenisChip.REDUP, JenisChip.PUTUS -> bening to w.tintaRedup
    }
    val berwarna = jenis == JenisChip.LEWAT || jenis == JenisChip.MEPET || jenis == JenisChip.AKTIF
    Kotak(
        modifier = modifier,
        bentuk = Bentuk.chip,
        latar = latar,
        putus = jenis == JenisChip.PUTUS,
        kilau = if (berwarna) Kilau.Atas(1.dp, Color.White.copy(alpha = 0.55f)) else null,
        bayangan = 0.dp,
        padding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            teks.uppercase(),
            style = if (jenis == JenisChip.PUTIH) Huruf.chip.copy(fontFamily = Fredoka) else Huruf.chip,
            color = warnaTeks,
            maxLines = 1,
        )
    }
}
