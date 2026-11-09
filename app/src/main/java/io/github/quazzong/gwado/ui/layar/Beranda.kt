package io.github.quazzong.gwado.ui.layar

import io.github.quazzong.gwado.ui.komponen.asalTombol
import io.github.quazzong.gwado.ui.komponen.ikutLayar
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.size
import io.github.quazzong.gwado.ui.komponen.Letupan
import io.github.quazzong.gwado.ui.komponen.munculKotak
import io.github.quazzong.gwado.ui.tema.Gerak
import androidx.compose.runtime.mutableIntStateOf
import io.github.quazzong.gwado.layanan.HasilAmbil
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.fetch.SebabTertahan
import io.github.quazzong.gwado.layanan.DAFTAR_LAYANAN
import io.github.quazzong.gwado.segar.StatusSumber
import io.github.quazzong.gwado.ui.komponen.itemsTab
import io.github.quazzong.gwado.ui.komponen.itemTab
import io.github.quazzong.gwado.ui.komponen.Ekspresi
import io.github.quazzong.gwado.ui.komponen.Stiker
import io.github.quazzong.gwado.ui.komponen.Kotak
import io.github.quazzong.gwado.ui.komponen.TombolPlum
import java.time.Duration
import io.github.quazzong.gwado.layanan.Tahan
import io.github.quazzong.gwado.segar.SumberTerakhir
import io.github.quazzong.gwado.segar.Tingkat
import io.github.quazzong.gwado.segar.banner
import io.github.quazzong.gwado.segar.statusSumber
import io.github.quazzong.gwado.segar.sumberTerakhir
import io.github.quazzong.gwado.ui.komponen.KueTarik
import io.github.quazzong.gwado.ui.komponen.Pita
import io.github.quazzong.gwado.ui.komponen.TombolPermen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.quazzong.gwado.beranda.beres
import io.github.quazzong.gwado.beranda.beresDiSitus
import io.github.quazzong.gwado.beranda.selesaiMinggu
import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.segar.jamSingkat
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.tugas.tandaiSemua
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

/** Tugas belum dicentang yang situsnya mencatat sudah terkumpul atau ditandai selesai. */
internal fun beresSitus(t: BarisTugas): Boolean =
    t.status != "done" && beresDiSitus(t.statusKumpulSitus)

/**
 * Tugas belum dicentang dan belum beres di situs yang sudah lewat,
 * hari ini, besok, atau kurang dari 1 jam lagi.
 */
internal fun mendesak(t: BarisTugas, sekarang: Instant): Boolean =
    t.status != "done" && !beresSitus(t) &&
        (t.deadline?.let { sisaChip(dariDb(it), sekarang).makna != Makna.NORMAL } ?: false)

private class DataKepala(
    val kuliahMulai: LocalDate?,
    val semua: List<BarisTugas>,
    val diambil: Instant?,
    val sumber: List<SumberTerakhir>,
)

/** Kartu "Centang N yang lewat" yang keluar satu per satu; sisanya keluar bersamaan. */
private const val MAKS_KELUAR_BERJEDA = 8
private const val JEDA_KELUAR_LEWAT = 40

/** Beranda dianggap sudah tampil (ms setelah disusun): kotak yang muncul sesudahnya diberi gerak masuk. */
private const val SIAP_GERAK = 600L

/**
 * Mencatat kotak beranda (pita, kotak gagal/ditahan, keadaan kosong) yang sedang ada, supaya
 * gerak muncul hanya diputar saat kotak itu benar-benar baru muncul ketika beranda sudah tampil:
 * tidak saat beranda baru dibuka, dan tidak saat kotaknya tergulir keluar lalu masuk lagi.
 */
private class Kemunculan {
    var siap = false
    private val ada = mutableSetOf<String>()

    /** True kalau kotak [kunci] baru muncul sekarang dan perlu gerak masuk. */
    fun baru(kunci: String): Boolean = (siap && kunci !in ada).also { ada += kunci }

    /** Lupakan kotak yang sudah tidak ada, supaya kalau muncul lagi diberi gerak lagi. */
    fun sisakan(kunci: Set<String>) {
        ada.retainAll(kunci)
    }
}

