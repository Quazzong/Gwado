package io.github.quazzong.gwado.ui.layar

import androidx.compose.runtime.LaunchedEffect
import io.github.quazzong.gwado.ui.komponen.tujuanKepala
import io.github.quazzong.gwado.ui.komponen.judulKepala
import io.github.quazzong.gwado.ui.komponen.LocalTerbang
import io.github.quazzong.gwado.ui.komponen.LetakKepala
import io.github.quazzong.gwado.ui.komponen.Kilau
import io.github.quazzong.gwado.ui.komponen.KartuTugas
import io.github.quazzong.gwado.ui.komponen.KartuTerbang
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.EnterExitState
import io.github.quazzong.gwado.ui.tema.gtween
import io.github.quazzong.gwado.ui.tema.Gerak
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import io.github.quazzong.gwado.ui.komponen.latarDatang
import io.github.quazzong.gwado.ui.komponen.isiLayar
import android.content.Context
import io.github.quazzong.gwado.ui.komponen.latarLayar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import io.github.quazzong.gwado.ui.komponen.asalTombol
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.tugas.BarisRiwayat
import io.github.quazzong.gwado.tugas.DetailTugas
import io.github.quazzong.gwado.tugas.TugasGalat
import io.github.quazzong.gwado.tugas.hapusManual
import io.github.quazzong.gwado.tugas.LABEL_STATUS
import io.github.quazzong.gwado.tugas.ambilTugas
import io.github.quazzong.gwado.tugas.linkSumber
import io.github.quazzong.gwado.tugas.pecahUrl
import io.github.quazzong.gwado.tugas.riwayatTampil
import io.github.quazzong.gwado.tugas.tandai
import io.github.quazzong.gwado.ui.komponen.Chip
import io.github.quazzong.gwado.ui.komponen.IkonLayanan
import io.github.quazzong.gwado.ui.komponen.JenisChip
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.LencanaKue
import io.github.quazzong.gwado.ui.komponen.Letupan
import io.github.quazzong.gwado.ui.komponen.Pita
import io.github.quazzong.gwado.ui.komponen.TombolPermen
import io.github.quazzong.gwado.ui.komponen.Taburan
import io.github.quazzong.gwado.ui.komponen.TombolGarisIkon
import io.github.quazzong.gwado.ui.komponen.TombolPlumIkon
import io.github.quazzong.gwado.ui.komponen.gradasiCss
import io.github.quazzong.gwado.ui.komponen.rememberSekarang
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.waktu.Makna
import io.github.quazzong.gwado.waktu.WIB
import io.github.quazzong.gwado.waktu.dariDb
import io.github.quazzong.gwado.waktu.formatTanggal
import io.github.quazzong.gwado.waktu.sisaChip
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val BULAN3 = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

private val LABEL_KUMPUL = mapOf(
    "belum" to "belum dikumpulkan",
    "draf" to "draf di situs",
    "terkumpul" to "terkumpul di situs",
    "selesai" to "selesai di situs",
)

private class IsiDetail(val tugas: DetailTugas, val riwayat: List<BarisRiwayat>, val domain: List<String>)

private sealed interface Keadaan {
    data object Memuat : Keadaan
    data object TidakAda : Keadaan
    class Gagal(val pesan: String) : Keadaan
    class Ada(val isi: IsiDetail) : Keadaan
}

/** Host dari alamat tiap sumber yang terdaftar, untuk menyaring tombol Buka. */
private fun Db.domainSumber(): List<String> =
    semua("SELECT url FROM sources").mapNotNull { r -> (r["url"] as String?)?.let { pecahUrl(it)?.second } }

/** Isi detail yang dimuat sebelum layar dibuka; diambil (dan dihapus) saat layar pertama tersusun. */
private val PRAMUAT = mutableMapOf<String, Keadaan>()

