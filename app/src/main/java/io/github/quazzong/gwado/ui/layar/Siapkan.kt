package io.github.quazzong.gwado.ui.layar

import io.github.quazzong.gwado.ui.komponen.latarLayar
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import io.github.quazzong.gwado.layanan.DAFTAR_LAYANAN
import io.github.quazzong.gwado.layanan.InfoLayanan
import io.github.quazzong.gwado.layanan.Layanan
import io.github.quazzong.gwado.layanan.Pengambil
import io.github.quazzong.gwado.semester.tanggalWib
import io.github.quazzong.gwado.semester.tebakSemester
import io.github.quazzong.gwado.siapkan.kurangSiap
import io.github.quazzong.gwado.ui.AturSemester
import io.github.quazzong.gwado.ui.Masuk
import io.github.quazzong.gwado.ui.komponen.Chip
import io.github.quazzong.gwado.ui.komponen.IkonLayanan
import io.github.quazzong.gwado.ui.komponen.JenisChip
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.Pemuat
import io.github.quazzong.gwado.ui.komponen.PesanGalat
import io.github.quazzong.gwado.ui.komponen.Taburan
import io.github.quazzong.gwado.ui.komponen.TeksStiker
import io.github.quazzong.gwado.ui.komponen.TombolGaris
import io.github.quazzong.gwado.ui.komponen.TombolPermen
import io.github.quazzong.gwado.ui.komponen.TombolPlum
import io.github.quazzong.gwado.ui.komponen.gradasiAksen
import io.github.quazzong.gwado.ui.komponen.gradasiCss
import io.github.quazzong.gwado.ui.komponen.pathGelombang
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.Fredoka
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Layar pertama kali dibuka: 1 sambungkan layanan, 2 konfirmasi semester, lalu Mulai.
 * Mulai menjalankan Ambil semua dan menampilkan layar tunggu; [onSelesai] dipanggil
 * setelah pengambilan pertama selesai.
 */
