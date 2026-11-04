package io.github.quazzong.gwado.ui.layar

import io.github.quazzong.gwado.ui.komponen.latarLayar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.beranda.kuliahMulai
import io.github.quazzong.gwado.beranda.simpanKuliahMulai
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.semester.periksaSemester
import io.github.quazzong.gwado.semester.semesterTersimpan
import io.github.quazzong.gwado.semester.simpanSemester
import io.github.quazzong.gwado.semester.tanggalWib
import io.github.quazzong.gwado.semester.tebakSemester
import io.github.quazzong.gwado.ui.komponen.IsianTeks
import io.github.quazzong.gwado.ui.komponen.KotakInfo
import io.github.quazzong.gwado.ui.komponen.LabelIsian
import io.github.quazzong.gwado.ui.komponen.Pemilih
import io.github.quazzong.gwado.ui.komponen.PesanGalat
import io.github.quazzong.gwado.ui.komponen.StripSub
import io.github.quazzong.gwado.ui.komponen.TombolGaris
import io.github.quazzong.gwado.ui.komponen.TombolPermen
import io.github.quazzong.gwado.ui.komponen.TombolPlumIkon
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val HARI = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
private val BULAN = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)

/** 2026-08-01 → "Sabtu, 1 Agustus 2026". */
internal fun tanggalLengkap(t: LocalDate): String =
    "${HARI[t.dayOfWeek.value - 1]}, ${t.dayOfMonth} ${BULAN[t.monthValue - 1]} ${t.year}"

/** Isian tanggal yang sedang dipilih lewat dialog. */
private enum class Pilih { MULAI, KULIAH }

