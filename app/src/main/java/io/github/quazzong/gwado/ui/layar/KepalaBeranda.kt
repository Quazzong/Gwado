package io.github.quazzong.gwado.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.beranda.Progres
import io.github.quazzong.gwado.beranda.mingguKe
import io.github.quazzong.gwado.ui.komponen.BatangProgres
import io.github.quazzong.gwado.ui.komponen.Chip
import io.github.quazzong.gwado.ui.komponen.JenisChip
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.LencanaKue
import io.github.quazzong.gwado.ui.komponen.Taburan
import io.github.quazzong.gwado.ui.komponen.TeksStiker
import io.github.quazzong.gwado.ui.komponen.gradasiCss
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.waktu.WIB
import java.time.Instant
import java.time.LocalDate

private val HARI_PENUH = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
private val BULAN_SINGKAT = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

/**
 * Kepala beranda: strip berisi minggu kuliah, nama hari, dan tanggal; di bawahnya
 * progres tugas minggu ini. Chip minggu hanya tampil kalau tanggal kuliah pertama diisi.
 */
@Composable
fun KepalaBeranda(sekarang: Instant, kuliahMulai: LocalDate?, progres: Progres, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    val wib = sekarang.atOffset(WIB)
    val minggu = kuliahMulai?.let { mingguKe(it, wib.toLocalDate()) }
    Kotak(
        modifier.fillMaxWidth(),
        bentuk = Bentuk.header,
        bayanganKedua = 6.dp,
        padding = PaddingValues(Jarak.garis * 2),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(gradasiCss(115f, w.strip)),
            ) {
                Taburan(benih = 11, tinggi = 110f, modifier = Modifier.matchParentSize())
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Jarak.s12, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Jarak.s8)) {
                        if (minggu != null) Chip("Minggu ke-$minggu", jenis = JenisChip.PUTIH)
                        TeksStiker(HARI_PENUH[wib.dayOfWeek.value - 1])
                    }
                    LencanaKue("${wib.dayOfMonth}", BULAN_SINGKAT[wib.monthValue - 1])
                }
            }
            Box(Modifier.fillMaxWidth().height(Jarak.garis).background(w.garis))
            Column(Modifier.padding(horizontal = 10.dp, vertical = Jarak.s8)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val gaya = Huruf.meta.copy(fontWeight = FontWeight.W800)
                    Text("Minggu ini", style = gaya, color = w.tintaRedup)
                    Text(
                        if (progres.total == 0) "Tidak ada deadline" else "${progres.selesai} dari ${progres.total} selesai",
                        style = gaya,
                        color = w.tintaRedup,
                    )
                }
                Spacer(Modifier.height(6.dp))
                BatangProgres(if (progres.total == 0) 0f else progres.selesai.toFloat() / progres.total)
            }
        }
    }
}
