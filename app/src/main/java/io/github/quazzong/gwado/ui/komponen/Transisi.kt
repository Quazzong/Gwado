package io.github.quazzong.gwado.ui.komponen

import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Transition
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import io.github.quazzong.gwado.ui.tema.Gerak
import io.github.quazzong.gwado.ui.tema.gtween
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Lingkup masuk-keluar isi satu tab: [lingkup] = keadaan tab (tampil atau keluar). [arah] dibaca
 * saat gerak dimulai (1 = tab di kanan, -1 = tab di kiri). Elemen mencatat letaknya di sini,
 * supaya urutan geraknya mengikuti posisinya di layar saat tab berganti.
 */
class MasukTab(val lingkup: Transition<EnterExitState>, val arah: () -> Int) {
    internal val letak = mutableMapOf<Any, LayoutCoordinates>()

    /**
     * Urutan elemen [kunci] di antara elemen yang terlihat di layar (atas ke bawah, lalu kiri ke
     * kanan) dan jumlah elemen yang terlihat; null kalau elemen ini tidak terlihat.
     */
    internal fun urutan(kunci: Any): Pair<Int, Int>? {
        val terlihat = letak.mapNotNull { (k, c) ->
            if (!c.isAttached) return@mapNotNull null
            val b = c.boundsInRoot()                     // sudah dipotong oleh daftar yang menggulir
            if (b.width <= 0f || b.height <= 0f) null else k to b
        }.sortedWith(compareBy({ it.second.top }, { it.second.left }))
        val i = terlihat.indexOfFirst { it.first == kunci }
        return if (i < 0) null else i to terlihat.size
    }
}

/**
 * Jeda per elemen untuk [jumlah] elemen yang bergerak berurutan: [jeda] ms, mengecil kalau
 * elemennya lebih dari [Gerak.MAKS_BERJEDA] supaya elemen terakhir tetap mulai paling lambat
 * jeda × MAKS_BERJEDA setelah yang pertama. Jadi semuanya tetap satu per satu.
 */
internal fun jedaBerurutan(jeda: Int, jumlah: Int): Int =
    if (jumlah <= 1) jeda else minOf(jeda, jeda * Gerak.MAKS_BERJEDA / (jumlah - 1))

val LocalMasukTab = compositionLocalOf<MasukTab?> { null }

/**
 * Keadaan masuk-keluar layar utama, yang tetap tersusun di bawah layar penuh (tidak ikut dibuang
 * saat layar penuh dibuka). Null di dalam layar navigasi biasa.
 */
val LocalLingkupUtama = compositionLocalOf<Transition<EnterExitState>?> { null }

/** Mulai isi baru masuk setelah isi lama mulai mengalir keluar. */
private const val MULAI_MASUK = 110

/** Butir kecil (tanggal) mulai meletup setelah wadahnya mulai mendarat. */
private const val MULAI_BUTIR = MULAI_MASUK + Gerak.JEDA_TAB + 80

/** Gerak masuk: dari geser ([dx], [dy] dp), skala [skala], tak terlihat → tempatnya. */
private class Masuk(val dx: Float, val dy: Float, val skala: Float, val durasi: Int, val tunda: Int, val kurva: Easing)

/** Gerak keluar: ke geser ([dx], [dy] dp) sambil memudar. */
private class Keluar(val dx: Float, val dy: Float, val durasi: Int, val tunda: Int, val kurva: Easing)

private fun Transition<EnterExitState>.sedangMasuk(): Boolean =
    targetState == EnterExitState.Visible && currentState != EnterExitState.Visible

/**
 * Keadaan masuk-keluar layar tempat elemen ini berada: layar utama ([LocalLingkupUtama]) atau
 * layar navigasi. Null di luar layar navigasi, misalnya pratinjau.
 */
@Composable
private fun lingkupLayar(): Transition<EnterExitState>? =
    LocalLingkupUtama.current ?: if (LocalTerbang.current == null) null else LocalNavAnimatedContentScope.current.transition