/**
 * Ubah semester aktif (label kategori Brone, kode nama singkat Eling, tanggal mulai Classroom)
 * dan tanggal kuliah pertama. Semester yang belum dikonfirmasi diisi tebakan dari tanggal hari ini.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarSemester(onKembali: () -> Unit, onTersimpan: () -> Unit) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val cakupan = rememberCoroutineScope()
    var label by rememberSaveable { mutableStateOf("") }
    var kode by rememberSaveable { mutableStateOf("") }
    var mulai by rememberSaveable { mutableStateOf<String?>(null) }    // YYYY-MM-DD
    var kuliah by rememberSaveable { mutableStateOf<String?>(null) }   // YYYY-MM-DD, null = belum diisi
    var dimuat by rememberSaveable { mutableStateOf(false) }
    var galat by remember { mutableStateOf<String?>(null) }
    var sibuk by remember { mutableStateOf(false) }
    var pilih by remember { mutableStateOf<Pilih?>(null) }

    LaunchedEffect(Unit) {
        if (dimuat) return@LaunchedEffect
        try {
            val (sm, km) = Gudang.pakai(konteks) { it.semesterTersimpan() to it.kuliahMulai() }
            val nilai = sm ?: tebakSemester(tanggalWib(Instant.now()))
            label = nilai["semester_label"].orEmpty()
            kode = nilai["semester_kode"].orEmpty()
            mulai = nilai["semester_mulai"]
            kuliah = km?.toString()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            galat = "Semester tersimpan tidak bisa dibaca. Isi ulang di bawah."
        }
        dimuat = true
    }

    val simpan: () -> Unit = simpan@{
        if (sibuk) return@simpan
        val nilai = mapOf(
            "semester_label" to label.trim(),
            "semester_kode" to kode.trim(),
            "semester_mulai" to mulai.orEmpty(),
        )
        periksaSemester(nilai)?.let {
            galat = it
            return@simpan
        }
        sibuk = true
        cakupan.launch {
            try {
                Gudang.pakai(konteks) { db ->
                    db.simpanSemester(nilai)
                    db.simpanKuliahMulai(kuliah?.let { LocalDate.parse(it) })
                }
                onTersimpan()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                galat = "Semester tidak bisa disimpan. Coba lagi."
            } finally {
                sibuk = false
            }
        }
    }

    Column(Modifier.fillMaxSize().latarLayar().safeDrawingPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Jarak.tepiLayar),
            verticalArrangement = Arrangement.spacedBy(Jarak.s16),
        ) {
            StripSub("Semester", kiri = { TombolPlumIkon(R.drawable.ikon_kembali, "Kembali", onKembali) })
            Text(
                "Hanya tugas dari mata kuliah semester ini yang ditampilkan. Tiap layanan punya cara sendiri " +
                    "untuk mengenali semester, jadi ketiga isian ini dipakai bersama.",
                style = Huruf.meta,
                color = w.tintaRedup,
            )
            Column {
                LabelIsian("Label semester")
                IsianTeks(label, { label = it; galat = null }, petunjuk = "Ganjil 2026/2027", satuBaris = true)
                Keterangan("Sama dengan kategori kelas di Brone.")
            }
            Column {
                LabelIsian("Kode semester")
                IsianTeks(kode, { kode = it; galat = null }, petunjuk = "20261", satuBaris = true)
                Keterangan("Ada di nama singkat kelas Eling.")
            }
            Column {
                LabelIsian("Mulai semester")
                Pemilih(
                    mulai?.let { runCatching { tanggalLengkap(LocalDate.parse(it)) }.getOrNull() ?: it } ?: "Pilih tanggal",
                    { pilih = Pilih.MULAI },
                    R.drawable.ikon_kalender,
                    redup = mulai == null,
                )
                Keterangan("Kelas Classroom yang dibuat sejak tanggal ini diikuti.")
            }
            Column {
                LabelIsian("Kuliah pertama", opsional = true)
                Row(horizontalArrangement = Arrangement.spacedBy(Jarak.s8), verticalAlignment = Alignment.CenterVertically) {
                    Pemilih(
                        kuliah?.let { tanggalLengkap(LocalDate.parse(it)) } ?: "Belum diisi",
                        { pilih = Pilih.KULIAH },
                        R.drawable.ikon_kalender,
                        Modifier.weight(1f),
                        redup = kuliah == null,
                    )
                    if (kuliah != null) TombolGaris("Kosongkan", { kuliah = null })
                }
                val belumMulai = kuliah?.let { LocalDate.parse(it) > tanggalWib(Instant.now()) } == true
                Keterangan(
                    if (belumMulai) "Tanggal ini belum lewat, jadi MINGGU KE-n baru tampil di beranda mulai tanggal ini."
                    else "Minggu (Senin–Minggu) yang memuat tanggal ini dihitung MINGGU KE-1 di beranda.",
                )
            }
            KotakInfo(
                "Semester baru berlaku mulai Ambil berikutnya. Tugas dari mata kuliah semester lain " +
                    "disembunyikan, tidak dihapus.",
            )
            galat?.let { PesanGalat(it) }
        }
        Row(Modifier.fillMaxWidth().padding(Jarak.tepiLayar)) {
            TombolPermen("Simpan semester", simpan, Modifier.weight(1f))
        }
    }

    pilih?.let { p ->
        val awal = (if (p == Pilih.MULAI) mulai else kuliah)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: tanggalWib(Instant.now())
        DialogTanggal(
            awal,
            onPilih = {
                if (p == Pilih.MULAI) mulai = it.toString() else kuliah = it.toString()
                galat = null
                pilih = null
            },
            onTutup = { pilih = null },
        )
    }
}

@Composable
private fun Keterangan(teks: String) {
    Text(teks, style = Huruf.meta, color = GwadoTema.warna.tintaRedup, modifier = Modifier.padding(top = 6.dp))
}

/** Pemilih tanggal Material 3. DatePicker bekerja dalam milidetik UTC tengah malam. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogTanggal(awal: LocalDate, onPilih: (LocalDate) -> Unit, onTutup: () -> Unit) {
    val warna = ButtonDefaults.textButtonColors(contentColor = GwadoTema.warna.tinta)
    val keadaan = rememberDatePickerState(
        initialSelectedDateMillis = awal.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onTutup,
        confirmButton = {
            TextButton({
                keadaan.selectedDateMillis?.let { onPilih(Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate()) }
                    ?: onTutup()
            }, colors = warna) { Text("Pilih") }
        },
        dismissButton = { TextButton(onTutup, colors = warna) { Text("Batal") } },
    ) {
        DatePicker(keadaan)
    }
}
