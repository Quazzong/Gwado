package io.github.quazzong.gwado.ui

import io.github.quazzong.gwado.ui.tema.Gerak
import io.github.quazzong.gwado.ui.komponen.ikutLayar
import io.github.quazzong.gwado.ui.komponen.Terbang
import io.github.quazzong.gwado.ui.komponen.LocalTerbang
import androidx.compose.foundation.layout.Box
import io.github.quazzong.gwado.ui.komponen.tahanSampaiSelesai
import kotlinx.coroutines.launch
import io.github.quazzong.gwado.ui.layar.praMuatDetail
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.ContentTransform
import io.github.quazzong.gwado.ui.komponen.latarLayar
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import android.os.SystemClock
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.compose.ui.semantics.clearAndSetSemantics
import io.github.quazzong.gwado.ui.komponen.LocalLingkupUtama
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.layanan.HasilAmbil
import io.github.quazzong.gwado.layanan.Tahan
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.ui.komponen.ItemTab
import io.github.quazzong.gwado.ui.komponen.LocalMasukTab
import io.github.quazzong.gwado.ui.komponen.MasukTab
import io.github.quazzong.gwado.ui.komponen.TabBar
import io.github.quazzong.gwado.ui.komponen.rememberSekarang
import io.github.quazzong.gwado.ui.layar.Beranda
import io.github.quazzong.gwado.ui.layar.LayarKalender
import io.github.quazzong.gwado.ui.layar.LayarMatkul
import io.github.quazzong.gwado.ui.layar.LayarPengaturan
import io.github.quazzong.gwado.ui.layar.LayarSemester
import io.github.quazzong.gwado.ui.layar.LayarSemuaSelesai
import io.github.quazzong.gwado.ui.layar.LayarFormulir
import io.github.quazzong.gwado.ui.layar.LayarDetail
import io.github.quazzong.gwado.ui.layar.mendesak
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Jarak
import kotlinx.serialization.Serializable

/** Layar dengan tab bar. Layar penuh lain (detail, formulir) ditumpuk di atasnya. */
@Serializable
data object Utama : NavKey

/** Detail satu tugas, menutupi tab bar. */
@Serializable
data class Detail(val id: String) : NavKey

/** Formulir tugas manual: id null = tugas baru. */
@Serializable
data class Formulir(val id: String? = null) : NavKey

/** Login Moodle satu layanan lewat browser, menutupi tab bar. */
@Serializable
data class Masuk(val id: String) : NavKey

/** Daftar mata kuliah satu layanan beserta pengecualian manualnya. */
@Serializable
data class DaftarMatkul(val sumber: String) : NavKey

/** Semua tugas yang dicentang atau beres di situs. */
@Serializable
data object SemuaSelesai : NavKey

/** Pengaturan tampilan: tekstur latar dan tema. */
@Serializable
data object Pengaturan : NavKey

/** Ubah semester aktif dan tanggal kuliah pertama. */
@Serializable
data object AturSemester : NavKey

private enum class Tab { TUGAS, KALENDER, LAYANAN }

/** Layar lama ditahan (ms) saat layar penuh dibuka dari layar selain layar utama: selama bidang baru melebar. */
private const val TAHAN_BUKA = Gerak.MULUS + 150

/** Layar yang ditutup ditahan (ms) selama gerak tutupnya (isi memudar, kartu/bidang menyusut). */
private const val TAHAN_TUTUP = Gerak.TUTUP + 40 + 150

/** Layar utama disembunyikan (ms, waktu nyata) setelah tertutup layar penuh, sesudah gerak bukanya selesai. */
private const val SEMBUNYIKAN_UTAMA = (Gerak.MULUS + 460).toLong()

/** Lama layar tidak disentuh (ms) sebelum tab yang belum dibuka disusun di belakang layar. */
private const val DIAM_SEBELUM_SUSUN = 1500L