/** Selesai run sukses terakhir dari sumber mana pun. */
internal fun Db.suksesTerakhir(): Instant? =
    (satu("SELECT max(selesai) AS s FROM source_runs WHERE status = 'success'")?.get("s") as String?)?.let(::dariDb)

/** Nama layanan di filter; nama panjang disingkat jadi huruf depan tiap kata (Google Classroom → GC). */
internal fun singkat(nama: String): String =
    if (nama.length > 10 && ' ' in nama) nama.split(' ').filter { it.isNotEmpty() }.joinToString("") { it.take(1) } else nama

/**
 * Beranda: kepala (minggu, hari, progres), ubin ringkasan, filter layanan, lalu tugas
 * dalam bagian MENDESAK, NANTI (tanpa deadline di akhirnya), dan SELESAI MINGGU INI
 * (dicentang atau beres di situs, deadline minggu ini). Yang beres di minggu lain ada
 * di layar semua yang selesai.
 * [versi] naik setiap kali data di database berubah. Ketuk kartu = buka detail.
 */
@OptIn(ExperimentalMaterial3Api::class)
/**
 * Hasil muat terakhir, dipakai sebagai nilai awal saat layar disusun ulang (misalnya kembali dari
 * detail), supaya layar langsung tampil lengkap dan tidak berkedip ke keadaan kosong.
 */