@Composable
fun LayarSiapkan(
    pengambil: Pengambil,
    versi: Int,
    semester: Map<String, String>?,
    onKonfirmasi: (Map<String, String>) -> Unit,
    onBuka: (NavKey) -> Unit,
    onSelesai: () -> Unit,
) {
    val w = GwadoTema.warna
    val tersambung by produceState(emptyList<InfoLayanan>(), versi) {
        value = try {
            pengambil.daftarTersambung()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
    val belum = DAFTAR_LAYANAN.filter { l -> tersambung.none { it.layanan.id == l.id } }
    var mulai by rememberSaveable { mutableStateOf(false) }
    var kurang by remember { mutableStateOf<String?>(null) }
    var lembar by remember { mutableStateOf(false) }
    var izinGoogle by remember { mutableStateOf<Layanan?>(null) }

    if (mulai) {
        LaunchedEffect(pengambil.sibuk) {
            if (!pengambil.sibuk) onSelesai()
        }
        LayarTunggu()
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .latarLayar()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(Jarak.tepiLayar),
        verticalArrangement = Arrangement.spacedBy(Jarak.s12),
    ) {
        KepalaSiapkan()

        Kotak(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NomorKue(1, selesai = tersambung.isNotEmpty())
                Column(Modifier.weight(1f)) {
                    Text("Layanan", style = Huruf.judulKartu, color = w.tinta)
                    Text(
                        if (tersambung.isEmpty()) "Sambungkan situs tempat dosen memberi tugas."
                        else "Tambah lagi kapan saja dari tab Layanan.",
                        style = Huruf.meta,
                        color = w.tintaRedup,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                    tersambung.forEachIndexed { i, info ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(Jarak.garis).background(w.garisHalus))
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IkonLayanan(info.layanan.nama, ukuran = 18.dp)
                            Text(info.layanan.label, style = Huruf.judulKartu, color = w.tinta, modifier = Modifier.weight(1f))
                            Chip("tersambung", jenis = JenisChip.NETRAL)
                        }
                    }
                    for (l in DAFTAR_LAYANAN) {
                        pengambil.status[l.id]?.takeIf { it.isNotBlank() }?.let {
                            Text("${l.label}: $it", style = Huruf.meta.copy(fontWeight = FontWeight.W800), color = w.tinta)
                        }
                    }
                    if (belum.isNotEmpty()) {
                        TombolPlum("+ Tambah layanan", { lembar = true; kurang = null }, Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        Kotak(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NomorKue(2, selesai = semester != null)
                Column(Modifier.weight(1f)) {
                    Text("Semester", style = Huruf.judulKartu, color = w.tinta)
                    if (semester != null) {
                        Text("${semester["semester_label"]}.", style = Huruf.meta, color = w.tintaRedup, modifier = Modifier.padding(top = 2.dp))
                    } else {
                        val tebakan = remember { tebakSemester(tanggalWib(Instant.now())) }
                        Text(
                            "Tebakan Gwado: ${tebakan["semester_label"]}, kode ${tebakan["semester_kode"]}, " +
                                "mulai ${tanggalLengkap(LocalDate.parse(tebakan.getValue("semester_mulai")))}. " +
                                "Hanya tugas dari mata kuliah semester ini yang ditampilkan.",
                            style = Huruf.meta,
                            color = w.tintaRedup,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Jarak.s8)) {
                            TombolPlum("Sudah benar", { onKonfirmasi(tebakan); kurang = null })
                            TombolGaris("Ubah", { onBuka(AturSemester) })
                        }
                    }
                }
            }
        }

        TombolPermen("Mulai pakai Gwado", {
            kurang = kurangSiap(adaLayanan = tersambung.isNotEmpty(), adaSemester = semester != null)
            if (kurang == null) {
                mulai = true
                pengambil.ambilSemua()
            }
        }, Modifier.fillMaxWidth())
        kurang?.let { PesanGalat(it) }
        Text(
            "Password tidak pernah disimpan. Data tugas dan token hanya ada di HP ini.",
            style = Huruf.meta,
            color = w.tintaRedup,
        )
    }

    if (lembar) {
        LembarTambah(belum, onTutup = { lembar = false }) { l ->
            lembar = false
            if (l.situs == null) izinGoogle = l else onBuka(Masuk(l.id))
        }
    }
    izinGoogle?.let { l ->
        DialogIzinGoogle(
            onLanjut = {
                izinGoogle = null
                pengambil.tambah(l)
            },
            onTutup = { izinGoogle = null },
        )
    }
}

/** Strip judul: nama aplikasi sebagai teks stiker + satu kalimat. */
@Composable
private fun KepalaSiapkan() {
    val w = GwadoTema.warna
    Kotak(
        Modifier.fillMaxWidth(),
        bentuk = Bentuk.header,
        latar = gradasiCss(115f, w.strip),
        bayanganKedua = 6.dp,
        padding = PaddingValues(Jarak.garis * 2),
    ) {
        Taburan(31, Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)), tinggi = 110f, mulai = 0.4f)
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp)) {
            TeksStiker("Gwado", ukuran = 30.sp)
            Text(
                "Semua tugas kuliahmu di satu tempat",
                style = Huruf.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                color = w.tinta,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Kue bernomor langkah; selesai = gradasi aksen bercentang. */
@Composable
private fun NomorKue(nomor: Int, selesai: Boolean) {
    val w = GwadoTema.warna
    val isi = w.gradasiAksen()
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension / 32f
            val kue = pathGelombang(Offset(15 * s, 15 * s), 11.5f * s, 9, 0.1f)
            translate(2 * s, 2 * s) { drawPath(kue, w.bayangan) }
            if (selesai) drawPath(kue, isi) else drawPath(kue, w.kartu)
            drawPath(kue, w.garis, style = Stroke(2 * s))
            if (selesai) {
                val centang = Path().apply {
                    moveTo(10 * s, 15.5f * s)
                    relativeLineTo(3.2f * s, 3.2f * s)
                    relativeLineTo(6 * s, -7 * s)
                }
                drawPath(centang, Color(0xFF3A1D36), style = Stroke(2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        if (!selesai) {
            Text(
                "$nomor",
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.W700, fontSize = 13.sp),
                color = w.tinta,
                modifier = Modifier.padding(end = 2.dp, bottom = 2.dp),
            )
        }
    }
}

/** Layar tunggu pengambilan pertama. */
@Composable
private fun LayarTunggu() {
    val w = GwadoTema.warna
    Column(
        Modifier.fillMaxSize().latarLayar().safeDrawingPadding().padding(Jarak.tepiLayar),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Mengambil tugas pertamamu", style = Huruf.judulDetail, color = w.tinta, textAlign = TextAlign.Center)
        Text(
            "Ini hanya sekali dan butuh sekitar satu menit. Setelah itu tarik beranda ke bawah untuk mengambil lagi.",
            style = Huruf.meta,
            color = w.tintaRedup,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Pemuat(Modifier.padding(top = 16.dp), ukuran = 44.dp, label = "Mengambil tugas")
    }
}
