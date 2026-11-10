package io.github.quazzong.gwado.ui.layar

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.layanan.prefsGwado
import io.github.quazzong.gwado.tampilan.CONTOH_WARNA
import io.github.quazzong.gwado.tampilan.KELOMPOK_WARNA
import io.github.quazzong.gwado.tampilan.Lch
import io.github.quazzong.gwado.tampilan.ModeWarna
import io.github.quazzong.gwado.tampilan.PilihanWarna
import io.github.quazzong.gwado.tampilan.dariLch
import io.github.quazzong.gwado.tampilan.keLch
import io.github.quazzong.gwado.tampilan.kelompokDari
import io.github.quazzong.gwado.tampilan.simpanPilihanWarna
import io.github.quazzong.gwado.tampilan.ujiKontras
import io.github.quazzong.gwado.ui.komponen.Chip
import io.github.quazzong.gwado.ui.komponen.IsianTeks
import io.github.quazzong.gwado.ui.komponen.JenisChip
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.LembarBawah
import io.github.quazzong.gwado.ui.komponen.Segmen
import io.github.quazzong.gwado.ui.komponen.TombolGaris
import io.github.quazzong.gwado.ui.komponen.TombolPermen
import io.github.quazzong.gwado.ui.komponen.pathGelombang
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.ui.tema.LocalPengaturWarna
import io.github.quazzong.gwado.widget.perbaruiWidget
import java.util.Locale
import kotlinx.coroutines.launch

private const val PEKAT_MAKS = 0.2
private const val TERANG_MIN = 0.4
private const val TERANG_MAKS = 0.97

/**
 * Bagian Warna di Pengaturan tampilan: mode Simpel (contoh + Bebas) atau
 * Lanjutan (tujuh kelompok + kontras). Warna seluruh aplikasi berubah selama digeser; pilihan
 * disimpan dan widget digambar ulang saat geseran dilepas atau pilihan diketuk.
 */
@Composable
fun BagianWarna() {
    val w = GwadoTema.warna
    val pengatur = LocalPengaturWarna.current
    val p = pengatur.pilihan
    val konteks = LocalContext.current
    val cakupan = rememberCoroutineScope()
    val simpan: () -> Unit = {
        prefsGwado(konteks).simpanPilihanWarna(pengatur.pilihan)
        cakupan.launch { perbaruiWidget(konteks) }
    }
    /** Ganti pilihan; [tetap] = langsung disimpan (ketukan), tidak untuk geseran yang masih berjalan. */
    val ganti: (PilihanWarna, Boolean) -> Unit = { baru, tetap ->
        pengatur.pilihan = baru
        if (tetap) simpan()
    }

    Text("WARNA", style = Huruf.label, color = w.tintaRedup, modifier = Modifier.padding(top = Jarak.s8))
    Segmen(listOf(ModeWarna.SIMPEL to "Simpel", ModeWarna.LANJUT to "Lanjutan"), p.mode, { ganti(p.copy(mode = it), true) })
    when (p.mode) {
        ModeWarna.SIMPEL -> BagianSimpel(p, ganti, simpan)
        ModeWarna.LANJUT -> BagianLanjutan(p, ganti, simpan)
    }
}

