package io.github.quazzong.gwado

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.github.quazzong.gwado.beranda.kuliahMulai
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.layanan.Pengambil
import io.github.quazzong.gwado.penjadwal.pasangPenjadwal
import io.github.quazzong.gwado.segar.statusSumber
import io.github.quazzong.gwado.segar.sumberTerakhir
import io.github.quazzong.gwado.semester.ringkasanMatkul
import io.github.quazzong.gwado.semester.semesterTersimpan
import io.github.quazzong.gwado.semester.simpanSemester
import io.github.quazzong.gwado.siapkan.perluSiapkan
import io.github.quazzong.gwado.siapkan.siapSelesai
import io.github.quazzong.gwado.siapkan.tandaiSiap
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.ui.Akar
import io.github.quazzong.gwado.ui.AturSemester
import io.github.quazzong.gwado.ui.DaftarMatkul
import io.github.quazzong.gwado.ui.Masuk
import io.github.quazzong.gwado.ui.Pengaturan
import io.github.quazzong.gwado.ui.komponen.LocalTekstur
import io.github.quazzong.gwado.tampilan.Tekstur
import io.github.quazzong.gwado.tampilan.tekstur
import io.github.quazzong.gwado.ui.layar.LayarLayanan
import io.github.quazzong.gwado.ui.layar.LayarMasuk
import io.github.quazzong.gwado.ui.layar.LayarSiapkan
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.widget.EKSTRA_TUGAS
import io.github.quazzong.gwado.widget.perbaruiWidget
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Isi database yang dibaca sekaligus untuk layar. */
private class DataLayar(
    val semester: Map<String, String>?,
    val kuliah: LocalDate?,
    val daftar: List<BarisTugas>,
    val matkul: Map<String, String>,
    val ditolak: Boolean,
    val tekstur: Tekstur,
)

class MainActivity : ComponentActivity() {
    private var daftar by mutableStateOf<List<BarisTugas>>(emptyList())
    private var umum by mutableStateOf("")
    private var versiData by mutableIntStateOf(0)
    private var semester by mutableStateOf<Map<String, String>?>(null)   // null = belum dikonfirmasi
    private var kuliah by mutableStateOf<LocalDate?>(null)
    private val matkul = mutableStateMapOf<String, String>()               // sumber → "N diikuti · M dilewati"
    private var tekstur by mutableStateOf(Tekstur.BAWAAN)
    private var lencanaLayanan by mutableStateOf(false)                    // ada layanan yang menolak login/izin
    private var tampilSiapkan by mutableStateOf(false)                     // layar Siapkan Gwado menggantikan tab
    private var bukaTugas by mutableStateOf<String?>(null)                 // detail yang diminta dari widget
    private val pengambil = Pengambil(this, onBerubah = { muatUlangDaftar() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GwadoTema {
                CompositionLocalProvider(LocalTekstur provides tekstur) {
                Akar(
                    daftar = daftar,
                    versi = versiData,
                    muatUlang = ::muatUlangDaftar,
                    menyegarkan = pengambil.sibuk,
                    onSegarkan = { pengambil.ambilSemua() },
                    hasilAmbil = pengambil.hasilTerakhir,
                    ditahan = pengambil.ditahan,
                    lencanaLayanan = lencanaLayanan,
                    bukaTugas = bukaTugas,
                    onTugasDibuka = { bukaTugas = null },
                    layanan = { buka ->
                        LayarLayanan(
                            pengambil, versiData, semester, kuliah, matkul,
                            onUbahSemester = { buka(AturSemester) },
                            onKonfirmasi = ::konfirmasiSemester,
                            onMatkul = { buka(DaftarMatkul(it)) },
                            onMasuk = { buka(Masuk(it.id)) },
                            pesanUmum = umum,
                            onPengaturan = { buka(Pengaturan) },
                        )
                    },
                    masuk = { id, kembali -> LayarMasuk(pengambil, id, kembali) },
                    siapkan = if (!tampilSiapkan) null else { buka ->
                        LayarSiapkan(
                            pengambil, versiData, semester,
                            onKonfirmasi = ::konfirmasiSemester,
                            onBuka = buka,
                            onSelesai = ::selesaiSiapkan,
                        )
                    },
                )
                }
            }
        }
        pasangPenjadwal(this)
        lifecycleScope.launch { tampilSiapkan = cekSiapkan() }
        // Hanya saat pertama dibuat: rotasi layar mengirim intent yang sama lagi (passport sudah terpakai).
        if (savedInstanceState == null) intentMasuk(intent)
    }

    /** Setiap kali layar tampil lagi: Ambil otomatis di latar mungkin sudah mengubah database. */
    override fun onStart() {
        super.onStart()
        muatUlangDaftar()
    }

    /** Baca ulang semester, kuliah pertama, daftar tugas, dan ringkasan mata kuliah. */
    private fun muatUlangDaftar() {
        lifecycleScope.launch {
            umum = try {
                val d = Gudang.pakai(this@MainActivity) { db ->
                    DataLayar(
                        db.semesterTersimpan(), db.kuliahMulai(), db.daftarTugas(), db.ringkasanMatkul(),
                        statusSumber(db.sumberTerakhir().filter { it.status != null }, Instant.now())
                            .any { it.label == "token ditolak" },
                        db.tekstur(),
                    )
                }
                semester = d.semester
                kuliah = d.kuliah
                daftar = d.daftar
                matkul.putAll(d.matkul)
                lencanaLayanan = d.ditolak
                tekstur = d.tekstur
                versiData++
                perbaruiWidget(this@MainActivity)
                ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Database tidak bisa dibuka: ${e.javaClass.simpleName}"
            }
        }
    }

    /** Apakah Siapkan Gwado perlu tampil. Instalasi lama yang sudah lengkap langsung ditandai siap. */
    private suspend fun cekSiapkan(): Boolean = try {
        val (selesai, adaSemester) = Gudang.pakai(this) { it.siapSelesai() to (it.semesterTersimpan() != null) }
        val perlu = perluSiapkan(selesai, adaSemester, pengambil.tersambung().isNotEmpty())
        if (!selesai && !perlu) Gudang.pakai(this) { it.tandaiSiap() }
        perlu
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    /** Pengambilan pertama selesai: tandai siap, tampilkan tab. */
    private fun selesaiSiapkan() {
        lifecycleScope.launch {
            try {
                Gudang.pakai(this@MainActivity) { it.tandaiSiap() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // tanda tidak tersimpan: Siapkan muncul lagi di pembukaan berikutnya
            }
            tampilSiapkan = false
            muatUlangDaftar()
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
        intent?.getStringExtra(EKSTRA_TUGAS)?.let { bukaTugas = it }
        val data = intent?.data ?: return
        if (data.scheme == "moodlemobile") pengambil.tangani(data.toString())
    }
}