private var kepalaTerakhir: DataKepala? = null

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
    onBerubah: () -> Unit = {},
    onSemuaSelesai: () -> Unit = {},
    ditahan: Map<String, Tahan> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val w = GwadoTema.warna
    val konteks = LocalContext.current
    val cakupan = rememberCoroutineScope()
    var konfirmasiLewat by remember { mutableStateOf(false) }
    // Tugas yang sedang dicentang lewat "Centang N yang lewat": disembunyikan satu per satu dulu
    // (kartunya keluar berurutan), baru disimpan ke database.
    var menghilang by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(versi) { menghilang = emptySet() }
    val kemunculan = remember { Kemunculan() }
    LaunchedEffect(Unit) {
        delay(SIAP_GERAK)
        kemunculan.siap = true
    }
    val data by produceState(kepalaTerakhir, versi) {
        value = runCatching {
            Gudang.pakai(konteks) { db -> DataKepala(db.kuliahMulai(), db.daftarTugas(semua = true), db.suksesTerakhir(), db.sumberTerakhir()) }
        }.getOrNull()
        kepalaTerakhir = value
    }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val layanan = daftar.map { it.sumber ?: "Manual" }.distinct().sortedBy { it == "Manual" }
    val aktif = filter?.takeIf { it in layanan }
    val tampil = (if (aktif == null) daftar else daftar.filter { (it.sumber ?: "Manual") == aktif })
        .filterNot { it.id in menghilang }
    val belum = tampil.filterNot(::beresSitus)
    val (atas, bawah) = belum.partition { mendesak(it, sekarang) }
    val lewat = atas.filter { lewatDeadline(it, sekarang) }
    val selesai = selesaiMinggu(data?.semua.orEmpty(), sekarang.atOffset(WIB).toLocalDate())
        .filter { aktif == null || (it.sumber ?: "Manual") == aktif }
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
    val adaBeres = data?.semua.orEmpty().any(::beres)
    val adaLayanan = data?.sumber?.any { it.status != null } ?: true      // true selama memuat: tidak berkedip
    val kosong = when {
        daftar.isNotEmpty() && belum.isNotEmpty() -> null
        !adaLayanan && daftar.isEmpty() && !adaBeres -> Kosong.BELUM_LAYANAN
        daftar.isEmpty() && !adaBeres -> Kosong.BELUM_TUGAS
        else -> Kosong.BERES
    }
    val adaKotak = buildSet {
        if (pitaSumber != null && stSumber.any { it.tingkat == Tingkat.MERAH }) add("gagal")
        if (pitaSumber != null && stSumber.any { it.tingkat == Tingkat.KUNING }) add("pita-sumber")
        if (ditahan.isNotEmpty()) add("ditahan")
        if (hasilBaru != null) add("pita-ambil")
        if (kosong == Kosong.BERES) add("beres")
    }
    SideEffect { kemunculan.sisakan(adaKotak) }

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
        itemTab(key = "kepala") {
            KepalaBeranda(
                sekarang,
                data?.kuliahMulai,
                progresMinggu(data?.semua.orEmpty(), sekarang.atOffset(WIB).toLocalDate()),
            )
        }
        if (pitaSumber != null) {
            val merah = stSumber.filter { it.tingkat == Tingkat.MERAH }
            val kuning = stSumber.filter { it.tingkat == Tingkat.KUNING }
            if (merah.isNotEmpty()) {
                itemTab(key = "gagal") {
                    Box(Modifier.munculKotak(remember { kemunculan.baru("gagal") })) {
                        KotakGagal(merah, stSumber.filter { it.tingkat == Tingkat.NORMAL }, data?.sumber.orEmpty(), menyegarkan, onSegarkan, onBukaLayanan)
                    }
                }
            }
            if (kuning.isNotEmpty()) {
                itemTab(key = "pita-sumber") {
                    Pita(
                        kuning.joinToString(", ", postfix = ". Ketuk untuk membuka Layanan.") { "${it.nama} ${chipStatus(it).first}" },
                        Modifier.munculKotak(remember { kemunculan.baru("pita-sumber") }),
                        onClick = onBukaLayanan,
                    )
                }
            }
        }
        if (ditahan.isNotEmpty()) {
            itemTab(key = "ditahan") {
                Box(Modifier.munculKotak(remember { kemunculan.baru("ditahan") })) { KotakDitahan(ditahan) }
            }
        }
        if (hasilBaru != null) {
            itemTab(key = "pita-ambil") {
                Pita(hasilBaru.pesan.joinToString("\n"), Modifier.munculKotak(remember { kemunculan.baru("pita-ambil") }))
            }
        }
        itemTab(key = "ubin") {
            Row(Modifier.fillMaxWidth().padding(top = Jarak.s4), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Ubin("Aktif", "${daftar.count { !beresSitus(it) }}", Modifier.weight(1f))
                Ubin("Mendesak", "${daftar.count { mendesak(it, sekarang) }}", Modifier.weight(1f))
                Ubin("Diambil", data?.diambil?.let { jamSingkat(it, sekarang) } ?: "—", Modifier.weight(1f))
            }
        }
        if (layanan.size > 1) {
            itemTab(key = "filter") {
                Segmen(
                    listOf<Pair<String?, String>>(null to "Semua") + layanan.map { it to singkat(it) },
                    aktif = aktif,
                    onPilih = { filter = it },
                    modifier = Modifier.padding(vertical = Jarak.s4),
                )
            }
        }
        when (kosong) {
            null -> Unit
            Kosong.BELUM_LAYANAN -> itemTab(key = "kosong") {
                IsiKosong(Ekspresi.MELAMBAI, "Belum ada layanan tersambung", "Sambungkan Brone, Eling, atau Classroom supaya tugasmu muncul di sini.") {
                    TombolPermen("Tambah layanan", onBukaLayanan, Modifier.padding(top = Jarak.s12))
                    TombolGaris("Tugas manual", onTambah, Modifier.padding(top = Jarak.s8))
                }
            }
            Kosong.BELUM_TUGAS -> itemTab(key = "kosong") {
                IsiKosong(Ekspresi.SANTAI, "Belum ada tugas", "Tarik ke bawah untuk mengambil tugas dari situs kuliah, atau catat tugas dari grup kelas sendiri.") {
                    TombolPermen(if (menyegarkan) "Mengambil…" else "Ambil sekarang", onSegarkan, Modifier.padding(top = Jarak.s12))
                    TombolGaris("Tugas manual", onTambah, Modifier.padding(top = Jarak.s8))
                }
            }
            Kosong.BERES -> itemTab(key = "beres") {
                // Berubah jadi beres saat beranda terlihat: kotaknya muncul + stiker dirayakan dengan letupan.
                val rayakan = remember { kemunculan.baru("beres") }
                Box(Modifier.munculKotak(rayakan)) {
                    IsiKosong(
                        Ekspresi.BERSORAK,
                        if (aktif == null) "Semua tugas beres" else "Semua tugas ${singkat(aktif)} beres",
                        "Tidak ada tugas aktif. Yang sudah selesai tetap bisa dilihat di bawah.",
                        rayakan = rayakan,
                    )
                }
            }
        }
        bagian("Mendesak", atas, sekarang, onBuka) {
            if (lewat.size >= 2) TombolGaris("Centang ${lewat.size} yang lewat", { konfirmasiLewat = true })
        }
        bagian("Nanti", bawah, sekarang, onBuka)
        bagian("Selesai minggu ini", selesai, sekarang, onBuka)
        if (data?.semua.orEmpty().any(::beres)) {
            itemTab(key = "semua-selesai") {
                TombolGaris("Lihat semua yang selesai", onSemuaSelesai, Modifier.padding(top = Jarak.s4).asalTombol("layar-selesai"))
            }
        }
    }
    TombolPlumIkon(
        R.drawable.ikon_tambah,
        "Tambah tugas manual",
        onTambah,
        Modifier.align(Alignment.BottomEnd).padding(Jarak.tepiLayar).ikutLayar().asalTombol("layar-formulir"),
    )
    }

    if (konfirmasiLewat) {
        val warna = ButtonDefaults.textButtonColors(contentColor = w.tinta)
        AlertDialog(
            onDismissRequest = { konfirmasiLewat = false },
            title = { Text("Centang ${lewat.size} tugas yang lewat?", style = Huruf.judulBagian) },
            text = {
                Text(
                    (if (aktif != null) "Semua tugas $aktif " else "Semua tugas ") +
                        "berchip LEWAT ditandai selesai. Bisa dibatalkan satu per satu dari detail tugas.",
                    style = Huruf.isi,
                )
            },
            confirmButton = {
                TextButton({
                    konfirmasiLewat = false
                    val id = lewat.map { it.id }
                    cakupan.launch {
                        // Kartu keluar berurutan (jeda 40 ms, paling banyak 8 berjeda), baru disimpan.
                        if (Gerak.skala > 0f) {
                            id.take(MAKS_KELUAR_BERJEDA).forEach {
                                menghilang = menghilang + it
                                delay(Gerak.ms(JEDA_KELUAR_LEWAT).toLong())
                            }
                            menghilang = menghilang + id
                            delay(Gerak.ms(Gerak.PUDAR).toLong())
                        }
                        runCatching { Gudang.pakai(konteks) { it.tandaiSemua(id, Instant.now()) } }
                        onBerubah()
                    }
                }, colors = warna) { Text("Centang semua") }
            },
            dismissButton = { TextButton({ konfirmasiLewat = false }, colors = warna) { Text("Batal") } },
        )
    }
}

