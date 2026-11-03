package io.github.quazzong.gwado.ui.layar

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
import androidx.compose.material3.Text
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

/** Layar detail satu tugas: info, deskripsi, riwayat, centang, dan buka di situs. */
@Composable
fun LayarDetail(id: String, onKembali: () -> Unit, onBerubah: () -> Unit) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val tautan = LocalUriHandler.current
    val cakupan = rememberCoroutineScope()
    val sekarang = rememberSekarang()
    var versi by remember { mutableIntStateOf(0) }
    var sibuk by remember { mutableStateOf(false) }
    var letup by remember { mutableIntStateOf(0) }
    val keadaan by produceState<Keadaan>(Keadaan.Memuat, id, versi) {
        value = try {
            Gudang.pakai(konteks) { db ->
                db.ambilTugas(id)?.let { IsiDetail(it, db.riwayatTampil(id).orEmpty(), db.domainSumber()) }
            }?.let { Keadaan.Ada(it) } ?: Keadaan.TidakAda
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Keadaan.Gagal(e.javaClass.simpleName)
        }
    }

    Column(Modifier.fillMaxSize().background(w.latar).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Jarak.tepiLayar, vertical = Jarak.s8),
            horizontalArrangement = Arrangement.spacedBy(Jarak.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TombolPlumIkon(R.drawable.ikon_kembali, "Kembali", onKembali)
            Text("DETAIL TUGAS", style = Huruf.label, color = w.tintaRedup)
        }
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
                    HeaderDetail(t, sekarang)
                    if (t.statusSitus == "hilang") {
                        Pita("Tugas ini sudah tidak ada di situs. Data terakhir tetap disimpan di HP.")
                    }
                    PanelInfo(t)
                    t.deskripsi?.takeIf { it.isNotBlank() }?.let { Deskripsi(it) }
                    PanelRiwayat(k.isi.riwayat)
                    Spacer(Modifier.height(Jarak.s8))
                }
                Row(
                    Modifier.fillMaxWidth().padding(Jarak.tepiLayar),
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

@Composable
private fun HeaderDetail(t: DetailTugas, sekarang: Instant) {
    val w = GwadoTema.warna
    Kotak(
        Modifier.fillMaxWidth(),
        bentuk = Bentuk.header,
        latar = gradasiCss(115f, w.strip),
        bayanganKedua = 6.dp,
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
                    ChipStatus(t, sekarang)
                    if (t.asal == "manual") Chip("manual")
                }
                Text(t.judul, style = Huruf.judulDetail, color = w.tinta)
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
