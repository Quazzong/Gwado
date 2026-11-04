package io.github.quazzong.gwado.ui.layar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.fetch.JENDELA_HINDARI
import io.github.quazzong.gwado.layanan.DAFTAR_LAYANAN
import io.github.quazzong.gwado.layanan.InfoLayanan
import io.github.quazzong.gwado.layanan.Pengambil
import io.github.quazzong.gwado.segar.StatusSumber
import io.github.quazzong.gwado.segar.Tingkat
import io.github.quazzong.gwado.segar.statusSumber
import io.github.quazzong.gwado.ui.komponen.Chip
import io.github.quazzong.gwado.ui.komponen.IkonLayanan
import io.github.quazzong.gwado.ui.komponen.JenisChip
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.StripSub
import io.github.quazzong.gwado.ui.komponen.TombolGaris
import io.github.quazzong.gwado.ui.komponen.TombolGarisIkon
import io.github.quazzong.gwado.ui.komponen.TombolPermen
import io.github.quazzong.gwado.ui.komponen.TombolPlum
import io.github.quazzong.gwado.ui.komponen.rememberSekarang
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

private val BULAN_NAMA = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)

/** "2026-08-01" → "1 Agustus 2026"; teks lain dikembalikan apa adanya. */
private fun tanggalPanjang(teks: String): String =
    runCatching { LocalDate.parse(teks) }.getOrNull()
        ?.let { "${it.dayOfMonth} ${BULAN_NAMA[it.monthValue - 1]} ${it.year}" } ?: teks

/** Chip kartu layanan menurut label status sumber. */
internal fun chipStatus(st: StatusSumber): Pair<String, JenisChip> = when (st.label) {
    "token ditolak" -> "login ulang" to JenisChip.LEWAT
    "gagal" -> "gagal" to JenisChip.LEWAT
    "perlu dicek" -> "perlu dicek" to JenisChip.MEPET
    "basi" -> "data lama" to JenisChip.MEPET
    "belum ada data" -> "belum diambil" to JenisChip.REDUP
    else -> "sukses" to JenisChip.NETRAL
}

/**
 * Tab Layanan: semester aktif, kartu tiap layanan tersambung dengan statusnya, dan
 * Ambil semua. [versi] naik setiap kali database atau token berubah.
 */
@Composable
fun LayarLayanan(
    pengambil: Pengambil,
    versi: Int,
    semester: Map<String, String>?,
    matkul: Map<String, String>,
    onUbahSemester: () -> Unit,
    onMatkul: (String) -> Unit,
    modifier: Modifier = Modifier,
    alatUji: @Composable () -> Unit = {},
) {
    val w = GwadoTema.warna
    val sekarang = rememberSekarang()
    val daftar by produceState<List<InfoLayanan>?>(null, versi) {
        value = try {
            pengambil.daftarTersambung()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
    val status = daftar?.let { d -> statusSumber(d.map { it.baris }, sekarang) }.orEmpty()
    val keterangan = listOf("Ambil manual bisa diulang setiap 1 menit.") + DAFTAR_LAYANAN.mapNotNull { l ->
        JENDELA_HINDARI[l.id]?.let { (m, s) ->
            "${l.label} tidak dihubungi pukul %02d.00 sampai %02d.00 karena situsnya sedang backup.".format(m, s)
        }
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(Jarak.tepiLayar),
        verticalArrangement = Arrangement.spacedBy(Jarak.s8),
    ) {
        item(key = "strip") { StripSub("Layanan", Modifier.padding(bottom = Jarak.s8), benih = 13, ukuran = 26.sp) }
        item(key = "semester") { PanelSemester(semester, onUbahSemester) }
        val d = daftar
        if (d != null && d.isEmpty()) {
            item(key = "kosong") {
                Kotak(Modifier.fillMaxWidth(), putus = true, kilau = null, bayangan = 0.dp) {
                    Text("Belum ada layanan tersambung.", style = Huruf.meta, color = w.tintaRedup)
                }
            }
        }
        if (d != null) {
            items(d.zip(status), key = { it.first.layanan.id }) { (info, st) ->
                KartuLayanan(
                    info, st, matkul[info.layanan.id], pengambil.status[info.layanan.id],
                    onAmbil = { pengambil.ambil(info.layanan) },
                    onLupakan = { pengambil.lupakan(info.layanan) },
                    onMatkul = { onMatkul(info.layanan.id) },
                )
            }
        }
        item(key = "ambil") {
            Column(Modifier.padding(top = Jarak.s8), verticalArrangement = Arrangement.spacedBy(Jarak.s8)) {
                TombolPermen(
                    if (pengambil.sibuk) "Mengambil…" else "Ambil semua sekarang",
                    { pengambil.ambilSemua() },
                    Modifier.fillMaxWidth(),
                )
                if (pengambil.pesan.isNotEmpty()) {
                    Text(pengambil.pesan, style = Huruf.meta.copy(fontWeight = FontWeight.W800), color = w.tinta)
                }
                Text(keterangan.joinToString(" "), style = Huruf.meta, color = w.tintaRedup)
            }
        }
        item(key = "uji") {
            Column(Modifier.padding(top = Jarak.s24), verticalArrangement = Arrangement.spacedBy(Jarak.s8)) {
                Text("ALAT UJI SEMENTARA", style = Huruf.label, color = w.tintaRedup)
                alatUji()
            }
        }
    }
}

@Composable
private fun PanelSemester(semester: Map<String, String>?, onUbah: () -> Unit) {
    val w = GwadoTema.warna
    Kotak(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Jarak.s8)) {
            Column(Modifier.weight(1f)) {
                Text("SEMESTER AKTIF", style = Huruf.label, color = w.tintaRedup)
                if (semester == null) {
                    Text("Belum dikonfirmasi", style = Huruf.angkaUbin, color = w.tinta, modifier = Modifier.padding(top = 2.dp))
                    Text("Ambil belum bisa dipakai sampai semester dikonfirmasi.", style = Huruf.meta, color = w.tintaRedup)
                } else {
                    Text(
                        semester["semester_label"].orEmpty(),
                        style = Huruf.angkaUbin,
                        color = w.tinta,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        "Kode ${semester["semester_kode"].orEmpty()}, mulai ${tanggalPanjang(semester["semester_mulai"].orEmpty())}",
                        style = Huruf.meta,
                        color = w.tintaRedup,
                    )
                }
            }
            if (semester != null) TombolGaris("Ubah", onUbah)
        }
    }
}

