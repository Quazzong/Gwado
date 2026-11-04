package io.github.quazzong.gwado

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.github.quazzong.gwado.beranda.kuliahMulai
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.layanan.Pengambil
import io.github.quazzong.gwado.semester.ringkasanMatkul
import io.github.quazzong.gwado.semester.semesterTersimpan
import io.github.quazzong.gwado.semester.simpanSemester
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.ui.Akar
import io.github.quazzong.gwado.ui.AturSemester
import io.github.quazzong.gwado.ui.DaftarMatkul
import io.github.quazzong.gwado.ui.Masuk
import io.github.quazzong.gwado.ui.layar.LayarLayanan
import io.github.quazzong.gwado.ui.layar.LayarMasuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Isi database yang dibaca sekaligus untuk layar. */
private class DataLayar(
    val semester: Map<String, String>?,
    val kuliah: LocalDate?,
    val daftar: List<BarisTugas>,
    val matkul: Map<String, String>,
)

class MainActivity : ComponentActivity() {
    private var daftar by mutableStateOf<List<BarisTugas>>(emptyList())
    private var umum by mutableStateOf("")
    private var versiData by mutableIntStateOf(0)
    private var semester by mutableStateOf<Map<String, String>?>(null)   // null = belum dikonfirmasi
    private var kuliah by mutableStateOf<LocalDate?>(null)
    private val matkul = mutableStateMapOf<String, String>()               // sumber → "N diikuti · M dilewati"
    private val pengambil = Pengambil(this, onBerubah = { muatUlangDaftar() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GwadoTema {
                Akar(
                    daftar = daftar,
                    versi = versiData,
                    muatUlang = ::muatUlangDaftar,
                    menyegarkan = pengambil.sibuk,
                    onSegarkan = { pengambil.ambilSemua() },
                    hasilAmbil = pengambil.hasilTerakhir,
                    layanan = { buka ->
                        LayarLayanan(
                            pengambil, versiData, semester, kuliah, matkul,
                            onUbahSemester = { buka(AturSemester) },
                            onKonfirmasi = ::konfirmasiSemester,
                            onMatkul = { buka(DaftarMatkul(it)) },
                            onMasuk = { buka(Masuk(it.id)) },
                            pesanUmum = umum,
                        )
                    },
                    masuk = { id, kembali -> LayarMasuk(pengambil, id, kembali) },
                )
            }
        }
        muatUlangDaftar()
        // Hanya saat pertama dibuat: rotasi layar mengirim intent yang sama lagi (passport sudah terpakai).
        if (savedInstanceState == null) intentMasuk(intent)
    }

    /** Baca ulang semester, kuliah pertama, daftar tugas, dan ringkasan mata kuliah. */
    private fun muatUlangDaftar() {
        lifecycleScope.launch {
            umum = try {
                val d = Gudang.pakai(this@MainActivity) { db ->
                    DataLayar(db.semesterTersimpan(), db.kuliahMulai(), db.daftarTugas(), db.ringkasanMatkul())
                }
                semester = d.semester
                kuliah = d.kuliah
                daftar = d.daftar
                matkul.putAll(d.matkul)
                versiData++
                ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Database tidak bisa dibuka: ${e.javaClass.simpleName}"
            }
        }
    }

    /** Simpan tebakan semester apa adanya (tombol Sudah benar). */
    private fun konfirmasiSemester(nilai: Map<String, String>) {
        lifecycleScope.launch {
            umum = try {
                Gudang.pakai(this@MainActivity) { it.simpanSemester(nilai) }
                semester = nilai
                ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Semester tidak bisa disimpan: ${e.javaClass.simpleName}"
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentMasuk(intent)
    }

    private fun intentMasuk(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "moodlemobile") pengambil.tangani(data.toString())
    }
}