/** Keadaan kosong beranda (tanpa tugas aktif yang tampil). */
private enum class Kosong { BELUM_LAYANAN, BELUM_TUGAS, BERES }

/** Tugas belum selesai yang deadline-nya sudah lewat (chip LEWAT). */
private fun lewatDeadline(t: BarisTugas, sekarang: Instant): Boolean =
    t.deadline?.let { sisaChip(dariDb(it), sekarang).makna == Makna.LEWAT } ?: false

private fun LazyListScope.bagian(
    judul: String,
    isi: List<BarisTugas>,
    sekarang: Instant,
    onBuka: (String) -> Unit,
    aksi: @Composable () -> Unit = {},
) {
    if (isi.isEmpty()) return
    itemTab(key = "bagian-$judul") {
        Row(
            Modifier.fillMaxWidth().padding(top = Jarak.s8),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(judul.uppercase(), style = Huruf.label, color = GwadoTema.warna.tintaRedup)
            aksi()
        }
    }
    itemsTab(isi, key = { it.id }) { t -> KartuTugas(t, sekarang, onClick = { onBuka(t.id) }) }
}

/** Keadaan kosong di beranda: stiker, judul, satu kalimat, dan tombol opsional. */
@Composable
private fun IsiKosong(
    stiker: Ekspresi,
    judul: String,
    kalimat: String,
    rayakan: Boolean = false,
    tombol: @Composable ColumnScope.() -> Unit = {},
) {
    val w = GwadoTema.warna
    Column(Modifier.fillMaxWidth().padding(vertical = Jarak.s24), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Stiker(stiker, ukuran = 64.dp)
            // Letupan bentuk kecil dari stiker, sekali saat keadaan ini baru muncul.
            if (rayakan) Letupan(1, Modifier.size(64.dp))
        }
        Text(judul, style = Huruf.judulBagian, color = w.tinta, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        Text(kalimat, style = Huruf.isi, color = w.tintaRedup, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        tombol()
    }
}

/**
 * Kotak merah untuk layanan yang gagal pada pengambilan terakhir: apa yang terjadi, bahwa data
 * lama aman, layanan lain yang berhasil, lalu Coba lagi (gagal biasa) atau Buka Layanan (login/izin ditolak).
 */
@Composable
private fun KotakGagal(
    merah: List<StatusSumber>,
    normal: List<StatusSumber>,
    baris: List<SumberTerakhir>,
    menyegarkan: Boolean,
    onCoba: () -> Unit,
    onLayanan: () -> Unit,
) {
    val w = GwadoTema.warna
    val ditolak = merah.filter { it.label == "token ditolak" }
    val gagal = merah - ditolak.toSet()
    val kalimat = buildList {
        for (st in ditolak) {
            add(if (st.id == "classroom") "Google menolak izin. Beri izin lagi lewat tab Layanan." else "${st.nama} menolak login. Login ulang lewat tab Layanan.")
        }
        for (st in gagal) {
            val jaringan = baris.firstOrNull { it.id == st.id }?.errorSingkat?.startsWith("jaringan") == true
            add(if (jaringan) "${st.nama} tidak bisa dihubungi, sudah dicoba 3 kali." else "${st.nama} gagal diambil.")
        }
        add("Tugas lama tetap aman di HP.")
        if (normal.isNotEmpty()) add("${normal.joinToString(" dan ") { it.nama }} berhasil diambil.")
    }
    Kotak(Modifier.fillMaxWidth(), latar = SolidColor(w.latarLewat), bayangan = Jarak.bayanganKecil) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stiker(Ekspresi.BINGUNG, ukuran = 64.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Gagal mengambil dari ${merah.joinToString(" dan ") { it.nama }}", style = Huruf.judulKartu, color = w.tinta)
            Text(kalimat.joinToString(" "), style = Huruf.isi.copy(fontSize = 13.sp), color = w.tinta)
            Row(Modifier.padding(top = Jarak.s4), horizontalArrangement = Arrangement.spacedBy(Jarak.s8)) {
                if (gagal.isNotEmpty()) TombolPlum(if (menyegarkan) "Mengambil…" else "Coba lagi", onCoba)
                if (ditolak.isNotEmpty()) TombolGaris("Buka Layanan", onLayanan)
            }
        }
      }
    }
}