@Composable
private fun KartuLayanan(
    info: InfoLayanan,
    st: StatusSumber,
    ringkasan: String?,
    pesanAmbil: String?,
    onAmbil: () -> Unit,
    onLupakan: () -> Unit,
    onMatkul: () -> Unit,
) {
    val w = GwadoTema.warna
    val moodle = info.layanan.situs != null
    val ditolak = st.label == "token ditolak"
    val belumLogin = moodle && !info.adaToken
    val (teksChip, jenis) = chipStatus(st)
    val kalimat = buildString {
        append(
            when {
                ditolak && moodle -> "Situs menolak login. Tugas lama tetap tersimpan."
                ditolak -> "Google menolak izin. Tugas lama tetap tersimpan."
                st.tingkat == Tingkat.NORMAL -> "Diambil ${st.label} WIB."
                else -> st.pesan.orEmpty().removePrefix("${st.nama} · ").replaceFirstChar { it.uppercase() } + "."
            },
        )
        if (belumLogin && !ditolak) append(" Belum login di HP ini.")
    }
    val latar = when (st.tingkat) {
        Tingkat.KUNING -> w.latarMepet
        Tingkat.MERAH -> w.latarLewat
        Tingkat.NORMAL -> w.kartu
    }
    Kotak(Modifier.fillMaxWidth(), latar = SolidColor(latar)) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    IkonLayanan(info.baris.nama, ukuran = 18.dp)
                    Text(st.nama, style = Huruf.judulKartu, color = w.tinta)
                }
                Chip(teksChip, jenis = jenis)
            }
            Text(kalimat, style = Huruf.meta, color = w.tintaRedup, modifier = Modifier.padding(top = 4.dp))
            ringkasan?.let { Text("Mata kuliah: $it", style = Huruf.meta, color = w.tintaRedup) }
            if (!pesanAmbil.isNullOrBlank()) {
                Text(
                    pesanAmbil,
                    style = Huruf.meta.copy(fontWeight = FontWeight.W800),
                    color = w.tinta,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = Jarak.s8),
                horizontalArrangement = Arrangement.spacedBy(Jarak.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (ditolak || belumLogin) TombolPlum(if (moodle) "Login" else "Beri izin lagi", onAmbil)
                TombolGaris("Mata kuliah", onMatkul)
                Spacer(Modifier.weight(1f))
                Box {
                    var buka by remember { mutableStateOf(false) }
                    TombolGarisIkon(R.drawable.ikon_menu, "Menu ${st.nama}", { buka = true })
                    DropdownMenu(
                        expanded = buka,
                        onDismissRequest = { buka = false },
                        shape = Bentuk.kartu,
                        containerColor = w.kartu,
                        border = BorderStroke(Jarak.garis, w.garis),
                    ) {
                        val gaya = Huruf.isi.copy(fontWeight = FontWeight.W800)
                        DropdownMenuItem(
                            text = { Text("Ambil layanan ini", style = gaya, color = w.tinta) },
                            onClick = { buka = false; onAmbil() },
                        )
                        if (moodle && info.adaToken) {
                            DropdownMenuItem(
                                text = { Text("Lupakan login", style = gaya, color = w.tinta) },
                                onClick = { buka = false; onLupakan() },
                            )
                        }
                    }
                }
            }
        }
    }
}
