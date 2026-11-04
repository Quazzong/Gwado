package io.github.quazzong.gwado.ui.layar

import io.github.quazzong.gwado.ui.komponen.latarLayar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.beranda.saring
import io.github.quazzong.gwado.beranda.semuaSelesai
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.ui.komponen.Chip
import io.github.quazzong.gwado.ui.komponen.IsianTeks
import io.github.quazzong.gwado.ui.komponen.JenisChip
import io.github.quazzong.gwado.ui.komponen.KartuTugas
import io.github.quazzong.gwado.ui.komponen.Segmen
import io.github.quazzong.gwado.ui.komponen.StripSub
import io.github.quazzong.gwado.ui.komponen.TombolPlumIkon
import io.github.quazzong.gwado.ui.komponen.rememberSekarang
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import java.time.Instant

/**
 * Semua tugas yang dicentang atau terkumpul di situs, dalam dua bagian: DICENTANG (belum
 * terkumpul di situs, termasuk tugas manual) lalu TERKUMPUL DI SITUS, masing-masing deadline
 * terbaru di atas. Bisa disaring per layanan dan dicari lewat judul atau mata kuliah.
 * [versi] naik setiap kali data berubah (misalnya setelah Batalkan selesai).
 */
@Composable
fun LayarSemuaSelesai(versi: Int, onBuka: (String) -> Unit, onKembali: () -> Unit) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val sekarang = rememberSekarang()
    val daftar by produceState<List<BarisTugas>?>(null, versi) {
        value = runCatching { Gudang.pakai(konteks) { semuaSelesai(it.daftarTugas(semua = true)) } }.getOrDefault(emptyList())
    }
    var kata by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val semua = daftar.orEmpty()
    val layanan = semua.map { it.sumber ?: "Manual" }.distinct().sortedBy { it == "Manual" }
    val aktif = filter?.takeIf { it in layanan }
    val tampil = saring(semua, kata, aktif)
    val (terkumpul, dicentang) = tampil.partition { it.statusKumpulSitus == "terkumpul" }

    LazyColumn(
        Modifier.fillMaxSize().latarLayar().safeDrawingPadding(),
        contentPadding = PaddingValues(Jarak.tepiLayar),
        verticalArrangement = Arrangement.spacedBy(Jarak.s8),
    ) {
        item(key = "strip") {
            StripSub(
                "Selesai",
                Modifier.padding(bottom = Jarak.s4),
                kiri = { TombolPlumIkon(R.drawable.ikon_kembali, "Kembali", onKembali) },
                kanan = { if (daftar != null) Chip("${tampil.size} tugas", jenis = JenisChip.PUTIH) },
            )
        }
        item(key = "cari") {
            IsianTeks(kata, { kata = it }, petunjuk = "Cari judul atau mata kuliah", satuBaris = true)
        }
        if (layanan.size > 1) {
            item(key = "filter") {
                Segmen(
                    listOf<Pair<String?, String>>(null to "Semua") + layanan.map { it to singkat(it) },
                    aktif = aktif,
                    onPilih = { filter = it },
                )
            }
        }
        if (daftar != null && tampil.isEmpty()) {
            item(key = "kosong") {
                Text(
                    if (semua.isEmpty()) "Belum ada tugas yang selesai." else "Tidak ada yang cocok dengan pencarian ini.",
                    style = Huruf.isi,
                    color = w.tintaRedup,
                )
            }
        }
        kelompok("Dicentang", dicentang, sekarang, onBuka)
        kelompok("Terkumpul di situs", terkumpul, sekarang, onBuka)
    }
}

private fun LazyListScope.kelompok(judul: String, isi: List<BarisTugas>, sekarang: Instant, onBuka: (String) -> Unit) {
    if (isi.isEmpty()) return
    item(key = "kelompok-$judul") {
        Text(
            "${judul.uppercase()} · ${isi.size}",
            style = Huruf.label,
            color = GwadoTema.warna.tintaRedup,
            modifier = Modifier.padding(top = Jarak.s8),
        )
    }
    items(isi, key = { it.id }) { t -> KartuTugas(t, sekarang, onClick = { onBuka(t.id) }) }
}
