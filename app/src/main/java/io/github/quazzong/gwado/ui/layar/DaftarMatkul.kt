package io.github.quazzong.gwado.ui.layar

import io.github.quazzong.gwado.ui.komponen.LayarPenuh
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.layanan.DAFTAR_LAYANAN
import io.github.quazzong.gwado.semester.Matkul
import io.github.quazzong.gwado.semester.aturPengecualian
import io.github.quazzong.gwado.semester.daftarMatkul
import io.github.quazzong.gwado.semester.semesterTersimpan
import io.github.quazzong.gwado.ui.komponen.Chip
import io.github.quazzong.gwado.ui.komponen.JenisChip
import io.github.quazzong.gwado.ui.komponen.KotakInfo
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.Segmen
import io.github.quazzong.gwado.ui.komponen.StripSub
import io.github.quazzong.gwado.ui.komponen.TombolPlumIkon
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Pilihan pengecualian: null = ikut aturan semester. */
private val PILIHAN: List<Pair<String?, String>> = listOf(null to "Otomatis", "ikuti" to "Ikuti", "abaikan" to "Abaikan")

private val BULAN = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)

/** Satu kalimat aturan semester yang dipakai sumber ini, atau null kalau semester belum dikonfirmasi. */
private fun aturan(sumber: String, semester: Map<String, String>?): String? {
    if (semester == null) return null
    return when (sumber) {
        "brone" -> "Aturan semester ini: kategori ${semester["semester_label"]}."
        "eling" -> "Aturan semester ini: nama singkat kelas memuat ${semester["semester_kode"]}."
        "classroom" -> {
            val t = runCatching { LocalDate.parse(semester["semester_mulai"]) }.getOrNull()
            val teks = t?.let { "${it.dayOfMonth} ${BULAN[it.monthValue - 1]} ${it.year}" } ?: semester["semester_mulai"]
            "Aturan semester ini: kelas dibuat sejak $teks."
        }
        else -> null
    }
}

/** Pilihan yang belum terlihat di status DIIKUTI/DILEWATI, karena baru diterapkan saat Ambil berikutnya. */
private fun menunggu(m: Matkul): Boolean = when (m.aksi) {
    "ikuti" -> !m.diikuti
    "abaikan" -> m.diikuti
    else -> m.alasan?.startsWith("pengecualian manual") == true
}

/**
 * Semua mata kuliah satu layanan: status diikuti/dilewati dari pengambilan terakhir dan alasannya.
 * Ketuk satu mata kuliah untuk memilih Otomatis, Ikuti, atau Abaikan.
 */
@Composable
fun LayarMatkul(sumber: String, onKembali: () -> Unit) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val cakupan = rememberCoroutineScope()
    val label = DAFTAR_LAYANAN.firstOrNull { it.id == sumber }?.label ?: sumber
    var daftar by remember { mutableStateOf<List<Matkul>?>(null) }
    var semester by remember { mutableStateOf<Map<String, String>?>(null) }
    var gagal by remember { mutableStateOf(false) }
    var buka by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(sumber) {
        try {
            val (sm, d) = Gudang.pakai(konteks) { it.semesterTersimpan() to it.daftarMatkul(sumber) }
            semester = sm
            daftar = d
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            gagal = true
        }
    }
    val pilih: (Matkul, String?) -> Unit = { m, aksi ->
        cakupan.launch {
            try {
                daftar = Gudang.pakai(konteks) { db ->
                    db.aturPengecualian(sumber, m.externalId, aksi)
                    db.daftarMatkul(sumber)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                gagal = true
            }
        }
    }

    LayarPenuh("layar-matkul-$sumber") {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Jarak.tepiLayar),
            verticalArrangement = Arrangement.spacedBy(Jarak.s12),
        ) {
            val d = daftar
            StripSub(
                label,
                kiri = { TombolPlumIkon(R.drawable.ikon_kembali, "Kembali", onKembali) },
                kanan = { if (d != null) Chip("${d.size} mata kuliah", jenis = JenisChip.PUTIH) },
            )
            if (gagal) {
                Text("Daftar mata kuliah tidak bisa dibuka. Coba lagi nanti.", style = Huruf.isi, color = w.galat)
            }
            when {
                d == null -> Unit
                d.isEmpty() -> Text(
                    "Belum ada mata kuliah. Ambil $label dulu supaya daftar kelasnya terbaca.",
                    style = Huruf.isi,
                    color = w.tintaRedup,
                )
                else -> {
                    Text(
                        listOfNotNull(
                            aturan(sumber, semester),
                            "Ketuk mata kuliah untuk mengikuti atau mengabaikannya secara manual.",
                        ).joinToString(" "),
                        style = Huruf.meta,
                        color = w.tintaRedup,
                    )
                    Kotak(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
                        Column {
                            d.forEachIndexed { i, m ->
                                if (i > 0) Box(Modifier.fillMaxWidth().height(Jarak.garis).background(w.garisHalus))
                                BarisMatkul(
                                    m,
                                    terbuka = buka == m.externalId,
                                    onKetuk = { buka = if (buka == m.externalId) null else m.externalId },
                                    onPilih = { aksi -> pilih(m, aksi) },
                                )
                            }
                        }
                    }
                    KotakInfo(
                        "Perubahan berlaku saat pengambilan berikutnya. Tugas dari mata kuliah yang dilewati " +
                            "disembunyikan, tidak dihapus.",
                    )
                }
            }
        }
    }
}

@Composable
private fun BarisMatkul(m: Matkul, terbuka: Boolean, onKetuk: () -> Unit, onPilih: (String?) -> Unit) {
    val w = GwadoTema.warna
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onKetuk)
            .padding(vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Jarak.s8),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(m.nama, style = Huruf.judulKartu, color = if (m.diikuti) w.tinta else w.tintaRedup)
                Text(m.kodeMk ?: "tanpa kode MK", style = Huruf.meta, color = w.tintaRedup)
                m.alasan?.takeIf { it.isNotBlank() }?.let {
                    Text(it.replaceFirstChar { c -> c.uppercase() }, style = Huruf.meta, color = w.tintaRedup)
                }
            }
            if (m.diikuti) Chip("diikuti", jenis = JenisChip.AKTIF) else Chip("dilewati", jenis = JenisChip.REDUP)
        }
        if (menunggu(m)) {
            Text(
                "Berubah saat Ambil berikutnya.",
                style = Huruf.meta,
                color = w.tinta,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (terbuka) {
            Segmen(PILIHAN, m.aksi, onPilih, Modifier.padding(top = Jarak.s8))
        }
    }
}
