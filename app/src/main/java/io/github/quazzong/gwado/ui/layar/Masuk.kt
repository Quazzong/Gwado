package io.github.quazzong.gwado.ui.layar

import io.github.quazzong.gwado.ui.komponen.LayarPenuh
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.layanan.DAFTAR_LAYANAN
import io.github.quazzong.gwado.layanan.Pengambil
import io.github.quazzong.gwado.ui.komponen.IsianTeks
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.RadioKue
import io.github.quazzong.gwado.ui.komponen.StripSub
import io.github.quazzong.gwado.ui.komponen.TombolPermen
import io.github.quazzong.gwado.ui.komponen.TombolPlum
import io.github.quazzong.gwado.ui.komponen.TombolPlumIkon
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak

/**
 * Login Moodle untuk satu layanan: penjelasan dialog "Buka dengan", tombol ke halaman login
 * di browser, dan cadangan tempel alamat moodlemobile://. Layar menutup diri setelah login berhasil.
 */
@Composable
fun LayarMasuk(pengambil: Pengambil, id: String, onKembali: () -> Unit) {
    val w = GwadoTema.warna
    val l = DAFTAR_LAYANAN.firstOrNull { it.id == id && it.situs != null }
    var tempel by rememberSaveable { mutableStateOf(false) }
    var alamat by rememberSaveable { mutableStateOf("") }
    val awal = remember { pengambil.loginBerhasil }
    val kini = pengambil.loginBerhasil
    LaunchedEffect(kini) {
        if (kini != awal && kini?.first == id) onKembali()
    }

    LayarPenuh("layar-masuk-$id") {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Jarak.tepiLayar),
            verticalArrangement = Arrangement.spacedBy(Jarak.s16),
        ) {
            StripSub(
                "Masuk ${l?.label.orEmpty()}",
                kiri = { TombolPlumIkon(R.drawable.ikon_kembali, "Kembali", onKembali) },
            )
            if (l == null) {
                Text("Layanan ini tidak memakai login lewat browser.", style = Huruf.isi, color = w.tintaRedup)
                return@Column
            }
            Kotak(Modifier.fillMaxWidth()) {
                Column {
                    Text("Browser akan membuka halaman login ${l.label}", style = Huruf.judulKartu, color = w.tinta)
                    Text(
                        "Kalau HP bertanya, pilih Gwado.",
                        style = Huruf.meta,
                        color = w.tintaRedup,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    ContohBukaDengan(Modifier.padding(top = 10.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Jarak.s8)) {
                TombolPermen("Buka halaman login", { pengambil.tambah(l) }, Modifier.fillMaxWidth())
                pengambil.status[l.id]?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = Huruf.meta.copy(fontWeight = FontWeight.W800), color = w.tinta)
                }
            }
            Box(
                Modifier
                    .defaultMinSize(minHeight = Jarak.sentuh)
                    .clickable(role = Role.Button) { tempel = !tempel },
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    "Tidak kembali ke Gwado setelah login?",
                    style = Huruf.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.W800, textDecoration = TextDecoration.Underline),
                    color = w.tinta,
                )
            }
            if (tempel) {
                Kotak(Modifier.fillMaxWidth(), putus = true, kilau = null, bayangan = 0.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(Jarak.s8)) {
                        Text(
                            "Salin alamat moodlemobile:// yang terbuka di aplikasi Moodle, lalu tempel di sini.",
                            style = Huruf.meta,
                            color = w.tintaRedup,
                        )
                        IsianTeks(alamat, { alamat = it }, petunjuk = "moodlemobile://token=…", satuBaris = true)
                        TombolPlum("Tempel dan sambungkan", {
                            if (alamat.isNotBlank()) {
                                pengambil.tangani(alamat, untuk = l.id)
                                alamat = ""
                            }
                        })
                    }
                }
            }
            Text("Password tidak pernah disimpan.", style = Huruf.meta, color = w.tintaRedup)
        }
    }
}

/** Gambaran dialog "Buka dengan": Gwado ditandai, Moodle redup. */
@Composable
private fun ContohBukaDengan(modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Kotak(
        modifier.fillMaxWidth(),
        latar = SolidColor(w.ubin),
        kilau = null,
        bayangan = 0.dp,
        padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Buka dengan", style = Huruf.meta.copy(fontWeight = FontWeight.W800), color = w.tinta)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Kotak(
                    Modifier.weight(1f),
                    kilau = null,
                    bayangan = Jarak.bayanganKecil,
                    padding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioKue(true, ukuran = 20.dp)
                        Text("Gwado", style = Huruf.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.W800), color = w.tinta)
                    }
                }
                Kotak(
                    Modifier.weight(1f),
                    latar = SolidColor(w.ubin),
                    garis = w.garisHalus,
                    putus = true,
                    kilau = null,
                    bayangan = 0.dp,
                    padding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioKue(false, ukuran = 20.dp)
                        Text("Moodle", style = Huruf.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.W800), color = w.tintaRedup)
                    }
                }
            }
        }
    }
}