@Composable
private fun BagianSimpel(p: PilihanWarna, ganti: (PilihanWarna, Boolean) -> Unit, simpan: () -> Unit) {
    val w = GwadoTema.warna
    val pelangi = remember { Brush.sweepGradient(List(13) { Color(dariLch(0.8, 0.12, it * 30.0)) }) }
    Kotak(Modifier.fillMaxWidth(), bayangan = 0.dp, padding = PaddingValues(horizontal = 6.dp, vertical = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val semua = CONTOH_WARNA.map { Triple(it.kode, it.label, SolidColor(Color(it.benih)) as Brush) } +
                Triple(PilihanWarna.KODE_BEBAS, "Bebas", if (p.contoh == PilihanWarna.KODE_BEBAS) SolidColor(Color(p.benih)) else pelangi)
            semua.chunked(5).forEach { baris ->
                Row(Modifier.fillMaxWidth().selectableGroup()) {
                    baris.forEach { (kode, label, isi) ->
                        KueWarna(isi, label, p.contoh == kode, Modifier.weight(1f)) {
                            val baru = if (kode == PilihanWarna.KODE_BEBAS && p.contoh != kode) {
                                p.copy(contoh = kode, bebas = keLch(p.benih))
                            } else {
                                p.copy(contoh = kode)
                            }
                            ganti(baru, true)
                        }
                    }
                    repeat(5 - baris.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (p.contoh == PilihanWarna.KODE_BEBAS) {
                val b = p.bebas
                Column(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GeserWarna(
                        "Rona", "${b.h.toInt()}°", (b.h / 360).toFloat(),
                        List(13) { Color(dariLch(b.l, maxOf(b.c, 0.06), it * 30.0)) },
                        { ganti(p.copy(bebas = b.copy(h = it * 359.0)), false) }, simpan,
                    )
                    GeserWarna(
                        "Kepekatan", "${(b.c / PEKAT_MAKS * 100).toInt()}%", (b.c / PEKAT_MAKS).toFloat(),
                        List(5) { Color(dariLch(b.l, it * PEKAT_MAKS / 4, b.h)) },
                        { ganti(p.copy(bebas = b.copy(c = it * PEKAT_MAKS)), false) }, simpan,
                    )
                    GeserWarna(
                        "Kecerahan", "${(b.l * 100).toInt()}%", ((b.l - TERANG_MIN) / (TERANG_MAKS - TERANG_MIN)).toFloat(),
                        List(5) { Color(dariLch(TERANG_MIN + it * (TERANG_MAKS - TERANG_MIN) / 4, b.c, b.h)) },
                        { ganti(p.copy(bebas = b.copy(l = TERANG_MIN + it * (TERANG_MAKS - TERANG_MIN))), false) }, simpan,
                    )
                }
            }
        }
    }
    Text(
        "Gwado mengatur terang-gelapnya sendiri, jadi tulisan tetap mudah dibaca di tema terang maupun gelap. " +
            "Warna tanda (lewat, mepet, layanan) tidak ikut berubah.",
        style = Huruf.meta,
        color = w.tintaRedup,
    )
}

/** Contoh warna berbentuk kue; terpilih = bayangan lebih tebal + centang. */
@Composable
private fun KueWarna(isi: Brush, label: String, aktif: Boolean, modifier: Modifier = Modifier, onPilih: () -> Unit) {
    val w = GwadoTema.warna
    Column(
        modifier.selectable(selected = aktif, role = Role.RadioButton, onClick = onPilih).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Canvas(Modifier.size(40.dp)) {
            val s = size.minDimension / 42f
            val kue = pathGelombang(Offset(20 * s, 20 * s), 15 * s, 9, 0.1f)
            val geser = (if (aktif) 3 else 2) * s
            translate(geser, geser) { drawPath(kue, w.bayangan) }
            drawPath(kue, isi)
            drawPath(kue, w.garis, style = Stroke(2 * s))
            if (aktif) {
                val centang = Path().apply {
                    moveTo(13.5f * s, 20.5f * s)
                    relativeLineTo(4.5f * s, 4.5f * s)
                    relativeLineTo(8.5f * s, -10 * s)
                }
                drawPath(centang, w.diAtasAksen, style = Stroke(3 * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Text(label, style = Huruf.meta.copy(fontSize = 11.sp), color = if (aktif) w.tinta else w.tintaRedup)
    }
}

/**
 * Slider warna: jalur bergradasi [jalur] bergaris + bayangan keras, kenop putih. [pecahan] 0..1;
 * [onUbah] selama digeser atau diketuk, [onSelesai] saat dilepas.
 */
@Composable
private fun GeserWarna(
    label: String,
    nilaiTeks: String,
    pecahan: Float,
    jalur: List<Color>,
    onUbah: (Float) -> Unit,
    onSelesai: () -> Unit,
) {
    val w = GwadoTema.warna
    val ubah by rememberUpdatedState(onUbah)
    val selesai by rememberUpdatedState(onSelesai)
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label.uppercase(), style = Huruf.label, color = w.tintaRedup, modifier = Modifier.weight(1f))
            Text(nilaiTeks, style = Huruf.meta, color = w.tintaRedup)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = nilaiTeks
                    progressBarRangeInfo = ProgressBarRangeInfo(pecahan, 0f..1f)
                    setProgress { v -> ubah(v.coerceIn(0f, 1f)); selesai(); true }
                }
                .pointerInput(Unit) {
                    detectTapGestures { o -> ubah((o.x / size.width).coerceIn(0f, 1f)); selesai() }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> ubah((o.x / size.width).coerceIn(0f, 1f)) },
                        onDragEnd = { selesai() },
                        onDragCancel = { selesai() },
                    ) { perubahan, _ -> ubah((perubahan.position.x / size.width).coerceIn(0f, 1f)) }
                },
        ) {
            val tinggi = 16.dp.toPx()
            val r = 11.dp.toPx()
            val atas = (size.height - tinggi) / 2
            val ukuranJalur = Size(size.width - 3.dp.toPx(), tinggi)
            val sudut = CornerRadius(tinggi / 2)
            translate(2.dp.toPx(), atas + 2.dp.toPx()) { drawRoundRect(w.bayangan, size = ukuranJalur, cornerRadius = sudut) }
            translate(0f, atas) {
                drawRoundRect(Brush.horizontalGradient(jalur, endX = ukuranJalur.width), size = ukuranJalur, cornerRadius = sudut)
                drawRoundRect(w.garis, size = ukuranJalur, cornerRadius = sudut, style = Stroke(2.dp.toPx()))
            }
            val x = r + pecahan.coerceIn(0f, 1f) * (ukuranJalur.width - 2 * r)
            val y = size.height / 2
            drawCircle(w.bayangan, r, Offset(x + 2.dp.toPx(), y + 2.dp.toPx()))
            drawCircle(w.kartu, r, Offset(x, y))
            drawCircle(w.garis, r, Offset(x, y), style = Stroke(2.dp.toPx()))
        }
    }
}

/** Rasio kontras ala Indonesia: "4,6 : 1". */
private fun teksRasio(r: Double) = String.format(Locale.ROOT, "%.1f", r).replace('.', ',') + " : 1"

@Composable
private fun BagianLanjutan(p: PilihanWarna, ganti: (PilihanWarna, Boolean) -> Unit, simpan: () -> Unit) {
    val w = GwadoTema.warna
    val pengatur = LocalPengaturWarna.current
    var temaGelap by remember { mutableStateOf<Boolean?>(null) }
    val gelap = temaGelap ?: GwadoTema.gelap
    var dibuka by remember { mutableStateOf<String?>(null) }
    var tanyaAturUlang by remember { mutableStateOf(false) }
    val (paletTerang, paletGelap) = pengatur.palet
    val token = if (gelap) paletGelap else paletTerang
    val kelompok = kelompokDari(token)
    val ubahKelompok: (String, Int, Boolean) -> Unit = { k, v, tetap ->
        val baru = if (gelap) p.copy(lanjutGelap = p.lanjutGelap + (k to v)) else p.copy(lanjutTerang = p.lanjutTerang + (k to v))
        ganti(baru, tetap)
    }

    Segmen(listOf(false to "Tema terang", true to "Tema gelap"), gelap, { temaGelap = it })
    Kotak(Modifier.fillMaxWidth(), bayangan = 0.dp, padding = PaddingValues(horizontal = 11.dp, vertical = 4.dp)) {
        Column {
            KELOMPOK_WARNA.forEachIndexed { i, (k, label) ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(Jarak.garis).background(w.garisHalus))
                BarisWarna(Color(kelompok.getValue(k)), label, kelompok.getValue(k)) { dibuka = k }
            }
        }
    }
    val hasil = ujiKontras(token)
    val buruk = hasil.count { !it.lolos }
    Row(Modifier.padding(top = Jarak.s4)) {
        Text("KONTRAS", style = Huruf.label, color = w.tintaRedup)
        if (buruk > 0) Text(" · $buruk SULIT DIBACA", style = Huruf.label, color = w.galat)
    }
    Kotak(Modifier.fillMaxWidth(), bayangan = 0.dp, padding = PaddingValues(horizontal = 11.dp, vertical = 4.dp)) {
        Column {
            hasil.forEachIndexed { i, h ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(Jarak.garis).background(w.garisHalus))
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(h.uji.label, style = Huruf.meta, color = w.tinta, modifier = Modifier.weight(1f))
                    Chip(
                        if (h.lolos) teksRasio(h.rasio) else teksRasio(h.rasio) + " · sulit dibaca",
                        jenis = if (h.lolos) JenisChip.NETRAL else JenisChip.LEWAT,
                    )
                }
            }
        }
    }
    Text(
        "Warna lain (ubin, garis tipis, gradasi strip) dihitung dari tujuh warna ini. Kontras di bawah 4,5 : 1 tetap boleh disimpan.",
        style = Huruf.meta,
        color = w.tintaRedup,
    )
    val namaTema = if (gelap) "tema gelap" else "tema terang"
    if ((if (gelap) p.lanjutGelap else p.lanjutTerang).isNotEmpty()) {
        TombolGaris("Atur ulang $namaTema", { tanyaAturUlang = true }, Modifier.fillMaxWidth())
    }
    TombolGaris("Kembali ke Simpel", { ganti(p.copy(mode = ModeWarna.SIMPEL), true) }, Modifier.fillMaxWidth())

    if (tanyaAturUlang) {
        AlertDialog(
            onDismissRequest = { tanyaAturUlang = false },
            title = { Text("Atur ulang $namaTema?", style = Huruf.judulBagian) },
            text = {
                Text(
                    "Tujuh warna $namaTema kembali ke hasil mode Simpel. Warna yang sudah kamu atur di $namaTema hilang; " +
                        "tema yang satunya tidak berubah.",
                    style = Huruf.isi,
                )
            },
            confirmButton = {
                TextButton(
                    {
                        ganti(if (gelap) p.copy(lanjutGelap = emptyMap()) else p.copy(lanjutTerang = emptyMap()), true)
                        tanyaAturUlang = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = w.galat),
                ) { Text("Atur ulang") }
            },
            dismissButton = {
                TextButton({ tanyaAturUlang = false }, colors = ButtonDefaults.textButtonColors(contentColor = w.tinta)) { Text("Batal") }
            },
        )
    }

    dibuka?.let { k ->
        LembarPemilihWarna(
            judul = KELOMPOK_WARNA.first { it.first == k }.second,
            awal = kelompok.getValue(k),
            onUbah = { ubahKelompok(k, it, false) },
            onTutup = { simpan(); dibuka = null },
        )
    }
}

