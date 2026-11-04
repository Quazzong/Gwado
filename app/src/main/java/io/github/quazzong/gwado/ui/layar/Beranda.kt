package io.github.quazzong.gwado.ui.layar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import io.github.quazzong.gwado.layanan.HasilAmbil
import io.github.quazzong.gwado.segar.SumberTerakhir
import io.github.quazzong.gwado.segar.Tingkat
import io.github.quazzong.gwado.segar.banner
import io.github.quazzong.gwado.segar.statusSumber
import io.github.quazzong.gwado.segar.sumberTerakhir
import io.github.quazzong.gwado.ui.komponen.KueTarik
import io.github.quazzong.gwado.ui.komponen.Pita
import io.github.quazzong.gwado.ui.komponen.TombolPermen
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.beranda.kuliahMulai
import io.github.quazzong.gwado.beranda.progresMinggu
import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.segar.jamSingkat
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.ui.komponen.KartuTugas
import io.github.quazzong.gwado.ui.komponen.Segmen
import io.github.quazzong.gwado.ui.komponen.TombolGaris
import io.github.quazzong.gwado.ui.komponen.TombolPlumIkon
import io.github.quazzong.gwado.ui.komponen.Ubin
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.waktu.Makna
import io.github.quazzong.gwado.waktu.WIB
import io.github.quazzong.gwado.waktu.dariDb
import io.github.quazzong.gwado.waktu.sisaChip
import java.time.Instant
import java.time.LocalDate

/** Tugas belum dicentang yang situsnya mencatat sudah dikumpulkan. */
internal fun terkumpul(t: BarisTugas): Boolean =
    t.status != "done" && t.statusKumpulSitus == "terkumpul"

/**
 * Tugas belum dicentang dan belum terkumpul di situs yang sudah lewat,
 * hari ini, besok, atau kurang dari 1 jam lagi.
 */
internal fun mendesak(t: BarisTugas, sekarang: Instant): Boolean =
    t.status != "done" && !terkumpul(t) &&
        (t.deadline?.let { sisaChip(dariDb(it), sekarang).makna != Makna.NORMAL } ?: false)

private class DataKepala(
    val kuliahMulai: LocalDate?,
    val semua: List<BarisTugas>,
    val diambil: Instant?,
    val sumber: List<SumberTerakhir>,
)

/** Selesai run sukses terakhir dari sumber mana pun. */
private fun Db.suksesTerakhir(): Instant? =
    (satu("SELECT max(selesai) AS s FROM source_runs WHERE status = 'success'")?.get("s") as String?)?.let(::dariDb)

/** Nama layanan di filter; nama panjang disingkat jadi huruf depan tiap kata (Google Classroom → GC). */
private fun singkat(nama: String): String =
    if (nama.length > 10 && ' ' in nama) nama.split(' ').filter { it.isNotEmpty() }.joinToString("") { it.take(1) } else nama

