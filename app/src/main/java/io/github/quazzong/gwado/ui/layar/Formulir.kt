package io.github.quazzong.gwado.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.tugas.TugasGalat
import io.github.quazzong.gwado.tugas.ambilTugas
import io.github.quazzong.gwado.tugas.pilihanMatkul
import io.github.quazzong.gwado.tugas.tambahManual
import io.github.quazzong.gwado.tugas.ubahManual
import io.github.quazzong.gwado.ui.komponen.IsianTeks
import io.github.quazzong.gwado.ui.komponen.KotakInfo
import io.github.quazzong.gwado.ui.komponen.LabelIsian
import io.github.quazzong.gwado.ui.komponen.LembarBawah
import io.github.quazzong.gwado.ui.komponen.Pemilih
import io.github.quazzong.gwado.ui.komponen.PesanGalat
import io.github.quazzong.gwado.ui.komponen.RadioKue
import io.github.quazzong.gwado.ui.komponen.Sakelar
import io.github.quazzong.gwado.ui.komponen.StripSub
import io.github.quazzong.gwado.ui.komponen.TombolPermen
import io.github.quazzong.gwado.ui.komponen.TombolPlumIkon
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.ui.tema.Nunito
import io.github.quazzong.gwado.waktu.WIB
import io.github.quazzong.gwado.waktu.dariDb
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val MAKS_JUDUL = 200
private val HARI = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
private val BULAN = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

private val GALAT = mapOf(
    "judul" to "Judul belum diisi. Tulis nama tugasnya dulu.",
    "kode_mk" to "Mata kuliah ini tidak dikenal lagi. Pilih ulang dari daftar.",
    "umum" to "Tugas tidak bisa disimpan. Coba lagi.",
)

/** Besok 23.59 WIB: deadline bawaan tugas baru. */
private fun besokMalam(): Long =
    LocalDate.now(WIB).plusDays(1).atTime(23, 59).atOffset(WIB).toInstant().toEpochMilli()

private fun labelTanggal(ms: Long): String {
    val d = Instant.ofEpochMilli(ms).atOffset(WIB)
    return "${HARI[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${BULAN[d.monthValue - 1]}"
}

private fun labelJam(ms: Long): String {
    val d = Instant.ofEpochMilli(ms).atOffset(WIB)
    return d.hour.toString().padStart(2, '0') + "." + d.minute.toString().padStart(2, '0')
}

