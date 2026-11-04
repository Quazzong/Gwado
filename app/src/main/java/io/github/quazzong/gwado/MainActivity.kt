package io.github.quazzong.gwado

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.github.quazzong.gwado.beranda.kuliahMulai
import io.github.quazzong.gwado.beranda.simpanKuliahMulai
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.layanan.DAFTAR_LAYANAN
import io.github.quazzong.gwado.layanan.Pengambil
import io.github.quazzong.gwado.semester.LABEL_SEMESTER
import io.github.quazzong.gwado.semester.Matkul
import io.github.quazzong.gwado.semester.aturPengecualian
import io.github.quazzong.gwado.semester.daftarMatkul
import io.github.quazzong.gwado.semester.periksaSemester
import io.github.quazzong.gwado.semester.ringkasanMatkul
import io.github.quazzong.gwado.semester.semesterTersimpan
import io.github.quazzong.gwado.semester.simpanSemester
import io.github.quazzong.gwado.semester.tanggalWib
import io.github.quazzong.gwado.semester.tebakSemester
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.ui.Akar
import io.github.quazzong.gwado.ui.layar.LayarLayanan
import io.github.quazzong.gwado.ui.tema.GwadoTema
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val PILIHAN_PENGECUALIAN: List<Pair<String?, String>> = listOf(
    null to "Otomatis",
    "ikuti" to "Ikuti",
    "abaikan" to "Abaikan",
)