/**
 * Beranda: kepala (minggu, hari, progres), ubin ringkasan, filter layanan, lalu tugas
 * dalam bagian MENDESAK, NANTI (tanpa deadline di akhirnya), dan TERKUMPUL DI SITUS.
 * [versi] naik setiap kali data di database berubah. Ketuk kartu = buka detail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Beranda(
    daftar: List<BarisTugas>,
    versi: Int,
    sekarang: Instant,
    onBuka: (String) -> Unit,
    onTambah: () -> Unit,
    menyegarkan: Boolean = false,
    onSegarkan: () -> Unit = {},
    hasilAmbil: HasilAmbil? = null,
    onBukaLayanan: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val data by produceState<DataKepala?>(null, versi) {
        value = runCatching {
            Gudang.pakai(konteks) { db -> DataKepala(db.kuliahMulai(), db.daftarTugas(semua = true), db.suksesTerakhir(), db.sumberTerakhir()) }
        }.getOrNull()
    }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val layanan = daftar.map { it.sumber ?: "Manual" }.distinct().sortedBy { it == "Manual" }
    val aktif = filter?.takeIf { it in layanan }
    val tampil = if (aktif == null) daftar else daftar.filter { (it.sumber ?: "Manual") == aktif }
    val (sudah, belum) = tampil.partition(::terkumpul)
    val (atas, bawah) = belum.partition { mendesak(it, sekarang) }
    val stSumber = data?.sumber?.filter { it.status != null }?.let { statusSumber(it, sekarang) }.orEmpty()
    val pitaSumber = banner(stSumber)
    var dilihat by rememberSaveable { mutableIntStateOf(0) }
    val hasilBaru = hasilAmbil?.takeIf { it.nomor > dilihat && it.pesan.isNotEmpty() }
    LaunchedEffect(hasilBaru?.nomor) {
        if (hasilBaru != null) {
            delay(6000)
            dilihat = hasilBaru.nomor
        }
    }
    val tarik = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = menyegarkan,
        onRefresh = onSegarkan,
        modifier = modifier.fillMaxSize(),
        state = tarik,
        indicator = { KueTarik(tarik, menyegarkan, Modifier.align(Alignment.TopCenter)) },
    ) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Jarak.tepiLayar, top = Jarak.tepiLayar, end = Jarak.tepiLayar, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(Jarak.s8),
    ) {
        item(key = "kepala") {
            KepalaBeranda(
                sekarang,
                data?.kuliahMulai,
                progresMinggu(data?.semua.orEmpty(), sekarang.atOffset(WIB).toLocalDate()),
            )
        }
        if (pitaSumber != null) {
            item(key = "pita-sumber") {
                val masalah = stSumber.filter { it.tingkat != Tingkat.NORMAL }
                Pita(
                    masalah.joinToString(", ", postfix = ". Ketuk untuk membuka Layanan.") { "${it.nama} ${chipStatus(it).first}" },
                    merah = pitaSumber.tingkat == Tingkat.MERAH,
                    onClick = onBukaLayanan,
                )
            }
        }
        if (hasilBaru != null) {
            item(key = "pita-ambil") { Pita(hasilBaru.pesan.joinToString("\n")) }
        }
        item(key = "ubin") {
            Row(Modifier.fillMaxWidth().padding(top = Jarak.s4), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Ubin("Aktif", "${daftar.size}", Modifier.weight(1f))
                Ubin("Mendesak", "${daftar.count { mendesak(it, sekarang) }}", Modifier.weight(1f))
                Ubin("Diambil", data?.diambil?.let { jamSingkat(it, sekarang) } ?: "—", Modifier.weight(1f))
            }
        }
        if (layanan.size > 1) {
            item(key = "filter") {
                Segmen(
                    listOf<Pair<String?, String>>(null to "Semua") + layanan.map { it to singkat(it) },
                    aktif = aktif,
                    onPilih = { filter = it },
                    modifier = Modifier.padding(vertical = Jarak.s4),
                )
            }
        }
        if (daftar.isEmpty()) {
            item(key = "kosong") {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = Jarak.s24),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Belum ada tugas", style = Huruf.judulBagian, color = w.tinta)
                    Text(
                        "Tarik ke bawah, atau tekan Ambil sekarang, untuk mengambil tugas dari situs kuliah.",
                        style = Huruf.isi,
                        color = w.tintaRedup,
                        textAlign = TextAlign.Center,
                    )
                    TombolPermen(if (menyegarkan) "Mengambil…" else "Ambil sekarang", onSegarkan, Modifier.padding(top = Jarak.s12))
                    TombolGaris("Tugas manual", onTambah, Modifier.padding(top = Jarak.s8))
                }
            }
        }
        bagian("Mendesak", atas, sekarang, onBuka)
        bagian("Nanti", bawah, sekarang, onBuka)
        bagian("Terkumpul di situs", sudah, sekarang, onBuka)
    }
    TombolPlumIkon(
        R.drawable.ikon_tambah,
        "Tambah tugas manual",
        onTambah,
        Modifier.align(Alignment.BottomEnd).padding(Jarak.tepiLayar),
    )
    }
}

private fun LazyListScope.bagian(judul: String, isi: List<BarisTugas>, sekarang: Instant, onBuka: (String) -> Unit) {
    if (isi.isEmpty()) return
    item(key = "bagian-$judul") {
        Text(
            judul.uppercase(),
            style = Huruf.label,
            color = GwadoTema.warna.tintaRedup,
            modifier = Modifier.padding(top = Jarak.s8),
        )
    }
    items(isi, key = { it.id }) { t -> KartuTugas(t, sekarang, onClick = { onBuka(t.id) }) }
}