/**
 * Gerak masuk-keluar milik sendiri. Gerak masuk dimulai SETELAH elemen tersusun, jadi frame
 * pertama yang berat (menyusun layar/tab baru) tidak memotong awal gerak; gerak keluar dimulai
 * begitu [lingkup] mulai keluar. Lingkup hanya dipakai sebagai penanda kapan masuk/keluar
 * (isinya ditahan cukup lama oleh transisinya, lihat [tahanSampaiSelesai]). Kalau lingkup
 * tampil lagi setelah keluar tanpa elemennya disusun ulang (layar utama, tab yang dipilih
 * lagi sebelum sempat dilepas), gerak masuk diputar lagi dari keadaan keluar.
 * [masuk]/[keluar] dihitung saat gerak dimulai, jadi arah dan jarak memakai keadaan saat itu.
 */
@Composable
private fun Modifier.gerakLingkup(
    lingkup: Transition<EnterExitState>,
    masuk: (() -> Masuk?)?,
    keluar: (() -> Keluar?)?,
    tungguTata: Boolean = false,
): Modifier {
    val px = LocalDensity.current.density
    val awalMasuk = remember { masuk != null && lingkup.sedangMasuk() }
    // Gerak masuk sedang berlaku: sejak awal tersusun, atau sejak tampil lagi setelah keluar.
    val masukBerlaku = remember { booleanArrayOf(awalMasuk) }
    val a = remember { Animatable(if (awalMasuk) 0f else 1f) }
    val k = remember { Animatable(0f) }
    val m = remember { arrayOfNulls<Masuk>(1) }
    val x = remember { arrayOfNulls<Keluar>(1) }
    if (awalMasuk) {
        LaunchedEffect(Unit) {
            // Letak elemen baru diketahui setelah satu frame tata letak.
            if (tungguTata) withFrameNanos { }
            val s = masuk!!()
            if (s == null) {
                a.snapTo(1f)                                // tanpa gerak: langsung di tempatnya
            } else {
                m[0] = s
                a.animateTo(1f, gtween(s.durasi, s.tunda, s.kurva))
            }
        }
    }
    val pernahKeluar = remember { booleanArrayOf(false) }
    LaunchedEffect(lingkup) {
        snapshotFlow { lingkup.targetState }.collectLatest { st ->
            if (st == EnterExitState.PostExit) {
                pernahKeluar[0] = true
                val s = keluar?.invoke()
                if (s != null) {
                    x[0] = s
                    k.animateTo(1f, gtween(s.durasi, s.tunda, s.kurva))
                }
            } else if (st == EnterExitState.Visible && pernahKeluar[0]) {
                // Tampil lagi tanpa disusun ulang: mulai dari tak terlihat, lalu masuk seperti baru.
                pernahKeluar[0] = false
                val s = masuk?.invoke()
                if (s != null) {
                    m[0] = s
                    masukBerlaku[0] = true
                    a.snapTo(0f)
                }
                x[0] = null
                k.snapTo(0f)
                if (s != null) a.animateTo(1f, gtween(s.durasi, s.tunda, s.kurva))
            }
        }
    }
    return graphicsLayer {
        val v = a.value
        val w = k.value
        val sm = m[0]
        val sk = x[0]
        var tx = 0f
        var ty = 0f
        var alfa = 1f
        if (masukBerlaku[0]) {
            // Sebelum gerak dimulai (frame penyusunan) v = 0: elemen tak terlihat.
            if (sm != null) {
                tx += (1f - v) * sm.dx * px
                ty += (1f - v) * sm.dy * px
                val skala = sm.skala + (1f - sm.skala) * v
                scaleX = skala
                scaleY = skala
            }
            alfa *= v.coerceIn(0f, 1f)
        }
        if (sk != null) {
            tx += w * sk.dx * px
            ty += w * sk.dy * px
            alfa *= 1f - w
        }
        translationX = tx
        translationY = ty
        alpha = alfa
    }
}

/**
 * Elemen terluar isi tab: saat ganti tab, isi lama mengalir keluar berlawanan arah tab sambil
 * memudar (jeda 14 ms per elemen), lalu isi baru mengalir masuk dari arah tab dan memantul ke
 * tempatnya (jeda 60 ms). Hanya elemen yang terlihat di layar yang bergerak, berurutan menurut
 * posisinya (atas ke bawah); jedanya mengecil kalau elemen terlihat banyak ([jedaBerurutan]).
 * Elemen di luar layar langsung di tempatnya. Tanpa efek di luar isi tab.
 */
