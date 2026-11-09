package io.github.quazzong.gwado.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.ColorProvider as GlanceColor
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.quazzong.gwado.MainActivity
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.beranda.progresMinggu
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.layanan.prefsGwado
import io.github.quazzong.gwado.tampilan.Tema
import io.github.quazzong.gwado.tampilan.tema
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.ui.komponen.Ekspresi
import io.github.quazzong.gwado.ui.layar.suksesTerakhir
import io.github.quazzong.gwado.waktu.Chip
import io.github.quazzong.gwado.waktu.Makna
import io.github.quazzong.gwado.waktu.WIB
import java.time.Instant
import kotlinx.coroutines.CancellationException

/** Extra intent dari widget: ID tugas yang detailnya dibuka. */
const val EKSTRA_TUGAS = "io.github.quazzong.gwado.TUGAS"

/** Naik setiap kali data berubah, supaya widget yang sedang hidup membaca ulang database. */
private val VERSI = intPreferencesKey("versi")

/** Warna widget yang berbeda antara tema terang dan gelap. */
private class Palet(val tinta: Long, val redup: Long, val garisHalus: Long, val ubin: Long, val merah: Long)

private val PALET_TERANG = Palet(0xFF3A1D36, 0xFF7A5872, 0xFFF1E4EE, 0xFFF1ECFA, 0xFFC8303B)
private val PALET_GELAP = Palet(0xFFF6E9F2, 0xFFC4ABBF, 0xFF3A2842, 0xFF33233B, 0xFFFF8A8F)

private val AKSEN = ColorProvider(Color(0xFFF7A8D8))
private val DI_ATAS_LEWAT = ColorProvider(Color(0xFF3A0F18))
private val DI_ATAS_MEPET = ColorProvider(Color(0xFF3A2A00))

/**
 * Warna, gaya teks, dan gambar widget untuk satu pilihan tema. [Tema.SISTEM] memakai warna
 * siang/malam dan gambar tanpa akhiran (dipilih launcher menurut mode gelap sistem); Terang dan
 * Gelap memakai warna dan gambar `_terang`/`_gelap` yang tetap, apa pun mode sistemnya.
 */
private class GayaWidget(tema: Tema) {
    private val gelap: Boolean? = when (tema) {
        Tema.SISTEM -> null
        Tema.TERANG -> false
        Tema.GELAP -> true
    }

    private fun warna(pilih: (Palet) -> Long): GlanceColor = when (gelap) {
        null -> ColorProvider(day = Color(pilih(PALET_TERANG)), night = Color(pilih(PALET_GELAP)))
        false -> ColorProvider(Color(pilih(PALET_TERANG)))
        true -> ColorProvider(Color(pilih(PALET_GELAP)))
    }

    private fun gambar(sistem: Int, terang: Int, gelap: Int) = when (this.gelap) {
        null -> sistem
        false -> terang
        true -> gelap
    }

    val tinta = warna { it.tinta }
    val redup = warna { it.redup }
    val garisHalus = warna { it.garisHalus }
    val ubin = warna { it.ubin }
    val merah = warna { it.merah }