/** Satu kelompok warna Lanjutan: kotak warna, nama, hex. */
@Composable
private fun BarisWarna(warna: Color, label: String, argb: Int, onKetuk: () -> Unit) {
    val w = GwadoTema.warna
    Row(
        Modifier.fillMaxWidth().selectable(selected = false, role = Role.Button, onClick = onKetuk).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(30.dp)) {
            val b = 2.dp.toPx()
            val kotak = Size(size.width - b, size.height - b)
            val sudut = CornerRadius(9.dp.toPx())
            translate(b, b) { drawRoundRect(w.bayangan, size = kotak, cornerRadius = sudut) }
            drawRoundRect(warna, size = kotak, cornerRadius = sudut)
            drawRoundRect(w.garis, size = kotak, cornerRadius = sudut, style = Stroke(2.dp.toPx()))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = Huruf.judulKartu.copy(fontSize = 13.sp), color = w.tinta, modifier = Modifier.weight(1f))
        Text("#%06X".format(argb and 0xFFFFFF), style = Huruf.meta, color = w.tintaRedup)
    }
}

/** Lembar bawah pemilih warna: kotak kepekatan × kecerahan, slider rona, kolom hex; semuanya live. */
@Composable
private fun LembarPemilihWarna(judul: String, awal: Int, onUbah: (Int) -> Unit, onTutup: () -> Unit) {
    val w = GwadoTema.warna
    var lch by remember { mutableStateOf(keLch(awal)) }
    var hex by remember { mutableStateOf("%06X".format(awal and 0xFFFFFF)) }
    val pasang: (Lch) -> Unit = { baru ->
        lch = baru
        val v = dariLch(baru)
        hex = "%06X".format(v and 0xFFFFFF)
        onUbah(v)
    }
    LembarBawah(onTutup) {
        Column(
            Modifier.padding(horizontal = Jarak.tepiLayar).padding(bottom = Jarak.s16),
            verticalArrangement = Arrangement.spacedBy(Jarak.s12),
        ) {
            Text(judul, style = Huruf.judulBagian, color = w.tinta)
            KotakPekatTerang(lch) { c, l -> pasang(lch.copy(c = c, l = l)) }
            GeserWarna(
                "Rona", "${lch.h.toInt()}°", (lch.h / 360).toFloat(),
                List(13) { Color(dariLch(0.75, 0.12, it * 30.0)) },
                { pasang(lch.copy(h = it * 359.0)) }, {},
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Jarak.s12)) {
                Text("Hex #", style = Huruf.label, color = w.tintaRedup)
                IsianTeks(
                    hex,
                    { teks ->
                        hex = teks.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                        if (hex.length == 6) {
                            val v = (0xFF000000 or hex.toLong(16)).toInt()
                            lch = keLch(v)
                            onUbah(v)
                        }
                    },
                    Modifier.weight(1f),
                    petunjuk = "RRGGBB",
                    satuBaris = true,
                )
            }
            TombolPermen("Selesai", onTutup, Modifier.fillMaxWidth())
        }
    }
}