class MainActivity : ComponentActivity() {
    private var daftar by mutableStateOf<List<BarisTugas>>(emptyList())
    private var umum by mutableStateOf("")
    private var alamatTempel by mutableStateOf("")
    private var isianKuliah by mutableStateOf("")
    private var versiData by mutableIntStateOf(0)
    private var semester by mutableStateOf<Map<String, String>?>(null)   // null = belum dikonfirmasi
    private val isianSemester = mutableStateMapOf<String, String>()
    private var lihatMatkul by mutableStateOf<String?>(null)              // sumber yang daftar matkulnya dibuka
    private var matkulSumber by mutableStateOf<List<Matkul>>(emptyList())
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
                ) {
                    LayarLayanan(
                        pengambil, versiData, semester, matkul,
                        onUbahSemester = { semester?.let { ubahSemester(it) } },
                        onMatkul = { bukaMatkul(it) },
                    ) { AlatUji() }
                }
            }
        }
        lifecycleScope.launch {
            umum = try {
                val (sm, d, r) = Gudang.pakai(this@MainActivity) { db ->
                    Triple(db.semesterTersimpan(), db.daftarTugas(), db.ringkasanMatkul())
                }
                semester = sm
                daftar = d
                versiData++
                isianKuliah = Gudang.pakai(this@MainActivity) { it.kuliahMulai() }?.toString().orEmpty()
                matkul.putAll(r)
                if (sm == null) isianSemester.putAll(tebakSemester(tanggalWib(Instant.now())))
                ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Database tidak bisa dibuka: ${e.javaClass.simpleName}"
            }
        }
        // Hanya saat pertama dibuat: rotasi layar mengirim intent yang sama lagi (passport sudah terpakai).
        if (savedInstanceState == null) intentMasuk(intent)
    }

    /** Isian sementara yang belum punya tampilan sendiri. */
    @Composable
    private fun AlatUji() {
        val sibuk = pengambil.sibuk
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (semester == null) {
                Text("Konfirmasi semester aktif (ditebak dari tanggal hari ini, betulkan kalau salah):")
                for ((kunci, label) in LABEL_SEMESTER) {
                    OutlinedTextField(
                        value = isianSemester[kunci].orEmpty(),
                        onValueChange = { isianSemester[kunci] = it },
                        label = { Text(label) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Button(onClick = { simpanSemesterDariIsian() }) { Text("Simpan semester") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = isianKuliah,
                    onValueChange = { isianKuliah = it },
                    label = { Text("Kuliah pertama (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { simpanKuliah() }, enabled = !sibuk) { Text("Simpan") }
            }
            Text("Sambungkan layanan:", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (l in DAFTAR_LAYANAN) {
                    OutlinedButton(onClick = { pengambil.ambil(l) }, enabled = !sibuk && semester != null) { Text(l.label) }
                }
            }
            for (l in DAFTAR_LAYANAN) {
                pengambil.status[l.id]?.takeIf { it.isNotBlank() }?.let {
                    Text("${l.label}: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedTextField(
                value = alamatTempel,
                onValueChange = { alamatTempel = it },
                label = { Text("Cadangan: tempel alamat moodlemobile://…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { pengambil.tangani(alamatTempel); alamatTempel = "" },
                enabled = !sibuk && alamatTempel.isNotBlank(),
            ) { Text("Pakai alamat") }
            if (umum.isNotEmpty()) Text(umum)
            val ms = lihatMatkul
            if (ms != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Mata kuliah ${DAFTAR_LAYANAN.first { it.id == ms }.label} · " +
                            "pengecualian berlaku saat Ambil berikutnya",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { lihatMatkul = null }) { Text("Tutup") }
                }
                for (m in matkulSumber) {
                    Column {
                        Text((if (m.diikuti) "✓ " else "– ") + m.nama)
                        Text(m.alasan.orEmpty(), style = MaterialTheme.typography.bodySmall)
                        Row {
                            for ((aksi, label) in PILIHAN_PENGECUALIAN) {
                                TextButton(
                                    onClick = { atur(ms, m.externalId, aksi) },
                                    enabled = m.aksi != aksi,
                                ) { Text(label) }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Simpan tanggal kuliah pertama (kosong = hapus), lalu muat ulang beranda. */
    private fun simpanKuliah() {
        val teks = isianKuliah.trim()
        val tanggal = if (teks.isEmpty()) null else runCatching { LocalDate.parse(teks) }.getOrNull()
        if (teks.isNotEmpty() && tanggal == null) {
            umum = "Tanggal kuliah pertama harus berbentuk YYYY-MM-DD, misalnya 2026-08-24"
            return
        }
        lifecycleScope.launch {
            Gudang.pakai(this@MainActivity) { it.simpanKuliahMulai(tanggal) }
            umum = if (tanggal == null) "Tanggal kuliah pertama dihapus" else "Tanggal kuliah pertama disimpan"
            muatUlangDaftar()
        }
    }

    /** Baca ulang daftar tugas, ringkasan mata kuliah, dan daftar mata kuliah yang sedang dibuka. */
    private fun muatUlangDaftar() {
        lifecycleScope.launch {
            val ms = lihatMatkul
            val (d, r, m) = Gudang.pakai(this@MainActivity) { db ->
                Triple(db.daftarTugas(), db.ringkasanMatkul(), ms?.let { db.daftarMatkul(it) })
            }
            daftar = d
            matkul.putAll(r)
            if (m != null) matkulSumber = m
            versiData++
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

    private fun bukaMatkul(sumber: String) {
        lifecycleScope.launch {
            matkulSumber = Gudang.pakai(this@MainActivity) { it.daftarMatkul(sumber) }
            lihatMatkul = sumber
        }
    }

    private fun atur(sumber: String, externalId: String, aksi: String?) {
        lifecycleScope.launch {
            matkulSumber = Gudang.pakai(this@MainActivity) { db ->
                db.aturPengecualian(sumber, externalId, aksi)
                db.daftarMatkul(sumber)
            }
        }
    }

    private fun simpanSemesterDariIsian() {
        val nilai = LABEL_SEMESTER.keys.associateWith { isianSemester[it].orEmpty().trim() }
        periksaSemester(nilai)?.let {
            umum = it
            return
        }
        lifecycleScope.launch {
            umum = try {
                Gudang.pakai(this@MainActivity) { it.simpanSemester(nilai) }
                semester = nilai
                "Semester disimpan; berlaku mulai Ambil berikutnya."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Semester tidak bisa disimpan: ${e.javaClass.simpleName}"
            }
        }
    }

    /** Buka lagi isian dengan nilai sekarang; Ambil mati sampai disimpan ulang. */
    private fun ubahSemester(sekarang: Map<String, String>) {
        isianSemester.clear()
        isianSemester.putAll(sekarang)
        semester = null
    }
}