/** Akar tampilan: back stack layar, dimulai dari layar utama (atau Siapkan Gwado kalau [siapkan] diisi). */
@Composable
fun Akar(
    daftar: List<BarisTugas>,
    versi: Int,
    muatUlang: () -> Unit,
    menyegarkan: Boolean,
    onSegarkan: () -> Unit,
    hasilAmbil: HasilAmbil?,
    ditahan: Map<String, Tahan>,
    lencanaLayanan: Boolean,
    bukaTugas: String? = null,
    onTugasDibuka: () -> Unit = {},
    layanan: @Composable (buka: (NavKey) -> Unit) -> Unit,
    masuk: @Composable (id: String, onKembali: () -> Unit) -> Unit,
    siapkan: (@Composable (buka: (NavKey) -> Unit) -> Unit)? = null,
) {
    val tumpukan = rememberNavBackStack(Utama)
    val konteks = LocalContext.current
    val cakupan = rememberCoroutineScope()
    val terbang = remember { Terbang(cakupan) }
    val buka = { k: NavKey ->
        if (k !is Detail) terbang.lupakan()       // layar lain tidak dibuka dari kartu: daftar tidak terbelah
        if (tumpukan.lastOrNull() != k) tumpukan.add(k)
    }
    // Menutup layar [k] hanya kalau ia masih paling atas. Layar yang sedang ditutup masih tampil
    // selama geraknya, dan tombol kembalinya masih bisa diketuk lagi: tanpa pengecekan ini ketukan
    // kedua ikut menutup layar di bawahnya (sampai tumpukan kosong dan aplikasi berhenti).
    val tutup = { k: NavKey ->
        if (tumpukan.size > 1 && tumpukan.lastOrNull() == k) tumpukan.removeAt(tumpukan.lastIndex)
        Unit
    }
    // Kartu yang diketuk dicatat posisinya, detail dimuat dulu (beberapa ms), baru dibuka:
    // kepala detail sudah ada di frame pertama dan kartu terbang dari posisi diamnya.
    val bukaDetail = { id: String ->
        terbang.siapkan(id)
        cakupan.launch {
            runCatching { praMuatDetail(konteks, id) }
            buka(Detail(id))
        }
        Unit
    }
    // Ketuk tugas di widget: buka detailnya (diabaikan selama Siapkan Gwado tampil).
    LaunchedEffect(bukaTugas) {
        if (bukaTugas != null) {
            if (siapkan == null) buka(Detail(bukaTugas))
            onTugasDibuka()
        }
    }
    // Layar utama (tab) disusun sekali di bawah tumpukan layar dan tidak dibuang saat layar penuh
    // dibuka, supaya kembali dari detail tidak menyusun ulang seluruh daftar. Entri Utama di
    // NavDisplay hanya penanda tembus pandang yang melaporkan keadaannya: [tujuanUtama] = masuk
    // atau keluar (penggerak gerak elemen), [utamaTampil] = masih tampil di NavDisplay (termasuk
    // selama ditahan sampai gerak buka selesai, dan selama gestur kembali).
    var tujuanUtama by remember { mutableStateOf(EnterExitState.Visible) }
    var utamaTampil by remember { mutableStateOf(true) }
    val lingkupUtama = updateTransition(tujuanUtama, label = "utama")
    // Layar utama tetap digambar sampai gerak buka layar di atasnya selesai, lalu disembunyikan.
    var utamaTersembunyi by remember { mutableStateOf(false) }
    LaunchedEffect(tujuanUtama) {
        if (tujuanUtama == EnterExitState.Visible) {
            utamaTersembunyi = false
        } else {
            delay(SEMBUNYIKAN_UTAMA)
            utamaTersembunyi = true
        }
    }
    Box(Modifier.fillMaxSize()) {
    CompositionLocalProvider(LocalTerbang provides terbang) {
    if (siapkan == null) {
        val tertutup = tujuanUtama != EnterExitState.Visible
        Box(
            Modifier
                .fillMaxSize()
                // Tertutup penuh oleh layar lain: tidak ikut digambar.
                .graphicsLayer {
                    alpha = if (!utamaTersembunyi || utamaTampil || tumpukan.lastOrNull() == Utama) 1f else 0f
                }
                .then(if (tertutup) Modifier.tahanSentuhan().clearAndSetSemantics {} else Modifier),
        ) {
            CompositionLocalProvider(LocalLingkupUtama provides lingkupUtama) {
                LayarUtama(daftar, versi, bukaDetail, { buka(Formulir()) }, menyegarkan, onSegarkan, hasilAmbil, muatUlang, { buka(SemuaSelesai) }, ditahan, lencanaLayanan) {
                    layanan(buka)
                }
            }
        }
    }
    NavDisplay(
        backStack = tumpukan,
        onBack = { if (tumpukan.size > 1) tumpukan.removeAt(tumpukan.lastIndex) },
        // Layar penuh tidak bergeser sebagai satu lembar: bidangnya melebar dari yang diketuk dan
        // isinya masuk sendiri (KartuTerbang, LayarPenuh, isiLayar). Layar lama ditahan sampai
        // gerak itu selesai; saat kembali, layar bawah digambar di bawah layar yang ditutup.
        // Selama transisi NavDisplay berjalan, tombol kembali harus menunggu transisi itu diputar
        // mundur, jadi penahanan dibuat sesingkat mungkin. Dari layar utama tidak perlu menahan
        // apa pun (layar utama tetap tersusun di bawah NavDisplay): transisinya selesai seketika.
        transitionSpec = {
            if (siapkan == null && tumpukan.size == 2) {
                ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)
            } else {
                ContentTransform(EnterTransition.None, tahanSampaiSelesai(TAHAN_BUKA), sizeTransform = null)
            }
        },
        popTransitionSpec = {
            ContentTransform(EnterTransition.None, tahanSampaiSelesai(TAHAN_TUTUP), targetContentZIndex = -1f, sizeTransform = null)
        },
        predictivePopTransitionSpec = {
            ContentTransform(EnterTransition.None, tahanSampaiSelesai(TAHAN_TUTUP), targetContentZIndex = -1f, sizeTransform = null)
        },
        entryProvider = entryProvider {
            entry<Utama> {
                if (siapkan != null) {
                    LayarNav { siapkan(buka) }
                } else {
                    PenandaUtama({ tujuanUtama = it }, { utamaTampil = it })
                }
            }
            entry<Detail> { k ->
                LayarNav {
                    LayarDetail(
                        k.id,
                        onKembali = { tutup(k) },
                        onBerubah = muatUlang,
                        onUbah = { id -> buka(Formulir(id)) },
                    )
                }
            }
            entry<Formulir> { k ->
                LayarNav {
                    LayarFormulir(
                        k.id,
                        onKembali = { tutup(k) },
                        onTersimpan = {
                            muatUlang()
                            tutup(k)
                        },
                    )
                }
            }
            entry<Masuk> { k -> LayarNav { masuk(k.id) { tutup(k) } } }
            entry<DaftarMatkul> { k -> LayarNav { LayarMatkul(k.sumber, onKembali = { tutup(k) }) } }
            entry<Pengaturan> { k ->
                LayarNav { LayarPengaturan(onKembali = { tutup(k) }, onBerubah = muatUlang) }
            }
            entry<SemuaSelesai> { k ->
                LayarNav { LayarSemuaSelesai(versi, onBuka = bukaDetail, onKembali = { tutup(k) }) }
            }
            entry<AturSemester> { k ->
                LayarNav {
                    LayarSemester(
                        onKembali = { tutup(k) },
                        onTersimpan = {
                            muatUlang()
                            tutup(k)
                        },
                    )
                }
            }
        },
    )
    }
    }
}