/** Kotak dua arah: kanan = lebih pekat, atas = lebih terang, pada rona [lch].h. */
@Composable
private fun KotakPekatTerang(lch: Lch, onUbah: (Double, Double) -> Unit) {
    val w = GwadoTema.warna
    val ubah by rememberUpdatedState(onUbah)
    val n = 40
    val gambar = remember(lch.h.toInt()) {
        val px = IntArray(n * n) { i ->
            val x = i % n
            val y = i / n
            dariLch(TERANG_MAKS - y.toDouble() / (n - 1) * (TERANG_MAKS - 0.08), x.toDouble() / (n - 1) * 0.25, lch.h)
        }
        Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    fun keNilai(o: Offset, lebar: Float, tinggi: Float) {
        val fx = (o.x / lebar).coerceIn(0f, 1f)
        val fy = (o.y / tinggi).coerceIn(0f, 1f)
        ubah(fx * 0.25, TERANG_MAKS - fy * (TERANG_MAKS - 0.08))
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics { contentDescription = "Kepekatan dan kecerahan" }
            .pointerInput(Unit) { detectTapGestures { o -> keNilai(o, size.width.toFloat(), size.height.toFloat()) } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { o -> keNilai(o, size.width.toFloat(), size.height.toFloat()) }) { perubahan, _ ->
                    keNilai(perubahan.position, size.width.toFloat(), size.height.toFloat())
                }
            },
    ) {
        val b = 3.dp.toPx()
        val ukuran = IntSize((size.width - b).toInt(), (size.height - b).toInt())
        val sudut = CornerRadius(12.dp.toPx())
        translate(b, b) { drawRoundRect(w.bayangan, size = Size(ukuran.width.toFloat(), ukuran.height.toFloat()), cornerRadius = sudut) }
        val tepi = Path().apply { addRoundRect(RoundRect(0f, 0f, ukuran.width.toFloat(), ukuran.height.toFloat(), sudut)) }
        clipPath(tepi) { drawImage(gambar, dstOffset = IntOffset.Zero, dstSize = ukuran, filterQuality = FilterQuality.Low) }
        drawRoundRect(w.garis, size = Size(ukuran.width.toFloat(), ukuran.height.toFloat()), cornerRadius = sudut, style = Stroke(2.dp.toPx()))
        val x = (lch.c / 0.25).toFloat().coerceIn(0f, 1f) * ukuran.width
        val y = ((TERANG_MAKS - lch.l) / (TERANG_MAKS - 0.08)).toFloat().coerceIn(0f, 1f) * ukuran.height
        drawCircle(w.kartu, 9.dp.toPx(), Offset(x, y))
        drawCircle(w.garis, 9.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
    }
}