/** Kotak kuning untuk layanan yang sedang ditahan, dengan hitung mundur per detik. */
@Composable
private fun KotakDitahan(ditahan: Map<String, Tahan>) {
    val w = GwadoTema.warna
    val kini by produceState(Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(1000)
        }
    }
    val masih = DAFTAR_LAYANAN.mapNotNull { l -> ditahan[l.id]?.takeIf { it.sampai.isAfter(kini) }?.let { l to it } }
    if (masih.isEmpty()) return
    Kotak(Modifier.fillMaxWidth(), latar = SolidColor(w.latarMepet), bayangan = Jarak.bayanganKecil) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stiker(Ekspresi.TIDUR, ukuran = 64.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ditahan sementara", style = Huruf.judulKartu, color = w.tinta)
            for ((l, t) in masih) {
                val teks = when (t.sebab) {
                    SebabTertahan.JENDELA_HINDARI -> {
                        val j = t.sampai.atOffset(WIB)
                        "${l.label} sedang backup, jadi tidak dihubungi sampai %02d.%02d WIB.".format(j.hour, j.minute)
                    }
                    SebabTertahan.COOLDOWN -> {
                        val detik = Duration.between(kini, t.sampai).seconds.coerceAtLeast(1)
                        val sisa = if (detik < 60) "$detik detik" else "%d.%02d menit".format(detik / 60, detik % 60)
                        "${l.label} baru saja diambil. Bisa diambil lagi dalam $sisa."
                    }
                }
                Text(teks, style = Huruf.isi.copy(fontSize = 13.sp), color = w.tinta)
            }
            Text("Tugas lama tetap aman di HP.", style = Huruf.meta, color = w.tintaRedup)
        }
      }
    }
}
