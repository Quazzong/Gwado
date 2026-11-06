package io.github.quazzong.gwado.ui.layar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.kalender.Hari
import io.github.quazzong.gwado.kalender.kejadian
import io.github.quazzong.gwado.kalender.perHari
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.ui.komponen.Chip
import io.github.quazzong.gwado.ui.komponen.JenisChip
import io.github.quazzong.gwado.ui.komponen.KartuTugas
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.StripSub
import io.github.quazzong.gwado.ui.komponen.TombolGarisIkon
import io.github.quazzong.gwado.ui.komponen.gradasiAksen
import io.github.quazzong.gwado.ui.komponen.pathGelombang
import io.github.quazzong.gwado.ui.tema.Fredoka
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.ui.tema.WarnaStatus
import io.github.quazzong.gwado.waktu.Makna
import io.github.quazzong.gwado.waktu.WIB
import io.github.quazzong.gwado.waktu.dariDb
import io.github.quazzong.gwado.waktu.sisaChip
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlin.coroutines.cancellation.CancellationException

private val HARI_SINGKAT = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
private val HARI_NAMA = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
private val BULAN_NAMA = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)

/** Penanda satu deadline di grid. */
private enum class Tanda { LEWAT, MEPET, NANTI, DI_SITUS }

private fun tanda(t: BarisTugas, sekarang: Instant): Tanda = when {
    beresSitus(t) -> Tanda.DI_SITUS
    else -> when (t.deadline?.let { sisaChip(dariDb(it), sekarang).makna }) {
        Makna.LEWAT -> Tanda.LEWAT
        Makna.MEPET -> Tanda.MEPET
        else -> Tanda.NANTI
    }
}

private fun hariPenuh(d: LocalDate) = HARI_NAMA[d.dayOfWeek.value - 1]

private fun keBulanKe(d: LocalDate) = d.year * 12 + d.monthValue - 1

private fun dariBulanKe(n: Int): YearMonth = YearMonth.of(Math.floorDiv(n, 12), Math.floorMod(n, 12) + 1)

/**
 * Kalender deadline satu bulan: grid mulai Senin dengan titik status (maks. 3 per hari),
 * lalu tugas pada hari terpilih. Hari kosong menunjuk ke deadline berikutnya.
 * Geser grid ke kiri/kanan atau ketuk panah untuk pindah bulan.
 */