@Composable
fun Modifier.masukTab(): Modifier {
    val mt = LocalMasukTab.current ?: return this
    val kunci = remember { Any() }
    DisposableEffect(mt) { onDispose { mt.letak.remove(kunci) } }
    return onPlaced { mt.letak[kunci] = it }.gerakLingkup(
        mt.lingkup,
        masuk = {
            mt.urutan(kunci)?.let { (u, n) ->
                Masuk(mt.arah() * 40f, 36f, 0.9f, Gerak.PEGAS, MULAI_MASUK + u * jedaBerurutan(Gerak.JEDA_TAB, n), Gerak.Pegas)
            }
        },
        keluar = {
            mt.urutan(kunci)?.let { (u, n) ->
                Keluar(-mt.arah() * 26f, 0f, Gerak.PUDAR, u * jedaBerurutan(Gerak.JEDA_KELUAR, n), Gerak.Keluar)
            }
        },
        tungguTata = true,
    )
}

/** Butir kecil di dalam elemen tab (tanggal kalender): meletup dari skala 0,45 setelah wadahnya, berurutan. */
@Composable
fun Modifier.masukButir(urutan: Int): Modifier {
    val mt = LocalMasukTab.current ?: return this
    return gerakLingkup(
        mt.lingkup,
        masuk = { Masuk(0f, 16f, 0.45f, Gerak.BUTIR, MULAI_BUTIR + urutan * Gerak.JEDA_BUTIR, Gerak.Pegas) },
        keluar = null,
    )
}

/** Butir kecil meletup dari skala 0,45 (+ naik 16dp) saat [lingkup] masuk, mulai [mulai] ms, jeda [jeda] ms per butir. */
@Composable
fun Modifier.letupButir(lingkup: AnimatedVisibilityScope, urutan: Int, mulai: Int, jeda: Int): Modifier =
    gerakLingkup(lingkup.transition, masuk = { Masuk(0f, 16f, 0.45f, Gerak.BUTIR, mulai + urutan * jeda, Gerak.Pegas) }, keluar = null)

/** Isi [lingkup] mengalir keluar [arah] (−1 kiri, 1 kanan) sambil memudar; dipakai grid saat ganti bulan. */
@Composable
fun Modifier.keluarSearah(lingkup: AnimatedVisibilityScope, arah: () -> Int): Modifier =
    gerakLingkup(lingkup.transition, masuk = null, keluar = { Keluar(arah() * 26f, 0f, Gerak.PUDAR, 0, Gerak.Keluar) })

/**
 * Item daftar yang ikut gerak: masuk-keluar saat ganti tab ([masukTab]), terbelah saat layar
 * penuh dibuka ([terbelah]), meluncur ke tempat baru saat isi daftar berubah, dan memudar
 * masuk/keluar saat ditambah/dihapus.
 */
fun LazyListScope.itemTab(key: Any, isi: @Composable LazyItemScope.() -> Unit) {
    item(key = key) {
        val lingkup = this
        Box(Modifier.gerakItem(lingkup).masukTab().terbelah(key)) { lingkup.isi() }
    }
}

/** Versi banyak item dari [itemTab]. */
fun <T> LazyListScope.itemsTab(isi: List<T>, key: (T) -> Any, konten: @Composable LazyItemScope.(T) -> Unit) {
    items(isi, key = key) { t ->
        val lingkup = this
        Box(Modifier.gerakItem(lingkup).masukTab().terbelah(key(t))) { lingkup.konten(t) }
    }
}

private fun Modifier.gerakItem(lingkup: LazyItemScope): Modifier = with(lingkup) {
    this@gerakItem.animateItem(
        fadeInSpec = gtween(Gerak.ISI_MASUK, easing = LinearOutSlowInEasing),
        placementSpec = gtween(Gerak.PINDAH, easing = Gerak.Pindah),
        fadeOutSpec = gtween(Gerak.PUDAR, easing = Gerak.Keluar),
    )
}