    val label = TextStyle(color = redup, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val judul = TextStyle(color = tinta, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val kecil = TextStyle(color = redup, fontSize = 11.sp, fontWeight = FontWeight.Bold)

    val kartu = gambar(R.drawable.widget_kartu, R.drawable.widget_kartu_terang, R.drawable.widget_kartu_gelap)
    val chipLewat = gambar(R.drawable.widget_chip_lewat, R.drawable.widget_chip_lewat_terang, R.drawable.widget_chip_lewat_gelap)
    val chipMepet = gambar(R.drawable.widget_chip_mepet, R.drawable.widget_chip_mepet_terang, R.drawable.widget_chip_mepet_gelap)
    val chipNetral = gambar(R.drawable.widget_chip_netral, R.drawable.widget_chip_netral_terang, R.drawable.widget_chip_netral_gelap)
    val chipPutus = gambar(R.drawable.widget_chip_putus, R.drawable.widget_chip_putus_terang, R.drawable.widget_chip_putus_gelap)

    fun stiker(e: Ekspresi) = gambar(e.widget, e.gambar, e.gambarGelap)
}

private val LocalGaya = staticCompositionLocalOf { GayaWidget(Tema.SISTEM) }

/** Tugas terbanyak yang dibaca untuk widget Terdekat. */
private const val MAKS_BARIS = 12

/** Ambang ukuran laporan launcher untuk tata letak widget Tugas. */
private val TINGGI_ANGKA_BESAR = 150.dp
private val LEBAR_SATU_KOTAK = 120.dp
private val LEBAR_DUA_KOTAK = 220.dp

/** Baca isi widget dari database; null kalau database tidak bisa dibuka. */
private suspend fun muatIsi(context: Context): IsiWidget? = try {
    val sekarang = Instant.now()
    Gudang.pakai(context) { db ->
        val progres = progresMinggu(db.daftarTugas(semua = true), sekarang.atOffset(WIB).toLocalDate())
        isiWidget(db.daftarTugas(), sekarang, db.suksesTerakhir(), maks = MAKS_BARIS, progres = progres)
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

/** Gambar ulang semua widget Gwado yang terpasang. Gagal = diam, widget tetap menampilkan isi lama. */
suspend fun perbaruiWidget(context: Context) {
    try {
        val manajer = GlanceAppWidgetManager(context)
        for (widget in listOf(WidgetTugas(), WidgetTerdekat())) {
            for (id in manajer.getGlanceIds(widget.javaClass)) {
                updateAppWidgetState(context, id) { it[VERSI] = (it[VERSI] ?: 0) + 1 }
                widget.update(context, id)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // widget tidak wajib; isi lama tetap tampil
    }
}

/** Buka Gwado; dengan [id], langsung ke detail tugas itu. */
private fun bukaGwado(context: Context, id: String? = null): Action =
    actionStartActivity(Intent(context, MainActivity::class.java).apply { if (id != null) putExtra(EKSTRA_TUGAS, id) })

/** Widget membaca database sekali saat mulai, lalu lagi setiap kali [perbaruiWidget] menaikkan versinya. */
abstract class WidgetGwado : GlanceAppWidget() {
    /** Ukuran sebenarnya dikirim ke komposisi ([LocalSize]), supaya isi mengikuti ukuran widget. */
    override val sizeMode: SizeMode = SizeMode.Exact

    @Composable
    internal abstract fun Isi(context: Context, isi: IsiWidget?)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val awal = muatIsi(context)
        val prefs = prefsGwado(context)
        provideContent {
            val versi = currentState(VERSI) ?: 0
            val isi by produceState(awal, versi) { value = muatIsi(context) }
            // Tema dibaca ulang setiap versi naik: mengganti tema di Gwado ikut memperbarui widget.
            val gaya = remember(versi) { GayaWidget(prefs.tema()) }
            CompositionLocalProvider(LocalGaya provides gaya) { Isi(context, isi) }
        }
    }
}

/** Jumlah tugas yang masih harus dikerjakan + tugas terdekat. */
class WidgetTugas : WidgetGwado() {
    @Composable
    override fun Isi(context: Context, isi: IsiWidget?) = IsiTugas(context, isi)
}

/** Tugas terdekat + jam pengambilan terakhir (+ progres minggu ini kalau ada ruang). */
class WidgetTerdekat : WidgetGwado() {
    @Composable
    override fun Isi(context: Context, isi: IsiWidget?) = IsiTerdekat(context, isi)
}

class PenerimaTugas : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetTugas()
}

class PenerimaTerdekat : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetTerdekat()
}

/** Kartu widget: garis + bayangan keras dari drawable (sudut bulat juga di Android lama). */
@Composable
private fun Kartu(isi: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(ImageProvider(LocalGaya.current.kartu))
            .padding(start = 12.dp, top = 10.dp, end = 15.dp, bottom = 13.dp),
        content = isi,
    )
}

@Composable
private fun ChipWidget(chip: Chip?) {
    val g = LocalGaya.current
    val (latar, warnaTeks) = when (chip?.makna) {
        null -> g.chipPutus to g.redup
        Makna.LEWAT -> g.chipLewat to DI_ATAS_LEWAT
        Makna.MEPET -> g.chipMepet to DI_ATAS_MEPET
        Makna.NORMAL -> g.chipNetral to g.tinta
    }
    Box(GlanceModifier.background(ImageProvider(latar)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(
            (chip?.label ?: "tanpa deadline").uppercase(),
            style = TextStyle(color = warnaTeks, fontSize = 11.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

/**
 * Stiker maskot untuk ruang kosong di widget: netral kalau ada tugas, santai kalau belum pernah
 * diambil, bersorak kalau semua beres, bingung kalau database tidak terbaca.
 */
@Composable
private fun StikerWidget(isi: IsiWidget?) {
    val ekspresi = when {
        isi == null -> Ekspresi.BINGUNG
        isi.terdekat.isNotEmpty() -> Ekspresi.NETRAL
        isi.diambil == null -> Ekspresi.SANTAI
        else -> Ekspresi.BERSORAK
    }
    Image(ImageProvider(LocalGaya.current.stiker(ekspresi)), contentDescription = null, modifier = GlanceModifier.size(56.dp))
}

@Composable
private fun TeksKosong(teks: String) {
    Text(teks, style = TextStyle(color = LocalGaya.current.redup, fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 2)
}

/**
 * Label widget Tugas: "TUGAS", ditambah jumlah yang mendesak (merah) kalau ada.
 * [duaBaris] untuk widget sempit: keterangan mendesak di baris kedua.
 */
@Composable
private fun LabelTugas(isi: IsiWidget?, duaBaris: Boolean, modifier: GlanceModifier = GlanceModifier) {
    val g = LocalGaya.current
    val mendesak = isi?.mendesak ?: 0
    val gayaMerah = g.label.copy(color = g.merah)
    if (duaBaris) {
        Column(modifier) {
            Text("TUGAS", style = g.label)
            if (mendesak > 0) Text("$mendesak MENDESAK", style = gayaMerah, maxLines = 1)
        }
    } else {
        Row(modifier) {
            Text("TUGAS", style = g.label)
            if (mendesak > 0) Text(" · $mendesak MENDESAK", style = gayaMerah, maxLines = 1)
        }
    }
}

/** Judul + chip tugas pertama; ketuk = buka detailnya. */
@Composable
private fun TugasPertama(context: Context, isi: IsiWidget?, barisJudul: Int) {
    val pertama = isi?.terdekat?.firstOrNull()
    val g = LocalGaya.current
    when {
        isi == null -> TeksKosong("Buka Gwado")
        pertama == null -> TeksKosong(pesanKosong(isi, singkat = true))
        else -> Column(GlanceModifier.fillMaxWidth().clickable(bukaGwado(context, pertama.id))) {
            Text(pertama.judul, style = g.judul.copy(fontSize = 12.sp), maxLines = barisJudul)
            Spacer(GlanceModifier.height(4.dp))
            ChipWidget(pertama.chip)
        }
    }
}

/**
 * Angka = tugas yang masih harus dikerjakan (sama dengan ubin Aktif di beranda); seberapa
 * mendesak terlihat dari label dan chip tugas terdekat. Tata letak menurut ukuran: 1×1 = angka
 * di atas chip; tinggi = angka besar di atas; 2×1 = label dan angka satu baris, tugas di bawahnya;
 * lebih lebar = angka besar di kiri, chip di kanan, judul + keterangan selebar widget.
 */
@Composable
private fun IsiTugas(context: Context, isi: IsiWidget?) {
    val g = LocalGaya.current
    val beranda = bukaGwado(context)
    val angka = isi?.jumlah?.toString() ?: "–"
    val gayaAngka = TextStyle(color = g.tinta, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    val ukuran = LocalSize.current
    Kartu {
        when {
            ukuran.width < LEBAR_SATU_KOTAK -> Column(
                GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val pertama = isi?.terdekat?.firstOrNull()
                val besar = ukuranAngkaSempit(
                    ukuran.width.value, ukuran.height.value, angka.length, adaChip = pertama != null,
                    skalaHuruf = context.resources.configuration.fontScale,
                )
                Text(angka, style = gayaAngka.copy(fontSize = besar.sp), maxLines = 1, modifier = GlanceModifier.clickable(beranda))
                if (pertama != null) {
                    Spacer(GlanceModifier.height(4.dp))
                    Box(GlanceModifier.clickable(bukaGwado(context, pertama.id))) { ChipWidget(pertama.chip) }
                }
            }
            ukuran.height >= TINGGI_ANGKA_BESAR -> {
                Box(GlanceModifier.fillMaxWidth().defaultWeight().clickable(beranda), contentAlignment = Alignment.BottomEnd) {
                    Column(GlanceModifier.fillMaxSize()) {
                        LabelTugas(isi, duaBaris = false)
                        Text(angka, style = gayaAngka)
                    }
                    StikerWidget(isi)
                }
                TugasPertama(context, isi, barisJudul = 2)
            }
            ukuran.width < LEBAR_DUA_KOTAK -> {
                Row(
                    GlanceModifier.fillMaxWidth().defaultWeight().clickable(beranda),
                    verticalAlignment = Alignment.Top,
                ) {
                    LabelTugas(isi, duaBaris = true, modifier = GlanceModifier.defaultWeight())
                    Text(angka, style = gayaAngka.copy(fontSize = 46.sp))
                }
                TugasPertama(context, isi, barisJudul = 1)
            }
            else -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text(angka, style = gayaAngka.copy(fontSize = 52.sp), modifier = GlanceModifier.clickable(beranda))
                Spacer(GlanceModifier.width(12.dp))
                // Lebar: chip di ujung kanan baris label, lalu judul + keterangan selebar widget.
                val pertama = isi?.terdekat?.firstOrNull()
                Column(GlanceModifier.defaultWeight()) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        LabelTugas(isi, duaBaris = false, modifier = GlanceModifier.defaultWeight().clickable(beranda))
                        if (pertama != null) ChipWidget(pertama.chip)
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    when {
                        isi == null -> TeksKosong("Buka Gwado")
                        pertama == null -> TeksKosong(pesanKosong(isi, singkat = false))
                        else -> Column(GlanceModifier.fillMaxWidth().clickable(bukaGwado(context, pertama.id))) {
                            Text(pertama.judul, style = g.judul, maxLines = 2)
                            Text(pertama.keterangan, style = g.kecil, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Baris tugas sebanyak yang muat (tanpa baris terpotong), sisanya disebut "+N lagi" di kepala.
 * Kalau semua tugas muat dan masih ada ruang: baris rinci (mata kuliah + tanggal), lalu progres
 * minggu ini di bawah, lalu stiker di ruang yang masih kosong.
 */
@Composable
private fun IsiTerdekat(context: Context, isi: IsiWidget?) {
    val g = LocalGaya.current
    val beranda = bukaGwado(context)
    val tata = tataTerdekat(LocalSize.current.height.value, isi?.terdekat?.size ?: 0, isi?.progres != null)
    val tampil = isi?.terdekat.orEmpty().take(tata.baris)
    val lagi = (isi?.jumlah ?: 0) - tampil.size
    Kartu {
        Row(
            GlanceModifier.fillMaxWidth().padding(bottom = 4.dp).clickable(beranda),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("TERDEKAT", style = g.label)
            if (lagi > 0) Text("  · +$lagi LAGI", style = g.label, maxLines = 1)
            Spacer(GlanceModifier.defaultWeight())
            isi?.diambil?.let { Text("diambil $it", style = g.label, maxLines = 1) }
        }
        when {
            isi == null -> TeksKosong("Buka Gwado")
            tampil.isEmpty() -> TeksKosong(pesanKosong(isi, singkat = false))
            else -> tampil.forEachIndexed { i, b ->
                if (i > 0) Spacer(GlanceModifier.fillMaxWidth().height(2.dp).background(g.garisHalus))
                Row(
                    GlanceModifier.fillMaxWidth().padding(vertical = if (tata.rinci) 5.dp else 3.dp)
                        .clickable(bukaGwado(context, b.id)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChipWidget(b.chip)
                    Spacer(GlanceModifier.width(8.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(b.judul, style = g.judul, maxLines = 1)
                        if (tata.rinci) Text(b.keterangan, style = g.kecil, maxLines = 1)
                    }
                }
            }
        }
        if (tata.stiker) {
            Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) { StikerWidget(isi) }
        }
        val progres = isi?.progres
        if (tata.progres && progres != null) {
            if (!tata.stiker) Spacer(GlanceModifier.defaultWeight())
            Column(GlanceModifier.fillMaxWidth().clickable(beranda)) {
                Text("MINGGU INI · ${progres.selesai} DARI ${progres.total} SELESAI", style = g.label, maxLines = 1)
                Spacer(GlanceModifier.height(5.dp))
                LinearProgressIndicator(
                    progress = progres.selesai.toFloat() / progres.total,
                    modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                    color = AKSEN,
                    backgroundColor = g.ubin,
                )
            }
        }
    }
}
