package io.github.quazzong.gwado.ui.layar

import io.github.quazzong.gwado.ui.komponen.LayarPenuh
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.layanan.prefsGwado
import io.github.quazzong.gwado.tampilan.Tekstur
import io.github.quazzong.gwado.tampilan.Tema
import io.github.quazzong.gwado.tampilan.simpanTema
import io.github.quazzong.gwado.tampilan.simpanTekstur
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.Segmen
import io.github.quazzong.gwado.ui.komponen.LocalTekstur
import io.github.quazzong.gwado.ui.komponen.StripSub
import io.github.quazzong.gwado.ui.komponen.TombolPlumIkon
import io.github.quazzong.gwado.ui.komponen.gradasiAksen
import io.github.quazzong.gwado.ui.komponen.tekstur
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.ui.tema.LocalPilihanTema
import kotlinx.coroutines.launch

/**
 * Pengaturan tampilan (DESAIN §9.10): tekstur latar sebagai tiga ubin pratinjau dan pilihan tema
 * (ikut sistem, terang, gelap). Pilihan langsung tersimpan; [onBerubah] memuat ulang supaya semua
 * layar ikut berganti.
 */
@Composable
fun LayarPengaturan(onKembali: () -> Unit, onBerubah: () -> Unit) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val cakupan = rememberCoroutineScope()
    val dipilih = LocalTekstur.current
    val temaDipilih = LocalPilihanTema.current
    val pilihTema: (Tema) -> Unit = { t ->
        prefsGwado(konteks).simpanTema(t)
        onBerubah()
    }
    val pilih: (Tekstur) -> Unit = { t ->
        cakupan.launch {
            runCatching { Gudang.pakai(konteks) { it.simpanTekstur(t) } }
            onBerubah()
        }
    }

    LayarPenuh("layar-pengaturan") {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(Jarak.tepiLayar),
            verticalArrangement = Arrangement.spacedBy(Jarak.s12),
        ) {
            StripSub("Tampilan", kiri = { TombolPlumIkon(R.drawable.ikon_kembali, "Kembali", onKembali) })
            Text("TEKSTUR LATAR", style = Huruf.label, color = w.tintaRedup)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (t in Tekstur.entries) UbinTekstur(t, t == dipilih, { pilih(t) }, Modifier.weight(1f))
                }
            }
            Text(
                "Tekstur hanya tampil di latar layar, tidak di belakang teks kartu.",
                style = Huruf.meta,
                color = w.tintaRedup,
            )
            Text("TEMA", style = Huruf.label, color = w.tintaRedup, modifier = Modifier.padding(top = Jarak.s8))
            Segmen(Tema.entries.map { it to it.label }, temaDipilih, pilihTema)
            Text(
                when (temaDipilih) {
                    Tema.SISTEM -> "Gwado dan widgetnya terang atau gelap mengikuti pengaturan tampilan HP."
                    Tema.TERANG -> "Gwado dan widgetnya selalu terang, apa pun pengaturan HP."
                    Tema.GELAP -> "Gwado dan widgetnya selalu gelap, apa pun pengaturan HP."
                },
                style = Huruf.meta,
                color = w.tintaRedup,
            )
        }
    }
}

@Composable
private fun UbinTekstur(t: Tekstur, aktif: Boolean, onPilih: () -> Unit, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Kotak(
        modifier.semantics { selected = aktif },
        latar = if (aktif) w.gradasiAksen() else SolidColor(w.kartu),
        bayangan = if (aktif) Jarak.bayangan else 0.dp,
        padding = PaddingValues(6.dp),
        onClick = onPilih,
        peran = Role.RadioButton,
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, w.garis, RoundedCornerShape(8.dp))
                    .background(w.latar)
                    .tekstur(t, w),
            )
            Text(
                t.label,
                style = Huruf.meta.copy(fontSize = 12.sp, fontWeight = FontWeight.W800),
                color = if (aktif) w.diAtasAksen else w.tinta,
                modifier = Modifier.padding(top = 5.dp, start = 2.dp),
            )
        }
    }
}
