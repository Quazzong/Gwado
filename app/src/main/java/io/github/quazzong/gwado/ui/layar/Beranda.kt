package io.github.quazzong.gwado.ui.layar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.ui.komponen.KartuTugas
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.waktu.Makna
import io.github.quazzong.gwado.waktu.dariDb
import io.github.quazzong.gwado.waktu.sisaChip
import java.time.Instant

/** Tugas belum dicentang yang situsnya mencatat sudah dikumpulkan. */
internal fun terkumpul(t: BarisTugas): Boolean =
    t.status != "done" && t.statusKumpulSitus == "terkumpul"

/**
 * Tugas belum dicentang dan belum terkumpul di situs yang sudah lewat,
 * hari ini, besok, atau kurang dari 1 jam lagi.
 */
internal fun mendesak(t: BarisTugas, sekarang: Instant): Boolean =
    t.status != "done" && !terkumpul(t) &&
        (t.deadline?.let { sisaChip(dariDb(it), sekarang).makna != Makna.NORMAL } ?: false)

/**
 * Daftar tugas: MENDESAK, lalu NANTI (tanpa deadline di akhirnya), lalu
 * TERKUMPUL DI SITUS. Tiap bagian tetap urut deadline. Ketuk kartu = buka detail.
 */
@Composable
fun Beranda(daftar: List<BarisTugas>, sekarang: Instant, onBuka: (String) -> Unit, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    if (daftar.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(Jarak.tepiLayar),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Belum ada tugas", style = Huruf.judulBagian, color = w.tinta)
            Text(
                "Tekan Ambil di tab Layanan untuk mengambil tugas dari situs kuliah.",
                style = Huruf.isi,
                color = w.tintaRedup,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val (sudah, belum) = daftar.partition(::terkumpul)
    val (atas, bawah) = belum.partition { mendesak(it, sekarang) }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(Jarak.tepiLayar),
        verticalArrangement = Arrangement.spacedBy(Jarak.s8),
    ) {
        bagian("Mendesak", atas, sekarang, onBuka)
        bagian("Nanti", bawah, sekarang, onBuka)
        bagian("Terkumpul di situs", sudah, sekarang, onBuka)
    }
}

private fun LazyListScope.bagian(judul: String, isi: List<BarisTugas>, sekarang: Instant, onBuka: (String) -> Unit) {
    if (isi.isEmpty()) return
    item(key = "bagian-$judul") {
        Text(
            judul.uppercase(),
            style = Huruf.label,
            color = GwadoTema.warna.tintaRedup,
            modifier = Modifier.padding(top = Jarak.s8),
        )
    }
    items(isi, key = { it.id }) { t -> KartuTugas(t, sekarang, onClick = { onBuka(t.id) }) }
}