/**
 * Isi entri Utama di NavDisplay: tembus pandang, hanya melaporkan apakah layar utama sedang
 * masuk/tampil atau keluar ([tujuan]) dan apakah entrinya masih tersusun ([tampil]).
 */
@Composable
private fun PenandaUtama(tujuan: (EnterExitState) -> Unit, tampil: (Boolean) -> Unit) {
    val target = LocalNavAnimatedContentScope.current.transition.targetState
    SideEffect { tujuan(target) }
    DisposableEffect(Unit) {
        tampil(true)
        onDispose {
            tampil(false)
            tujuan(EnterExitState.PostExit)
        }
    }
}

/** Bentuk tanpa bidang: lapisan yang dipotong dengan bentuk ini tidak tergambar dan tidak bisa disentuh. */
private val TANPA_BIDANG = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Rectangle(Rect.Zero)
}

/**
 * Isi satu layar di NavDisplay. Selama layar ini keluar (masih digambar untuk gerak tutupnya),
 * ketukan tembus ke layar di bawahnya: isinya dipotong ke bidang kosong (tidak ikut uji sentuhan,
 * jadi tombolnya yang sedang memudar juga tidak bisa tertekan), sementara gambarnya direkam ke
 * lapisan sendiri dan tetap digambar di tempatnya sampai gerak tutup selesai.
 */