private suspend fun muatKeadaan(konteks: Context, id: String): Keadaan = try {
    Gudang.pakai(konteks) { db ->
        db.ambilTugas(id)?.let { IsiDetail(it, db.riwayatTampil(id).orEmpty(), db.domainSumber()) }
    }?.let { Keadaan.Ada(it) } ?: Keadaan.TidakAda
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Keadaan.Gagal(e.javaClass.simpleName)
}

/** Muat isi detail sebelum layarnya dibuka, supaya layar tampil lengkap di frame pertama. */
suspend fun praMuatDetail(konteks: Context, id: String) {
    PRAMUAT[id] = muatKeadaan(konteks, id)
}

/** Layar detail satu tugas: info, deskripsi, riwayat, centang, dan buka di situs. */
@Composable
fun LayarDetail(id: String, onKembali: () -> Unit, onBerubah: () -> Unit, onUbah: (String) -> Unit) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val tautan = LocalUriHandler.current
    val cakupan = rememberCoroutineScope()
    val sekarang = rememberSekarang()
    var versi by remember { mutableIntStateOf(0) }
    var sibuk by remember { mutableStateOf(false) }
    var letup by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var konfirmasi by remember { mutableStateOf(false) }
    var galatHapus by remember { mutableStateOf(false) }
    // Isi yang sudah dimuat sebelum layar dibuka (praMuatDetail) langsung tampil di frame pertama,
    // supaya kepala detail sudah ada saat kartu terbang ke tempatnya.
    val awal = remember(id) { PRAMUAT.remove(id) ?: Keadaan.Memuat }
    val keadaan by produceState(awal, id, versi) { value = muatKeadaan(konteks, id) }

    val terbang = LocalTerbang.current
    val dariKartu = remember(id) { terbang?.id == id }
    val lingkup = LocalNavAnimatedContentScope.current
    // Sejak mulai ditutup, latar dan kepala detail tetap tersembunyi sampai layarnya dilepas
    // (layar ditahan sebentar sesudah gerak selesai; tanpa ini detail sempat tampil lagi).
    var tertutup by remember { mutableStateOf(false) }
    LaunchedEffect(lingkup) {
        snapshotFlow { lingkup.transition.targetState }.collect {
            if (it == EnterExitState.PostExit) {
                tertutup = true
                // Dibuka dari kartu: kepala menyusut kembali ke kartu asalnya di daftar.
                if (dariKartu) terbang?.tutup(id)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    // Dibuka dari kartu: latar detail disembunyikan selama kartu terbang (latar daftar di bawahnya
    // sama persis, jadi pergantiannya tak terlihat). Tanpa kartu: latar memudar masuk/keluar.
    val latar = if (dariKartu && terbang != null) {
        Modifier.graphicsLayer { alpha = if (tertutup || (terbang.tahanKepala && terbang.id == id)) 0f else 1f }
    } else {
        Modifier.latarDatang()
    }
    Box(Modifier.matchParentSize().then(latar).latarLayar())
    (keadaan as? Keadaan.Ada)?.let { k ->
        KartuTerbang(
            id,
            wajahKartu = { b -> KartuTugas(b, sekarang, polos = true, sembunyikanJudul = true) },
            wajahKepala = { HeaderDetail(k.isi.tugas, sekarang, polos = true) },
        )
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.isiLayar(0).fillMaxWidth().padding(horizontal = Jarak.tepiLayar, vertical = Jarak.s8),
            horizontalArrangement = Arrangement.spacedBy(Jarak.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TombolPlumIkon(R.drawable.ikon_kembali, "Kembali", onKembali)
            Text("DETAIL TUGAS", style = Huruf.label, color = w.tintaRedup, modifier = Modifier.weight(1f))
            if ((keadaan as? Keadaan.Ada)?.isi?.tugas?.asal == "manual") {
                Box {
                    // Ubah membuka formulir yang melebar dari tombol menu ini (item menunya sudah tertutup).
                    TombolGarisIkon(R.drawable.ikon_menu, "Menu tugas", { menu = true }, Modifier.asalTombol("layar-ubah-$id"))
                    DropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false },
                        shape = Bentuk.kartu,
                        containerColor = w.kartu,
                        border = BorderStroke(Jarak.garis, w.garis),
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ubah", style = Huruf.isi.copy(fontWeight = FontWeight.W800), color = w.tinta) },
                            onClick = { menu = false; onUbah(id) },
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus", style = Huruf.isi.copy(fontWeight = FontWeight.W800), color = w.galat) },
                            onClick = { menu = false; konfirmasi = true },
                        )
                    }
                }
            }
        }
        HapusDialog(
            konfirmasi = konfirmasi,
            galat = galatHapus,
            onHapus = {
                konfirmasi = false
                cakupan.launch {
                    try {
                        Gudang.pakai(konteks) { it.hapusManual(id) }
                        onBerubah()
                        onKembali()
                    } catch (e: TugasGalat) {
                        galatHapus = true
                    }
                }
            },
            onTutup = {
                konfirmasi = false
                galatHapus = false
            },
        )
        when (val k = keadaan) {
            Keadaan.Memuat -> Unit
            Keadaan.TidakAda -> Pesan("Tugas tidak ditemukan", "Mungkin sudah dihapus.")
            is Keadaan.Gagal -> Pesan("Detail tidak bisa dibuka", k.pesan)
            is Keadaan.Ada -> {
                val t = k.isi.tugas
                val link = if (t.asal == "sumber") linkSumber(t.url, k.isi.domain) else null
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Jarak.tepiLayar),
                    verticalArrangement = Arrangement.spacedBy(Jarak.s12),
                ) {
                    Box(Modifier.graphicsLayer { alpha = if (tertutup && dariKartu) 0f else 1f }) { HeaderDetail(t, sekarang) }
                    if (t.statusSitus == "hilang") {
                        Pita("Tugas ini sudah tidak ada di situs.", Modifier.isiLayar(2))
                    }
                    Box(Modifier.isiLayar(2)) { PanelInfo(t) }
                    t.deskripsi?.takeIf { it.isNotBlank() }?.let { Box(Modifier.isiLayar(3)) { Deskripsi(it) } }
                    Box(Modifier.isiLayar(4)) { PanelRiwayat(k.isi.riwayat) }
                    Spacer(Modifier.height(Jarak.s8))
                }
                Row(
                    Modifier.isiLayar(5).fillMaxWidth().padding(Jarak.tepiLayar),
                    horizontalArrangement = Arrangement.spacedBy(Jarak.s8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val selesai = t.status == "done"
                    val ubah: () -> Unit = {
                        if (!sibuk) {
                            sibuk = true
                            cakupan.launch {
                                try {
                                    Gudang.pakai(konteks) { it.tandai(id, !selesai, Instant.now()) }
                                    if (!selesai) letup++
                                    versi++
                                    onBerubah()
                                } finally {
                                    sibuk = false
                                }
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        TombolPermen(
                            if (selesai) "Batalkan selesai" else "Tandai selesai",
                            ubah,
                            Modifier.fillMaxWidth(),
                            lepas = selesai,
                        )
                        Letupan(letup, Modifier.matchParentSize())
                    }
                    if (link != null) {
                        TombolPlumIkon(
                            R.drawable.ikon_buka,
                            "Buka di ${t.sumber ?: "situs"}",
                            { runCatching { tautan.openUri(link) } },
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun HapusDialog(
    konfirmasi: Boolean,
    galat: Boolean,
    onHapus: () -> Unit,
    onTutup: () -> Unit,
) {
    val w = GwadoTema.warna
    val warna = ButtonDefaults.textButtonColors(contentColor = w.tinta)
    if (konfirmasi) {
        AlertDialog(
            onDismissRequest = onTutup,
            title = { Text("Hapus tugas ini?", style = Huruf.judulBagian) },
            text = { Text("Tugas ini dihapus permanen.", style = Huruf.isi) },
            confirmButton = { TextButton(onHapus, colors = ButtonDefaults.textButtonColors(contentColor = w.galat)) { Text("Hapus") } },
            dismissButton = { TextButton(onTutup, colors = warna) { Text("Batal") } },
        )
    }
    if (galat) {
        AlertDialog(
            onDismissRequest = onTutup,
            title = { Text("Belum bisa dihapus", style = Huruf.judulBagian) },
            text = { Text("Ada tugas lain yang ditandai sebagai duplikat tugas ini.", style = Huruf.isi) },
            confirmButton = { TextButton(onTutup, colors = warna) { Text("Oke") } },
        )
    }
}

/**
 * Kepala detail. [polos] = hanya isi (tanpa gradasi, garis, bayangan, judul), dipakai sebagai
 * wajah kepala di kartu terbang; versi asli menjadi tujuan kartu terbang ([tujuanKepala]).
 */
@Composable
private fun HeaderDetail(t: DetailTugas, sekarang: Instant, polos: Boolean = false) {
    val w = GwadoTema.warna
    val letak = remember(t.id) { LetakKepala() }
    Kotak(
        (if (polos) Modifier else Modifier.tujuanKepala(t.id, letak)).fillMaxWidth(),
        bentuk = Bentuk.header,
        latar = if (polos) SolidColor(Color.Transparent) else gradasiCss(115f, w.strip),
        garis = if (polos) Color.Transparent else w.garis,
        kilau = if (polos) null else Kilau.Cincin(w.kilau),
        warnaBayangan = if (polos) Color.Transparent else w.bayangan,
        bayanganKedua = 6.dp,
        warnaBayanganKedua = if (polos) Color.Transparent else w.lavender,
        padding = PaddingValues(Jarak.garis * 2),
    ) {
        Taburan(
            benih = t.id.hashCode(),
            mulai = 0.4f,
            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
        )
        Row(
            Modifier.fillMaxWidth().padding(Jarak.s16 - Jarak.garis * 2),
            horizontalArrangement = Arrangement.spacedBy(Jarak.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Jarak.s8)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Jarak.s4)) {
                    AnimatedContent(
                        targetState = t,
                        contentKey = { listOf(it.status, it.statusKumpulSitus) },
                        transitionSpec = { fadeIn(gtween(Gerak.PUDAR)) togetherWith fadeOut(gtween(Gerak.PUDAR, easing = Gerak.Keluar)) },
                        label = "chip-detail",
                    ) { tt -> ChipStatus(tt, sekarang) }
                    if (t.asal == "manual") Chip("manual")
                }
                Text(
                    t.judul,
                    style = Huruf.judulDetail,
                    color = w.tinta,
                    modifier = if (polos) Modifier.graphicsLayer { alpha = 0f } else Modifier.judulKepala(t.id, letak),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    IkonLayanan(t.sumber)
                    Text(
                        listOfNotNull(t.sumber ?: "Manual", t.kodeMk ?: t.kodeMkManual).joinToString(" · "),
                        style = Huruf.meta,
                        color = w.tinta,
                    )
                }
            }
            val dl = t.deadline?.let { dariDb(it).atOffset(WIB) }
            if (dl != null) LencanaKue("${dl.dayOfMonth}", BULAN3[dl.monthValue - 1])
            else LencanaKue("?", null, putus = true)
        }
    }
}

@Composable
private fun ChipStatus(t: DetailTugas, sekarang: Instant) {
    val c = t.deadline?.let { sisaChip(dariDb(it), sekarang) }
    when {
        t.status == "done" -> Chip("selesai", jenis = JenisChip.AKTIF)
        t.statusKumpulSitus == "terkumpul" -> Chip("terkumpul di situs")
        t.statusKumpulSitus == "selesai" -> Chip("selesai di situs")
        c == null -> Chip("tanpa deadline", jenis = JenisChip.PUTUS)
        else -> Chip(
            c.label,
            jenis = when (c.makna) {
                Makna.LEWAT -> JenisChip.LEWAT
                Makna.MEPET -> JenisChip.MEPET
                Makna.NORMAL -> JenisChip.NETRAL
            },
        )
    }
}

@Composable
private fun PanelInfo(t: DetailTugas) {
    Kotak(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Jarak.s8)) {
            BarisInfo("Mata kuliah", t.matkul ?: "—")
            BarisInfo("Deadline", t.deadline?.let { formatTanggal(dariDb(it)) + " WIB" } ?: "Tanpa deadline")
            BarisInfo("Status", LABEL_STATUS[t.status] ?: t.status)
            if (t.asal == "sumber") {
                val kumpul = LABEL_KUMPUL[t.statusKumpulSitus]
                BarisInfoIsi("Di situs") {
                    if (kumpul == null) {
                        Text("tidak diketahui", style = Huruf.isi, color = GwadoTema.warna.tintaRedup)
                    } else {
                        Chip(
                            kumpul,
                            jenis = when {
                                t.status == "done" -> JenisChip.REDUP
                                t.statusKumpulSitus == "draf" -> JenisChip.MEPET
                                else -> JenisChip.NETRAL
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisInfo(label: String, nilai: String) {
    val w = GwadoTema.warna
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Jarak.s12)) {
        Text(label.uppercase(), style = Huruf.label, color = w.tintaRedup, modifier = Modifier.width(96.dp).alignByBaseline())
        Text(nilai, style = Huruf.isi, color = w.tinta, modifier = Modifier.weight(1f).alignByBaseline())
    }
}

/** Baris info dengan isi bebas (misalnya chip) di kolom nilai. */
@Composable
private fun BarisInfoIsi(label: String, isi: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Jarak.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), style = Huruf.label, color = GwadoTema.warna.tintaRedup, modifier = Modifier.width(96.dp))
        isi()
    }
}

@Composable
private fun Deskripsi(teks: String) {
    val w = GwadoTema.warna
    var buka by rememberSaveable { mutableStateOf(false) }
    var terpotong by remember { mutableStateOf(false) }
    Kotak(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Jarak.s8)) {
            Text("DESKRIPSI", style = Huruf.label, color = w.tintaRedup)
            Text(
                teks,
                style = Huruf.isi,
                color = w.tinta,
                maxLines = if (buka) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!buka) terpotong = it.hasVisualOverflow },
            )
            if (terpotong || buka) {
                Text(
                    if (buka) "Ringkas" else "Selengkapnya",
                    style = Huruf.meta.copy(fontWeight = FontWeight.W800),
                    color = w.tinta,
                    modifier = Modifier.minimumInteractiveComponentSize().clickable { buka = !buka },
                )
            }
        }
    }
}

@Composable
private fun PanelRiwayat(riwayat: List<BarisRiwayat>) {
    val w = GwadoTema.warna
    Kotak(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Jarak.s12)) {
            Text("RIWAYAT", style = Huruf.label, color = w.tintaRedup)
            if (riwayat.isEmpty()) Text("Belum ada perubahan.", style = Huruf.isi, color = w.tintaRedup)
            riwayat.forEach { r ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${r.label} · ${r.oleh}", style = Huruf.meta.copy(fontWeight = FontWeight.W800), color = w.tinta)
                    if (r.dari != null) {
                        Text(
                            "${r.dari} → ${r.ke}",
                            style = Huruf.meta,
                            color = w.tinta,
                            modifier = if (r.kuning) {
                                Modifier.background(w.latarMepet, Bentuk.chip).padding(horizontal = 6.dp, vertical = 2.dp)
                            } else {
                                Modifier
                            },
                        )
                    }
                    Text(r.waktu, style = Huruf.meta, color = w.tintaRedup)
                }
            }
        }
    }
}

@Composable
private fun Pesan(judul: String, kalimat: String) {
    val w = GwadoTema.warna
    Column(
        Modifier.fillMaxSize().padding(Jarak.tepiLayar),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(judul, style = Huruf.judulBagian, color = w.tinta)
        Text(kalimat, style = Huruf.isi, color = w.tintaRedup, textAlign = TextAlign.Center)
    }
}