/**
 * Item daftar di bawah layar penuh: terbelah menjauh dari kartu yang diketuk (yang di atas naik,
 * yang di bawah turun 14 + 6dp per jarak, maks 3, jeda 20 ms per jarak) sambil memudar, lalu
 * memantul kembali dari arah yang sama saat layar penuh ditutup (mulai 120 ms, jeda 25 ms).
 * Kalau layar penuh tidak dibuka dari kartu, item hanya memudar di tempat.
 */
@Composable
fun Modifier.terbelah(kunci: Any): Modifier {
    val t = LocalTerbang.current ?: return this
    val av = lingkupLayar() ?: return this
    val px = LocalDensity.current.density
    // Letak item ini sendiri: kunci yang sama bisa dipakai item di tab lain yang tetap tersusun.
    val letak = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val tengah = remember { floatArrayOf(Float.NaN) }
    val catat = this.onPlaced { letak[0] = it }
    if (kunci == t.id) return catat
    fun jarak(): Pair<Float, Int> {
        val dari = t.id?.let { t.tengahAsal() } ?: return 0f to 0
        letak[0]?.takeIf { it.isAttached }?.let { tengah[0] = it.positionInRoot().y + it.size.height / 2f }
        val aku = tengah[0].takeIf { !it.isNaN() } ?: return 0f to 0
        val d = aku - dari
        val jauh = (abs(d) / (80 * px)).roundToInt().coerceIn(1, 3)
        return sign(d) * (14f + 6f * jauh) to jauh
    }
    return catat.gerakLingkup(
        av,
        // Kembali dari detail (kartu terbang pulang): mulai 120 ms + 25 ms per jarak. Dari layar lain: segera.
        masuk = { val (dy, j) = jarak(); Masuk(0f, dy, 1f, Gerak.ISI_MASUK, if (t.id == null) 40 else 120 + j * 25, Gerak.Pegas) },
        keluar = { val (dy, j) = jarak(); Keluar(0f, dy, Gerak.PINDAH, j * 20, Gerak.Pindah) },
    )
}

/** Bagian layar daftar di luar item (tab bar, tombol +): memudar saat layar penuh dibuka, muncul lagi saat ditutup. */
@Composable
fun Modifier.ikutLayar(): Modifier {
    val t = LocalTerbang.current ?: return this
    val av = lingkupLayar() ?: return this
    return gerakLingkup(
        av,
        masuk = { Masuk(0f, 0f, 1f, Gerak.ISI_MASUK / 2, if (t.id == null) 40 else 120, LinearOutSlowInEasing) },
        keluar = { Keluar(0f, 0f, Gerak.PINDAH, 0, Gerak.Pindah) },
    )
}

/**
 * Isi layar penuh: melompat masuk berurutan (dari bawah 32dp, skala 0,92) setelah bidangnya
 * melebar (mulai 170 ms, jeda 55 ms), dan memudar turun 14dp saat layar ditutup (jeda 8 ms).
 */
@Composable
fun Modifier.isiLayar(urutan: Int, mulai: Int = 170): Modifier {
    val av = lingkupLayar() ?: return this
    val u = urutan.coerceAtMost(Gerak.MAKS_BERJEDA)
    return gerakLingkup(
        av,
        masuk = { Masuk(0f, 32f, 0.92f, Gerak.ISI_MASUK, mulai + u * Gerak.JEDA_ISI, Gerak.Pegas) },
        keluar = { Keluar(0f, 14f, Gerak.PUDAR, u * 8, Gerak.Keluar) },
    )
}

/** Latar layar penuh tanpa bidang asal: memudar masuk, dan memudar keluar saat ditutup. */
@Composable
fun Modifier.latarDatang(): Modifier {
    val av = lingkupLayar() ?: return this
    return gerakLingkup(
        av,
        masuk = { Masuk(0f, 0f, 1f, Gerak.PUDAR, 0, LinearEasing) },
        keluar = { Keluar(0f, 0f, Gerak.PUDAR, 40, Gerak.Keluar) },
    )
}