@Composable
private fun LayarNav(isi: @Composable () -> Unit) {
    val lingkup = LocalNavAnimatedContentScope.current.transition
    val keluar by remember(lingkup) { derivedStateOf { lingkup.targetState == EnterExitState.PostExit } }
    val lapisan = rememberGraphicsLayer()
    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (keluar) drawLayer(lapisan)
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (keluar) {
                        clip = true
                        shape = TANPA_BIDANG
                    }
                }
                .drawWithContent {
                    if (keluar) lapisan.record { this@drawWithContent.drawContent() } else drawContent()
                }
                .then(if (keluar) Modifier.clearAndSetSemantics {} else Modifier),
        ) { isi() }
    }
}

/** Geser tab dianggap jadi kalau jarinya bergeser sejauh ini, atau dilempar secepat ini (per detik). */
private val JARAK_GESER_TAB = 72.dp
private val LAJU_GESER_TAB = 900.dp

/**
 * Geser mendatar di isi tab untuk pindah tab: ke kiri = tab di kanan ([onGeser] dengan 1), ke kanan
 * = tab di kiri (-1). Sentuhan dibaca sesudah isi tab, jadi gestur yang sudah dipakai isinya
 * (geser bulan di grid kalender, gulir daftar, tarik untuk ambil) tidak ikut memindah tab. Begitu
 * geserannya jelas mendatar, sentuhan dipakai di sini supaya kartu di bawah jari tidak terketuk.
 */
private fun Modifier.geserTab(onGeser: (Int) -> Unit): Modifier = pointerInput(Unit) {
    val jarak = JARAK_GESER_TAB.toPx()
    val laju = LAJU_GESER_TAB.toPx()
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val turun = awaitFirstDown(requireUnconsumed = false)
        val pelacak = VelocityTracker().apply { addPosition(turun.uptimeMillis, turun.position) }
        var dx = 0f
        var dy = 0f
        var milik = false
        while (true) {
            val e = awaitPointerEvent()
            if (e.changes.size > 1) break                       // dua jari: bukan geser tab
            val c = e.changes.firstOrNull { it.id == turun.id } ?: break
            if (!milik && c.isConsumed) break                   // sudah dipakai isi tab
            if (!c.pressed) {
                if (milik) {
                    val v = pelacak.calculateVelocity().x
                    when {
                        dx < 0 && (dx <= -jarak || v <= -laju) -> onGeser(1)
                        dx > 0 && (dx >= jarak || v >= laju) -> onGeser(-1)
                    }
                    c.consume()
                }
                break
            }
            val d = c.positionChange()
            dx += d.x
            dy += d.y
            pelacak.addPosition(c.uptimeMillis, c.position)
            if (!milik) {
                if (abs(dy) > slop && abs(dy) >= abs(dx)) break   // gulir tegak
                if (abs(dx) > slop && abs(dx) > 2 * abs(dy)) milik = true
            }
            if (milik) c.consume()
        }
    }
}

/** Menelan semua sentuhan sebelum sampai ke isinya (layar yang sedang tertutup layar lain). */
private fun Modifier.tahanSentuhan(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}

/**
 * Isi satu tab yang tetap tersusun. Saat tab lain dipilih, elemennya mengalir keluar
 * ([LocalMasukTab]), lalu isinya disembunyikan: tidak digambar, tidak menerima sentuhan, dan
 * tidak terbaca pembaca layar. Saat dipilih lagi, elemennya masuk seperti baru. [pertama] = tab
 * yang tampil saat layar utama dibuka (tanpa gerak masuk).
 */