@Composable
fun LayarKalender(
    daftar: List<BarisTugas>,
    versi: Int,
    sekarang: Instant,
    onBuka: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val hariIni = sekarang.atOffset(WIB).toLocalDate()
    var bulanKe by rememberSaveable { mutableStateOf(keBulanKe(hariIni)) }
    var pilihHari by rememberSaveable { mutableStateOf(hariIni.toEpochDay()) }
    val bulan = dariBulanKe(bulanKe)
    val pilih = LocalDate.ofEpochDay(pilihHari)
    val indeks = remember(daftar) { daftar.associateBy { it.id } }

    val hari by produceState<Map<LocalDate, Hari>>(emptyMap(), versi, bulanKe) {
        val b = dariBulanKe(bulanKe)
        val dari = b.atDay(1)
        val sampai = b.atEndOfMonth()
        value = try {
            Gudang.pakai(konteks) { db -> perHari(db.kejadian(dari, sampai), dari, sampai) }.associateBy { it.tanggal }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun lompat(tanggal: LocalDate) {
        bulanKe = keBulanKe(tanggal)
        pilihHari = tanggal.toEpochDay()
    }

    fun geserBulan(n: Int) {
        val b = dariBulanKe(bulanKe + n)
        lompat(if (YearMonth.from(hariIni) == b) hariIni else b.atDay(1))
    }

    val isiHari = hari[pilih]?.deadline.orEmpty().mapNotNull { indeks[it.kejadian.ref] }
    val aktif = isiHari.count { !beresSitus(it) }
    val berikutnya = daftar.firstOrNull { t ->
        !beresSitus(t) && t.deadline?.let { dariDb(it).atOffset(WIB).toLocalDate() > pilih } == true
    }
    val tanpaDeadline = daftar.count { it.deadline == null }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(Jarak.tepiLayar),
        verticalArrangement = Arrangement.spacedBy(Jarak.s8),
    ) {
        item(key = "strip") {
            StripSub(
                BULAN_NAMA[bulan.monthValue - 1],
                benih = 9,
                ukuran = 28.sp,
                kanan = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Chip("${bulan.year}", jenis = JenisChip.PUTIH)
                        TombolGarisIkon(R.drawable.ikon_kiri, "Bulan sebelumnya", { geserBulan(-1) })
                        TombolGarisIkon(R.drawable.ikon_kanan, "Bulan berikutnya", { geserBulan(1) })
                    }
                },
            )
        }
        item(key = "grid") {
            GridBulan(
                bulan, hariIni, pilih, hari, indeks, sekarang,
                onPilih = { pilihHari = it.toEpochDay() },
                onGeser = { geserBulan(it) },
                modifier = Modifier.padding(top = Jarak.s8, bottom = 6.dp),
            )
        }
        item(key = "judul-hari") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${hariPenuh(pilih)}, ${pilih.dayOfMonth} ${BULAN_NAMA[pilih.monthValue - 1]}",
                    style = Huruf.judulBagian,
                    color = w.tinta,
                )
                if (aktif > 0) Chip("$aktif deadline") else Chip("kosong", jenis = JenisChip.REDUP)
            }
        }
        if (isiHari.isNotEmpty()) {
            items(isiHari, key = { it.id }) { t -> KartuTugas(t, sekarang, onClick = { onBuka(t.id) }) }
        } else {
            item(key = "kosong") { HariKosong(berikutnya) { lompat(it) } }
        }
        if (tanpaDeadline > 0) {
            item(key = "tanpa-deadline") {
                Text(
                    "$tanpaDeadline tugas tanpa deadline ada di tab Tugas",
                    style = Huruf.meta,
                    color = w.tintaRedup,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun GridBulan(
    bulan: YearMonth,
    hariIni: LocalDate,
    pilih: LocalDate,
    hari: Map<LocalDate, Hari>,
    indeks: Map<String, BarisTugas>,
    sekarang: Instant,
    onPilih: (LocalDate) -> Unit,
    onGeser: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val w = GwadoTema.warna
    val ambang = with(LocalDensity.current) { 56.dp.toPx() }
    val geser by rememberUpdatedState(onGeser)
    val awal = bulan.atDay(1)
    val depan = awal.dayOfWeek.value - 1
    val sel = List(depan) { awal.minusDays((depan - it).toLong()) } +
        List(bulan.lengthOfMonth()) { awal.plusDays(it.toLong()) }

    Kotak(
        modifier.fillMaxWidth().pointerInput(Unit) {
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = { if (total <= -ambang) geser(1) else if (total >= ambang) geser(-1) },
            ) { _, jarak -> total += jarak }
        },
        padding = PaddingValues(start = 6.dp, top = 8.dp, end = 6.dp, bottom = 6.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth()) {
                HARI_SINGKAT.forEachIndexed { i, n ->
                    Text(
                        n,
                        style = Huruf.label.copy(letterSpacing = 0.sp),
                        color = if (i == 6) w.minggu else w.tintaRedup,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(top = 2.dp, bottom = 6.dp),
                    )
                }
            }
            sel.chunked(7).forEach { minggu ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    minggu.forEach { d ->
                        val tugas = hari[d]?.deadline.orEmpty().mapNotNull { indeks[it.kejadian.ref] }
                        SelTanggal(
                            d,
                            diBulan = d.month == bulan.month,
                            hariIni = d == hariIni,
                            terpilih = d == pilih,
                            lewat = d < hariIni,
                            tanda = tugas.map { tanda(it, sekarang) },
                            onPilih = { onPilih(d) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(7 - minggu.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Legenda()
        }
    }
}

@Composable
private fun SelTanggal(
    tanggal: LocalDate,
    diBulan: Boolean,
    hariIni: Boolean,
    terpilih: Boolean,
    lewat: Boolean,
    tanda: List<Tanda>,
    onPilih: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val w = GwadoTema.warna
    val isiKue = w.gradasiAksen()
    val warnaAngka = when {
        !diBulan -> w.luarBulan
        terpilih -> Color(0xFF3A1D36)
        tanggal.dayOfWeek == DayOfWeek.SUNDAY -> w.minggu
        lewat -> w.tintaRedup
        else -> w.tinta
    }
    val ket = "${hariPenuh(tanggal)} ${tanggal.dayOfMonth} ${BULAN_NAMA[tanggal.monthValue - 1]}, ${tanda.size} deadline"
    Box(
        modifier
            .height(44.dp)
            .then(
                if (diBulan) {
                    Modifier
                        .selectable(selected = terpilih, interactionSource = null, indication = null, role = Role.Button, onClick = onPilih)
                        .semantics { contentDescription = ket }
                } else {
                    Modifier.clearAndSetSemantics {}
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (diBulan && (terpilih || hariIni)) {
            Canvas(Modifier.requiredSize(42.dp)) {
                val kue = pathGelombang(center, 16.dp.toPx(), 9, 0.1f)
                val g = Jarak.garis.toPx()
                if (terpilih) {
                    translate(g, g) { drawPath(kue, w.bayangan) }
                    drawPath(kue, isiKue)
                    drawPath(kue, w.garis, style = Stroke(g))
                } else {
                    drawPath(kue, w.kartu)
                    drawPath(
                        kue, w.garis,
                        style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.2.dp.toPx(), 2.4.dp.toPx()))),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${tanggal.dayOfMonth}",
                style = TextStyle(
                    fontFamily = Fredoka,
                    fontWeight = if (diBulan && (hariIni || terpilih)) FontWeight.W700 else FontWeight.W600,
                    fontSize = 15.sp,
                    lineHeight = 15.sp,
                ),
                color = warnaAngka,
                modifier = Modifier.clearAndSetSemantics {},
            )
            Row(Modifier.padding(top = 4.dp).height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                tanda.take(3).forEach { Titik(it, 6.dp) }
            }
        }
    }
}

@Composable
private fun Titik(t: Tanda, ukuran: Dp) {
    val w = GwadoTema.warna
    val isi = when (t) {
        Tanda.LEWAT -> WarnaStatus.lewat[1]
        Tanda.MEPET -> WarnaStatus.mepet[1]
        Tanda.NANTI -> w.garis
        Tanda.DI_SITUS -> Color.Transparent
    }
    val tepi = if (t == Tanda.DI_SITUS) w.tintaRedup else w.garis
    Box(Modifier.size(ukuran).background(isi, CircleShape).border(1.5.dp, tepi, CircleShape))
}

@Composable
private fun Legenda() {
    val w = GwadoTema.warna
    Column(Modifier.padding(top = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(Jarak.garis).background(w.garisHalus))
        Row(
            Modifier.fillMaxWidth().padding(start = 2.dp, top = 7.dp, end = 2.dp, bottom = 1.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            listOf(Tanda.LEWAT to "Lewat", Tanda.MEPET to "Mepet", Tanda.NANTI to "Nanti", Tanda.DI_SITUS to "Beres di situs")
                .forEach { (t, nama) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Titik(t, 7.dp)
                        Text(nama, style = Huruf.meta.copy(fontSize = 11.sp), color = w.tintaRedup)
                    }
                }
        }
    }
}

/** Hari tanpa deadline: kotak putus-putus; kalau ada deadline sesudahnya, ketuk untuk lompat ke sana. */
@Composable
private fun HariKosong(berikutnya: BarisTugas?, onLompat: (LocalDate) -> Unit) {
    val w = GwadoTema.warna
    val tanggal = berikutnya?.deadline?.let { dariDb(it).atOffset(WIB).toLocalDate() }
    Kotak(
        Modifier.fillMaxWidth(),
        putus = true,
        kilau = null,
        bayangan = 0.dp,
        onClick = tanggal?.let { d -> { onLompat(d) } },
    ) {
        Column {
            Text("Tidak ada deadline di hari ini", style = Huruf.judulKartu, color = w.tinta)
            if (berikutnya != null && tanggal != null) {
                Text(
                    "Berikutnya ${berikutnya.judul}, ${hariPenuh(tanggal)} ${tanggal.dayOfMonth} ${BULAN_NAMA[tanggal.monthValue - 1].take(3)}",
                    style = Huruf.meta,
                    color = w.tintaRedup,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