/**
 * Keluar tanpa memudar: layar/tab lama tetap tersusun cukup lama untuk gerak milik sendiri
 * (terbelah, kartu terbang yang kembali), lalu dilepas.
 */
fun tahanSampaiSelesai(ms: Int = Gerak.MULUS + 460): ExitTransition = fadeOut(snap(delayMillis = Gerak.ms(ms)))

/**
 * Layar penuh: latarnya melebar dari tombol yang diketuk ([kunciAsal], tombol bertanda
 * [asalTombol]) lalu isinya melompat masuk; saat ditutup isinya memudar turun dan bidangnya
 * menyusut kembali ke tombol. Tanpa tombol asal, latarnya hanya memudar masuk/keluar.
 */
@Composable
fun LayarPenuh(kunciAsal: Any?, modifier: Modifier = Modifier, isi: @Composable BoxScope.() -> Unit) {
    val t = LocalTerbang.current
    val av = lingkupLayar()
    val dariTombol = remember { kunciAsal != null && t != null && av != null && av.sedangMasuk() && t.siapkanTombol(kunciAsal) }
    // Sejak mulai ditutup, latar layar tetap tersembunyi sampai layarnya dilepas.
    val tertutup = remember { mutableStateOf(false) }
    if (av != null) {
        LaunchedEffect(av) {
            snapshotFlow { av.targetState }.collect {
                if (it == EnterExitState.PostExit) {
                    tertutup.value = true
                    if (dariTombol) t!!.tutupLayar(kunciAsal!!)
                }
            }
        }
    }
    Box(modifier.fillMaxSize()) {
        val latar = if (dariTombol) {
            Modifier.graphicsLayer { alpha = if (tertutup.value || (t!!.kunciTombol == kunciAsal && !t.tombolMendarat)) 0f else 1f }
        } else {
            Modifier.latarDatang()
        }
        Box(Modifier.matchParentSize().then(latar).latarLayar())
        if (dariTombol) TombolMelebar(kunciAsal!!)
        // Dari tombol: isi mulai masuk lebih awal (100 ms), menyambung dengan bidang yang melebar.
        Box(Modifier.fillMaxSize().isiLayar(0, mulai = if (dariTombol) 100 else 170), content = isi)
    }
}

/**
 * Kotak yang muncul saat layar sudah tampil (pita, kotak gagal/ditahan, keadaan kosong): ruangnya
 * membuka (`pindah`) sementara isinya melompat masuk dari atas 12dp, skala 0,92 (`isiMasuk`).
 * [gerak] false = langsung tampil (misalnya saat layar baru dibuka). Ukuran yang berubah
 * sesudahnya (isi pita berganti) ikut memanjang/memendek dengan mulus.
 */
@Composable
fun Modifier.munculKotak(gerak: Boolean): Modifier {
    val px = LocalDensity.current.density
    val ruang = remember { Animatable(if (gerak) 0f else 1f) }
    val isi = remember { Animatable(if (gerak) 0f else 1f) }
    if (gerak) {
        LaunchedEffect(Unit) {
            launch { ruang.animateTo(1f, gtween(Gerak.PINDAH, easing = Gerak.Pindah)) }
            isi.animateTo(1f, gtween(Gerak.ISI_MASUK, easing = Gerak.Pegas))
        }
    }
    return this
        // Selama ruangnya membuka, isi yang belum muat dipotong supaya tidak menimpa item di bawahnya.
        .drawWithContent { if (ruang.value < 1f) clipRect { this@drawWithContent.drawContent() } else drawContent() }
        .layout { m, c ->
            val pl = m.measure(c)
            val tinggi = (pl.height * ruang.value).roundToInt()
            layout(pl.width, tinggi) { pl.place(0, 0) }
        }
        .animateContentSize(gtween(Gerak.PINDAH, easing = Gerak.Pindah))
        .graphicsLayer {
            val v = isi.value
            translationY = (1f - v) * -12f * px
            val sk = 0.92f + 0.08f * v
            scaleX = sk
            scaleY = sk
            alpha = v.coerceIn(0f, 1f)
        }
}