@Composable
private fun IsiTab(aktif: Boolean, pertama: Boolean, arah: () -> Int, isi: @Composable () -> Unit) {
    val keadaan = remember {
        MutableTransitionState(if (pertama) EnterExitState.Visible else EnterExitState.PreEnter)
            .apply { targetState = EnterExitState.Visible }
    }
    SideEffect { keadaan.targetState = if (aktif) EnterExitState.Visible else EnterExitState.PostExit }
    val lingkup = rememberTransition(keadaan, label = "tab")
    val masuk = remember { MasukTab(lingkup, arah) }
    var tersembunyi by remember { mutableStateOf(!aktif) }
    LaunchedEffect(aktif) {
        if (aktif) {
            tersembunyi = false
        } else {
            delay((Gerak.PUDAR + 6 * Gerak.JEDA_KELUAR + 60).toLong())
            tersembunyi = true
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(if (aktif) 1f else 0f)
            .graphicsLayer { alpha = if (tersembunyi) 0f else 1f }
            .then(if (aktif) Modifier else Modifier.tahanSentuhan().clearAndSetSemantics {}),
    ) {
        CompositionLocalProvider(LocalMasukTab provides masuk) { isi() }
    }
}

@Composable
private fun LayarUtama(
    daftar: List<BarisTugas>,
    versi: Int,
    bukaDetail: (String) -> Unit,
    tambah: () -> Unit,
    menyegarkan: Boolean,
    onSegarkan: () -> Unit,
    hasilAmbil: HasilAmbil?,
    muatUlang: () -> Unit,
    bukaSelesai: () -> Unit,
    ditahan: Map<String, Tahan>,
    lencanaLayanan: Boolean,
    layanan: @Composable () -> Unit,
) {
    val w = GwadoTema.warna
    val sekarang = rememberSekarang()
    var tab by rememberSaveable { mutableStateOf(Tab.TUGAS) }
    var arah by remember { mutableIntStateOf(1) }
    // Tab yang pernah dibuka tetap tersusun (hanya disembunyikan), supaya kembali ke tab itu tidak
    // menyusun ulang seluruh isinya.
    val tabAwal = remember { tab }
    val dikunjungi = remember { mutableStateListOf(tab) }
    val pilihTab = { baru: Tab ->
        if (baru != tab) {
            arah = if (baru.ordinal > tab.ordinal) 1 else -1
            if (baru !in dikunjungi) dikunjungi.add(baru)
            tab = baru
        }
    }
    // Tab lain disusun lebih dulu di belakang layar, satu per satu, saat layar sudah 1,5 detik tidak
    // disentuh: menyusun satu tab bisa makan beberapa frame, jadi jangan sampai terjadi saat
    // pengguna sedang menggulir atau mengetuk.
    val terakhirSentuh = remember { longArrayOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(Unit) {
        for (t in Tab.entries) {
            if (t in dikunjungi) continue
            while (SystemClock.uptimeMillis() - terakhirSentuh[0] < DIAM_SEBELUM_SUSUN) delay(250)
            if (t !in dikunjungi) dikunjungi.add(t)
            withFrameNanos { }
            withFrameNanos { }
        }
    }
    val penyimpan = rememberSaveableStateHolder()
    val jumlahMendesak = daftar.count { mendesak(it, sekarang) }
    val item = listOf(
        ItemTab(Tab.TUGAS, "Tugas", R.drawable.ikon_tugas, jumlahMendesak.takeIf { it > 0 }?.toString()),
        ItemTab(Tab.KALENDER, "Kalender", R.drawable.ikon_kalender),
        ItemTab(Tab.LAYANAN, "Layanan", R.drawable.ikon_layanan, if (lencanaLayanan) "!" else null),
    )
    Column(
        Modifier
            .fillMaxSize()
            .latarLayar()
            .safeDrawingPadding()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        terakhirSentuh[0] = SystemClock.uptimeMillis()
                    }
                }
            },
    ) {
        // Ganti tab: isi lama mengalir keluar, isi baru mengalir masuk lalu memantul, per elemen
        // (Modifier.masukTab di tiap layar). Isi pertama saat dibuka tidak dianimasikan.
        val geser by rememberUpdatedState { langkah: Int -> Tab.entries.getOrNull(tab.ordinal + langkah)?.let(pilihTab) }
        Box(Modifier.weight(1f).fillMaxWidth().geserTab { geser(it) }) {
            for (t in dikunjungi) {
                key(t) {
                    IsiTab(aktif = t == tab, pertama = t == tabAwal, arah = { arah }) {
                        penyimpan.SaveableStateProvider(t) {
                            when (t) {
                                Tab.TUGAS -> Beranda(
                                    daftar, versi, sekarang, bukaDetail, tambah, menyegarkan, onSegarkan, hasilAmbil,
                                    onBukaLayanan = { pilihTab(Tab.LAYANAN) },
                                    onBerubah = muatUlang,
                                    onSemuaSelesai = bukaSelesai,
                                    ditahan = ditahan,
                                )
                                Tab.KALENDER -> LayarKalender(daftar, versi, sekarang, bukaDetail)
                                Tab.LAYANAN -> layanan()
                            }
                        }
                    }
                }
            }
        }
        TabBar(
            item,
            aktif = tab,
            onPilih = pilihTab,
            modifier = Modifier.ikutLayar().padding(start = Jarak.tepiLayar, end = Jarak.tepiLayar, top = Jarak.s8, bottom = Jarak.s12),
        )
    }
}