/**
 * Formulir tambah (id null) atau ubah tugas manual. Isian tetap ada saat layar diputar;
 * data lama hanya dimuat sekali.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarFormulir(id: String?, onKembali: () -> Unit, onTersimpan: () -> Unit) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val cakupan = rememberCoroutineScope()
    var judul by rememberSaveable { mutableStateOf("") }
    var catatan by rememberSaveable { mutableStateOf("") }
    var kode by rememberSaveable { mutableStateOf<String?>(null) }
    var tanpa by rememberSaveable { mutableStateOf(false) }
    var deadlineMs by rememberSaveable { mutableStateOf(besokMalam()) }
    var dimuat by rememberSaveable { mutableStateOf(id == null) }
    var galat by remember { mutableStateOf<String?>(null) }
    var sibuk by remember { mutableStateOf(false) }
    var lembar by remember { mutableStateOf(false) }
    var pilihTanggal by remember { mutableStateOf(false) }
    var pilihJam by remember { mutableStateOf(false) }
    val matkul by produceState(emptyList<Pair<String, String>>()) {
        value = runCatching { Gudang.pakai(konteks) { it.pilihanMatkul() } }.getOrDefault(emptyList())
    }
    LaunchedEffect(id) {
        if (id != null && !dimuat) {
            Gudang.pakai(konteks) { it.ambilTugas(id) }?.let { t ->
                judul = t.judul
                catatan = t.deskripsi.orEmpty()
                kode = t.kodeMkManual
                tanpa = t.deadline == null
                t.deadline?.let { deadlineMs = dariDb(it).toEpochMilli() }
            }
            dimuat = true
        }
    }

    val simpan: () -> Unit = {
        when {
            sibuk -> Unit
            judul.isBlank() -> galat = "judul"
            else -> {
                sibuk = true
                cakupan.launch {
                    try {
                        val dl = if (tanpa) null else Instant.ofEpochMilli(deadlineMs)
                        Gudang.pakai(konteks) { db ->
                            if (id == null) {
                                db.tambahManual(judul, dl, catatan, Instant.now(), kode)
                            } else {
                                db.ubahManual(
                                    id,
                                    mapOf("judul" to judul, "deadline" to dl, "deskripsi" to catatan, "kode_mk" to kode),
                                    Instant.now(),
                                )
                            }
                        }
                        onTersimpan()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: TugasGalat) {
                        galat = e.kolom ?: "umum"
                    } catch (e: Exception) {
                        galat = "umum"
                    } finally {
                        sibuk = false
                    }
                }
            }
        }
    }

    val namaMk = kode?.let { k -> matkul.firstOrNull { it.first == k }?.second ?: k } ?: "Tanpa mata kuliah"

    Column(Modifier.fillMaxSize().background(w.latar).safeDrawingPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Jarak.tepiLayar),
            verticalArrangement = Arrangement.spacedBy(Jarak.s16),
        ) {
            StripSub(
                if (id == null) "Tugas baru" else "Ubah tugas",
                kiri = { TombolPlumIkon(R.drawable.ikon_kembali, "Kembali", onKembali) },
            )
            Column {
                LabelIsian("Judul tugas")
                IsianTeks(
                    judul,
                    {
                        if (it.codePointCount(0, it.length) <= MAKS_JUDUL) judul = it
                        if (galat == "judul") galat = null
                    },
                    gaya = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.W800, fontSize = 18.sp, lineHeight = 1.3.em),
                    petunjuk = "Laporan kelompok bab 2",
                    salah = galat == "judul",
                    minTinggi = 76.dp,
                )
                if (galat == "judul") {
                    PesanGalat(GALAT.getValue("judul"))
                } else {
                    Text(
                        "${judul.codePointCount(0, judul.length)}/$MAKS_JUDUL",
                        style = Huruf.meta.copy(fontSize = 11.sp),
                        color = w.tintaRedup,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
            Column {
                LabelIsian("Mata kuliah")
                Pemilih(namaMk, { lembar = true }, R.drawable.ikon_bawah, kecil = kode, redup = kode == null)
                if (galat == "kode_mk") PesanGalat(GALAT.getValue("kode_mk"))
            }
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LabelIsian("Deadline", Modifier.padding(top = 6.dp))
                    Sakelar(tanpa, { tanpa = it }, "Tanpa deadline")
                }
                if (tanpa) {
                    KotakInfo("Tugas tampil paling bawah dengan tanda Tanpa deadline.")
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Jarak.s8)) {
                        Pemilih(labelTanggal(deadlineMs), { pilihTanggal = true }, R.drawable.ikon_kalender, Modifier.weight(1.5f))
                        Pemilih(labelJam(deadlineMs), { pilihJam = true }, R.drawable.ikon_jam, Modifier.weight(1f))
                    }
                }
            }
            Column {
                LabelIsian("Catatan", opsional = true)
                IsianTeks(catatan, { catatan = it }, petunjuk = "Dari grup WA kelas, dikumpulkan lewat ketua kelompok", minTinggi = 68.dp)
            }
            if (galat == "umum") PesanGalat(GALAT.getValue("umum"))
        }
        Row(Modifier.fillMaxWidth().padding(Jarak.tepiLayar)) {
            TombolPermen("Simpan tugas", simpan, Modifier.weight(1f))
        }
    }

    if (lembar) {
        LembarBawah({ lembar = false }) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("Pilih mata kuliah", style = Huruf.judulBagian, color = w.tinta)
                Text("diikuti semester ini", style = Huruf.meta, color = w.tintaRedup)
            }
            val pilihan: List<Pair<String?, String>> = matkul + (null to "Tanpa mata kuliah")
            LazyColumn(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                items(pilihan, key = { it.first ?: "" }) { (k, nama) ->
                    val dipilih = k == kode
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .background(if (dipilih) w.ubin else Color.Transparent, RoundedCornerShape(12.dp))
                            .selectable(selected = dipilih, role = Role.RadioButton, onClick = {
                                kode = k
                                if (galat == "kode_mk") galat = null
                                lembar = false
                            })
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioKue(dipilih)
                        Column {
                            Text(
                                nama,
                                style = Huruf.isi.copy(fontSize = 14.sp, fontWeight = FontWeight.W800, lineHeight = 1.2.em),
                                color = if (k != null) w.tinta else w.tintaRedup,
                            )
                            if (k != null) Text(k, style = Huruf.meta.copy(fontSize = 11.sp), color = w.tintaRedup)
                        }
                    }
                }
            }
        }
    }

    val warnaTombol = ButtonDefaults.textButtonColors(contentColor = w.tinta)
    if (pilihTanggal) {
        val lama = Instant.ofEpochMilli(deadlineMs).atOffset(WIB)
        val keadaan = rememberDatePickerState(
            initialSelectedDateMillis = lama.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pilihTanggal = false },
            confirmButton = {
                TextButton({
                    keadaan.selectedDateMillis?.let { ms ->
                        val tanggal = Instant.ofEpochMilli(ms).atOffset(ZoneOffset.UTC).toLocalDate()
                        deadlineMs = tanggal.atTime(lama.toLocalTime()).atOffset(WIB).toInstant().toEpochMilli()
                    }
                    pilihTanggal = false
                }, colors = warnaTombol) { Text("Pilih") }
            },
            dismissButton = { TextButton({ pilihTanggal = false }, colors = warnaTombol) { Text("Batal") } },
        ) {
            DatePicker(keadaan)
        }
    }
    if (pilihJam) {
        val lama = Instant.ofEpochMilli(deadlineMs).atOffset(WIB)
        val keadaan = rememberTimePickerState(lama.hour, lama.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pilihJam = false },
            confirmButton = {
                TextButton({
                    deadlineMs = lama.toLocalDate().atTime(keadaan.hour, keadaan.minute).atOffset(WIB).toInstant().toEpochMilli()
                    pilihJam = false
                }, colors = warnaTombol) { Text("Pilih") }
            },
            dismissButton = { TextButton({ pilihJam = false }, colors = warnaTombol) { Text("Batal") } },
            text = { TimePicker(keadaan) },
        )
    }
}
