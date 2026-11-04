package io.github.quazzong.gwado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
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
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.ui.komponen.ItemTab
import io.github.quazzong.gwado.ui.komponen.TabBar
import io.github.quazzong.gwado.ui.komponen.rememberSekarang
import io.github.quazzong.gwado.ui.layar.Beranda
import io.github.quazzong.gwado.ui.layar.LayarKalender
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

private enum class Tab { TUGAS, KALENDER, LAYANAN }

/** Akar tampilan: back stack layar, dimulai dari layar utama. */
@Composable
fun Akar(daftar: List<BarisTugas>, versi: Int, muatUlang: () -> Unit, layanan: @Composable () -> Unit) {
    val tumpukan = rememberNavBackStack(Utama)
    val buka = { k: NavKey -> if (tumpukan.lastOrNull() != k) tumpukan.add(k) }
    NavDisplay(
        backStack = tumpukan,
        onBack = { tumpukan.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Utama> {
                LayarUtama(daftar, versi, { id -> buka(Detail(id)) }, { buka(Formulir()) }, layanan)
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
        },
    )
}

@Composable
private fun LayarUtama(
    daftar: List<BarisTugas>,
    versi: Int,
    bukaDetail: (String) -> Unit,
    tambah: () -> Unit,
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
        ItemTab(Tab.LAYANAN, "Layanan", R.drawable.ikon_layanan),
    )
    Column(Modifier.fillMaxSize().background(w.latar).safeDrawingPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            penyimpan.SaveableStateProvider(tab) {
                when (tab) {
                    Tab.TUGAS -> Beranda(daftar, versi, sekarang, bukaDetail, tambah)
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
