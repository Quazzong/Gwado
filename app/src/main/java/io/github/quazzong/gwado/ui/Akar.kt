package io.github.quazzong.gwado.ui

import io.github.quazzong.gwado.ui.komponen.latarLayar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.layanan.HasilAmbil
import io.github.quazzong.gwado.layanan.Tahan
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.ui.komponen.ItemTab
import io.github.quazzong.gwado.ui.komponen.TabBar
import io.github.quazzong.gwado.ui.komponen.rememberSekarang
import io.github.quazzong.gwado.ui.layar.Beranda
import io.github.quazzong.gwado.ui.layar.LayarKalender
import io.github.quazzong.gwado.ui.layar.LayarMatkul
import io.github.quazzong.gwado.ui.layar.LayarPengaturan
import io.github.quazzong.gwado.ui.layar.LayarSemester
import io.github.quazzong.gwado.ui.layar.LayarSemuaSelesai
import io.github.quazzong.gwado.ui.layar.LayarFormulir
import io.github.quazzong.gwado.ui.layar.LayarDetail
import io.github.quazzong.gwado.ui.layar.mendesak
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Jarak
import kotlinx.serialization.Serializable

/** Layar dengan tab bar. Layar penuh lain (detail, formulir) ditumpuk di atasnya. */
@Serializable
data object Utama : NavKey

/** Detail satu tugas, menutupi tab bar. */
@Serializable
data class Detail(val id: String) : NavKey

/** Formulir tugas manual: id null = tugas baru. */
@Serializable
data class Formulir(val id: String? = null) : NavKey

/** Login Moodle satu layanan lewat browser, menutupi tab bar. */
@Serializable
data class Masuk(val id: String) : NavKey

/** Daftar mata kuliah satu layanan beserta pengecualian manualnya. */
@Serializable
data class DaftarMatkul(val sumber: String) : NavKey

/** Semua tugas yang dicentang atau beres di situs. */
@Serializable
data object SemuaSelesai : NavKey

/** Pengaturan tampilan: tekstur latar dan tema. */
@Serializable
data object Pengaturan : NavKey

/** Ubah semester aktif dan tanggal kuliah pertama. */
@Serializable
data object AturSemester : NavKey

private enum class Tab { TUGAS, KALENDER, LAYANAN }

/** Akar tampilan: back stack layar, dimulai dari layar utama (atau Siapkan Gwado kalau [siapkan] diisi). */
@Composable
fun Akar(
    daftar: List<BarisTugas>,
    versi: Int,
    muatUlang: () -> Unit,
    menyegarkan: Boolean,
    onSegarkan: () -> Unit,
    hasilAmbil: HasilAmbil?,
    ditahan: Map<String, Tahan>,
    lencanaLayanan: Boolean,
    bukaTugas: String? = null,
    onTugasDibuka: () -> Unit = {},
    layanan: @Composable (buka: (NavKey) -> Unit) -> Unit,
    masuk: @Composable (id: String, onKembali: () -> Unit) -> Unit,
    siapkan: (@Composable (buka: (NavKey) -> Unit) -> Unit)? = null,
) {
    val tumpukan = rememberNavBackStack(Utama)
    val buka = { k: NavKey -> if (tumpukan.lastOrNull() != k) tumpukan.add(k) }
    // Ketuk tugas di widget: buka detailnya (diabaikan selama Siapkan Gwado tampil).
    LaunchedEffect(bukaTugas) {
        if (bukaTugas != null) {
            if (siapkan == null) buka(Detail(bukaTugas))
            onTugasDibuka()
        }
    }
    NavDisplay(
        backStack = tumpukan,
        onBack = { tumpukan.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Utama> {
                if (siapkan != null) {
                    siapkan(buka)
                } else {
                    LayarUtama(daftar, versi, { id -> buka(Detail(id)) }, { buka(Formulir()) }, menyegarkan, onSegarkan, hasilAmbil, muatUlang, { buka(SemuaSelesai) }, ditahan, lencanaLayanan) {
                        layanan(buka)
                    }
                }
            }
            entry<Detail> { k ->
                LayarDetail(
                    k.id,
                    onKembali = { tumpukan.removeLastOrNull() },
                    onBerubah = muatUlang,
                    onUbah = { id -> buka(Formulir(id)) },
                )
            }
            entry<Formulir> { k ->
                LayarFormulir(
                    k.id,
                    onKembali = { tumpukan.removeLastOrNull() },
                    onTersimpan = {
                        muatUlang()
                        tumpukan.removeLastOrNull()
                    },
                )
            }
            entry<Masuk> { k -> masuk(k.id) { tumpukan.removeLastOrNull() } }
            entry<DaftarMatkul> { k -> LayarMatkul(k.sumber, onKembali = { tumpukan.removeLastOrNull() }) }
            entry<Pengaturan> {
                LayarPengaturan(onKembali = { tumpukan.removeLastOrNull() }, onBerubah = muatUlang)
            }
            entry<SemuaSelesai> {
                LayarSemuaSelesai(versi, onBuka = { id -> buka(Detail(id)) }, onKembali = { tumpukan.removeLastOrNull() })
            }
            entry<AturSemester> {
                LayarSemester(
                    onKembali = { tumpukan.removeLastOrNull() },
                    onTersimpan = {
                        muatUlang()
                        tumpukan.removeLastOrNull()
                    },
                )
            }
        },
    )
}

@Composable
private fun LayarUtama(
    daftar: List<BarisTugas>,
    versi: Int,
    bukaDetail: (String) -> Unit,
    tambah: () -> Unit,
    menyegarkan: Boolean,
    onSegarkan: () -> Unit,
    hasilAmbil: HasilAmbil?,
    muatUlang: () -> Unit,
    bukaSelesai: () -> Unit,
    ditahan: Map<String, Tahan>,
    lencanaLayanan: Boolean,
    layanan: @Composable () -> Unit,
) {
    val w = GwadoTema.warna
    val sekarang = rememberSekarang()
    var tab by rememberSaveable { mutableStateOf(Tab.TUGAS) }
    val penyimpan = rememberSaveableStateHolder()
    val jumlahMendesak = daftar.count { mendesak(it, sekarang) }
    val item = listOf(
        ItemTab(Tab.TUGAS, "Tugas", R.drawable.ikon_tugas, jumlahMendesak.takeIf { it > 0 }?.toString()),
        ItemTab(Tab.KALENDER, "Kalender", R.drawable.ikon_kalender),
        ItemTab(Tab.LAYANAN, "Layanan", R.drawable.ikon_layanan, if (lencanaLayanan) "!" else null),
    )
    Column(Modifier.fillMaxSize().latarLayar().safeDrawingPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            penyimpan.SaveableStateProvider(tab) {
                when (tab) {
                    Tab.TUGAS -> Beranda(
                        daftar, versi, sekarang, bukaDetail, tambah, menyegarkan, onSegarkan, hasilAmbil,
                        onBukaLayanan = { tab = Tab.LAYANAN },
                        onBerubah = muatUlang,
                        onSemuaSelesai = bukaSelesai,
                        ditahan = ditahan,
                    )
                    Tab.KALENDER -> LayarKalender(daftar, versi, sekarang, bukaDetail)
                    Tab.LAYANAN -> layanan()
                }
            }
        }
        TabBar(
            item,
            aktif = tab,
            onPilih = { tab = it },
            modifier = Modifier.padding(start = Jarak.tepiLayar, end = Jarak.tepiLayar, top = Jarak.s8, bottom = Jarak.s12),
        )
    }
}
