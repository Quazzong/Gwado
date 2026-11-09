package io.github.quazzong.gwado.ui.komponen

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.ui.tema.Gerak
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.ui.tema.gtween
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/** Bidang terbang: batas luar (termasuk ruang bayangan) dan batas judul, dalam piksel layar. */
internal class Bidang(val luar: Rect, val judul: Rect)

/**
 * Keadaan "kartu terbang" (kartu tugas ↔ kepala detail) dan "tombol melebar" (tombol ↔ layar penuh).
 * Gerak memakai [Animatable] sendiri yang baru dimulai setelah layar tujuan selesai ditata, jadi
 * frame pertama yang berat (menyusun layar baru) tidak memotong awal gerak.
 */
class Terbang(internal val cakupan: CoroutineScope) {
    // --- kartu ↔ kepala detail ---
    /** Id tugas yang sedang dibuka/ditutup lewat kartunya, null kalau tidak ada. */
    var id by mutableStateOf<String?>(null)
        internal set
    /** Kepala detail asli disembunyikan (menunggu kartu terbang datang / sedang pergi). */
    var tahanKepala by mutableStateOf(false)
        internal set
    /** Kartu terbang tampil (kartu asli di daftar disembunyikan). */
    var aktif by mutableStateOf(false)
        internal set
    internal val p = Animatable(0f)
    internal var kartu: Bidang? = null
    internal var kepala: Bidang? = null
    internal var baris: BarisTugas? = null
    internal var warnaKartu: Color = Color.White
    internal var bayanganKartu: Dp = Jarak.bayangan
    internal val letakKartu = mutableMapOf<String, LayoutCoordinates>()
    internal val letakJudul = mutableMapOf<String, LayoutCoordinates>()
    internal val barisKartu = mutableMapOf<String, BarisTugas>()
    private var nomor = 0

    // --- posisi item daftar (untuk terbelah) ---
    internal val letak = mutableMapOf<Any, LayoutCoordinates>()
    internal val tengah = mutableMapOf<Any, Float>()

    internal fun tengahDari(kunci: Any): Float? {
        val c = letak[kunci]?.takeIf { it.isAttached } ?: return tengah[kunci]
        return (c.positionInRoot().y + c.size.height / 2f).also { tengah[kunci] = it }
    }

    /** Dipanggil saat kartu diketuk (sebelum detail dibuka): catat posisi diam kartu itu. */
    fun siapkan(idTugas: String) {
        val c = letakKartu[idTugas]?.takeIf { it.isAttached }
        val j = letakJudul[idTugas]?.takeIf { it.isAttached }
        if (c == null || j == null) {
            id = null
            return
        }
        id = idTugas
        kartu = Bidang(c.boundsInRoot(), j.boundsInRoot())
        kepala = null
        baris = barisKartu[idTugas]
        tahanKepala = true
        tengahDari(idTugas)
        // Pengaman: kalau kepala detail tidak pernah tertata (misalnya detail gagal dimuat), tampilkan biasa.
        cakupan.launch {
            delay(800)
            if (id == idTugas && kepala == null) tahanKepala = false
        }
    }

    /** Layar lain dibuka (bukan dari kartu): daftar tidak terbelah dari kartu mana pun. */
    fun lupakan() {
        if (!aktif) {
            id = null
            tahanKepala = false
        }
    }

    /** Kepala detail sudah ditata: mulai terbang dari kartu ke kepala. */
    internal fun kepalaSiap(idTugas: String, luar: Rect, judul: Rect) {
        if (id != idTugas || kartu == null || kepala != null) return
        kepala = Bidang(luar, judul)
        val n = ++nomor
        cakupan.launch {
            p.snapTo(0f)
            aktif = true
            p.animateTo(1f, gtween(Gerak.MULUS, easing = Gerak.Mulus))
            if (n == nomor) {
                aktif = false
                tahanKepala = false
            }
        }
    }

    /** Detail ditutup: kepala menyusut kembali ke posisi diam kartu di daftar. */
    internal fun tutup(idTugas: String) {
        if (id != idTugas || kartu == null || kepala == null) return
        tahanKepala = true
        aktif = true
        val n = ++nomor
        cakupan.launch {
            // Tunggu daftar tersusun lagi, lalu pakai posisi kartu yang sekarang (posisi diam).
            withFrameNanos { }
            letakKartu[idTugas]?.takeIf { it.isAttached }?.let { c ->
                val j = letakJudul[idTugas]?.takeIf { it.isAttached }
                kartu = Bidang(c.boundsInRoot(), j?.boundsInRoot() ?: kartu!!.judul)
            }
            p.animateTo(0f, gtween(Gerak.TUTUP, 40, Gerak.Mulus))
            if (n == nomor) {
                aktif = false
                tahanKepala = false
            }
        }
    }

    // --- tombol ↔ layar penuh ---
    var kunciTombol by mutableStateOf<Any?>(null)
        internal set
    var aktifTombol by mutableStateOf(false)
        internal set
    /** Latar layar penuh sudah selesai melebar (latar asli layar boleh tampil). */
    var tombolMendarat by mutableStateOf(false)
        internal set
    internal val pt = Animatable(0f)
    internal var tombol: Rect? = null
    internal var layar: Rect? = null
    internal val letakTombol = mutableMapOf<Any, LayoutCoordinates>()
    private var nomorTombol = 0

    /** Layar penuh dari tombol [kunci] mulai tersusun: catat posisi tombol (kalau tombolnya ada). */
    internal fun siapkanTombol(kunci: Any): Boolean {
        val c = letakTombol[kunci]?.takeIf { it.isAttached } ?: return false
        kunciTombol = kunci
        tombol = c.boundsInRoot()
        layar = null
        tombolMendarat = false
        // Pengaman: kalau layarnya tidak pernah tertata, tampilkan latar asli.
        cakupan.launch {
            delay(800)
            if (kunciTombol == kunci && layar == null) tombolMendarat = true
        }
        return true
    }

    internal fun layarSiap(kunci: Any, r: Rect) {
        if (kunciTombol != kunci || layar != null) return
        layar = r
        val n = ++nomorTombol
        cakupan.launch {
            pt.snapTo(0f)
            aktifTombol = true
            pt.animateTo(1f, gtween(Gerak.MULUS, easing = Gerak.Mulus))
            if (n == nomorTombol) {
                tombolMendarat = true
                aktifTombol = false
            }
        }
    }

    internal fun tutupLayar(kunci: Any) {
        if (kunciTombol != kunci || layar == null) return
        tombolMendarat = false
        aktifTombol = true
        val n = ++nomorTombol
        cakupan.launch {
            withFrameNanos { }
            letakTombol[kunci]?.takeIf { it.isAttached }?.let { tombol = it.boundsInRoot() }
            pt.animateTo(0f, gtween(Gerak.TUTUP, 40, Gerak.Mulus))
            if (n == nomorTombol) {
                aktifTombol = false
                kunciTombol = null
            }
        }
    }
}

val LocalTerbang = staticCompositionLocalOf<Terbang?> { null }

/** Kartu tugas di daftar: catat posisinya (posisi diam, sebelum geseran tekan) dan sembunyikan saat terbang. */
@Composable
internal fun Modifier.asalKartu(baris: BarisTugas, warna: Color, bayangan: Dp): Modifier {
    val t = LocalTerbang.current ?: return this
    t.barisKartu[baris.id] = baris
    return this
        .onPlaced { c ->
            t.letakKartu[baris.id] = c
            if (t.id == baris.id) {
                t.warnaKartu = warna
                t.bayanganKartu = bayangan
            }
        }
        .graphicsLayer { alpha = if (t.aktif && t.id == baris.id) 0f else 1f }
}

/** Judul kartu tugas: catat posisinya untuk judul yang ikut terbang. */
@Composable
internal fun Modifier.judulKartu(id: String): Modifier {
    val t = LocalTerbang.current ?: return this
    return onPlaced { t.letakJudul[id] = it }
}

/** Letak kepala detail dan judulnya; penerbangan mulai begitu keduanya sudah tertata. */
class LetakKepala {
    internal var luar: LayoutCoordinates? = null
    internal var judul: LayoutCoordinates? = null

    internal fun coba(t: Terbang, id: String) {
        val l = luar?.takeIf { it.isAttached } ?: return
        val j = judul?.takeIf { it.isAttached } ?: return
        t.kepalaSiap(id, l.boundsInRoot(), j.boundsInRoot())
    }
}

/** Kepala detail: sembunyikan selama kartu terbang; setelah ditata (bersama judulnya), mulai terbang ke sini. */
@Composable
internal fun Modifier.tujuanKepala(id: String, letak: LetakKepala): Modifier {
    val t = LocalTerbang.current ?: return this
    return this
        .onPlaced { c ->
            letak.luar = c
            letak.coba(t, id)
        }
        .graphicsLayer { alpha = if (t.tahanKepala && t.id == id) 0f else 1f }
}

/** Judul kepala detail (lihat [tujuanKepala]). */
@Composable
internal fun Modifier.judulKepala(id: String, letak: LetakKepala): Modifier {
    val t = LocalTerbang.current ?: return this
    return onPlaced { c ->
        letak.judul = c
        letak.coba(t, id)
    }
}

private fun lerpRect(a: Rect, b: Rect, f: Float) = Rect(
    a.left + (b.left - a.left) * f,
    a.top + (b.top - a.top) * f,
    a.right + (b.right - a.right) * f,
    a.bottom + (b.bottom - a.bottom) * f,
)

private fun bagian(p: Float, dari: Float, ke: Float) = ((p - dari) / (ke - dari)).coerceIn(0f, 1f)

/**
 * Kartu terbang di dalam layar detail (di bawah isinya): dari kartu ke kepala (p 0 → 1) dan
 * kembali. Dasar warna kartu selalu buram; gradasi kepala muncul di p 0,1–0,5; wajah kartu
 * (tanpa judul) memudar di p 0–0,4; wajah kepala (tanpa judul) ikut gradasi; dua versi judul
 * menempel di dalam kartu dan saling memudar di p 0,1–0,45, posisinya sampai di tujuan saat p 0,5.
 */
@Composable
internal fun KartuTerbang(
    id: String,
    wajahKartu: @Composable (BarisTugas) -> Unit,
    wajahKepala: @Composable () -> Unit,
) {
    val t = LocalTerbang.current ?: return
    if (!(t.aktif && t.id == id)) return
    val k = t.kartu ?: return
    val h = t.kepala ?: return
    val baris = t.baris
    val w = GwadoTema.warna
    val d = LocalDensity.current
    val strip = remember(w) { gradasiCss(115f, w.strip) }
    val asal = remember { floatArrayOf(0f, 0f) }
    val padK = with(d) { t.bayanganKartu.toPx() }
    val padH = with(d) { 6.dp.toPx() }
    val g = with(d) { Jarak.garis.toPx() }
    val r14 = with(d) { 14.dp.toPx() }
    val r16 = with(d) { 16.dp.toPx() }
    val b3 = with(d) { Jarak.bayangan.toPx() }
    val fJudul = Huruf.judulKartu.fontSize.value / Huruf.judulDetail.fontSize.value
    Box(Modifier.fillMaxSize().onPlaced { val o = it.positionInRoot(); asal[0] = o.x; asal[1] = o.y }) {
        Box(
            Modifier
                .layout { m, _ ->
                    val r = lerpRect(k.luar, h.luar, t.p.value)
                    val pl = m.measure(Constraints.fixed(max(1, r.width.roundToInt()), max(1, r.height.roundToInt())))
                    layout(pl.width, pl.height) { pl.place((r.left - asal[0]).roundToInt(), (r.top - asal[1]).roundToInt()) }
                }
                .drawBehind {
                    val p = t.p.value
                    val pad = padK + (padH - padK) * p
                    val badan = Size(size.width - pad, size.height - pad)
                    val rad = CornerRadius(r14 + (r16 - r14) * p)
                    if (p > 0f) drawRoundRect(w.lavender, Offset(pad, pad), badan, rad, alpha = p)
                    val bb = if (padK > 0f) b3 else b3 * p
                    if (bb > 0f) drawRoundRect(w.bayangan, Offset(bb, bb), badan, rad)
                    drawRoundRect(t.warnaKartu, Offset.Zero, badan, rad)
                    drawRoundRect(strip, Offset.Zero, badan, rad, alpha = bagian(p, 0.1f, 0.5f))
                },
        ) {
            val bentuk = remember {
                GenericShape { s, _ ->
                    val p = t.p.value
                    val pad = padK + (padH - padK) * p
                    val rad = r14 + (r16 - r14) * p
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, s.width - pad, s.height - pad, CornerRadius(rad)))
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(bentuk)
                    .drawWithContent {
                        drawContent()
                        val p = t.p.value
                        val pad = padK + (padH - padK) * p
                        val rad = CornerRadius(r14 + (r16 - r14) * p)
                        val badan = Size(size.width - pad, size.height - pad)
                        drawRoundRect(w.kilau, Offset(g * 1.5f, g * 1.5f), Size(badan.width - g * 3, badan.height - g * 3), rad, style = Stroke(g))
                        drawRoundRect(w.garis, Offset(g / 2, g / 2), Size(badan.width - g, badan.height - g), rad, style = Stroke(g))
                    },
            ) {
                if (baris != null) {
                    Box(
                        Modifier
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(with(d) { k.luar.width.toDp() }, with(d) { k.luar.height.toDp() })
                            .graphicsLayer { alpha = 1f - bagian(t.p.value, 0f, 0.4f) },
                    ) { wajahKartu(baris) }
                }
                Box(
                    Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(with(d) { h.luar.width.toDp() }, with(d) { h.luar.height.toDp() })
                        .graphicsLayer { alpha = bagian(t.p.value, 0.1f, 0.5f) },
                ) { wajahKepala() }
                // Judul: posisi relatif di dalam kartu, sampai di tujuan saat p = 0,5 lalu menempel.
                val jk = Offset(k.judul.left - k.luar.left, k.judul.top - k.luar.top)
                val jh = Offset(h.judul.left - h.luar.left, h.judul.top - h.luar.top)
                val judul = baris?.judul.orEmpty()
                Text(
                    judul,
                    style = Huruf.judulKartu,
                    color = w.tinta,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(with(d) { k.judul.width.toDp() }, with(d) { k.judul.height.toDp() })
                        .graphicsLayer {
                            val p = t.p.value
                            val q = bagian(p, 0f, 0.5f)
                            val sk = 1f + (1f / fJudul - 1f) * q
                            val ty = jh.y + h.judul.height / 2f - k.judul.height * (1f / fJudul) / 2f
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                            translationX = jk.x + (jh.x - jk.x) * q
                            translationY = jk.y + (ty - jk.y) * q
                            scaleX = sk
                            scaleY = sk
                            alpha = 1f - bagian(p, 0.1f, 0.45f)
                        },
                )
                Text(
                    judul,
                    style = Huruf.judulDetail,
                    color = w.tinta,
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(with(d) { h.judul.width.toDp() }, with(d) { h.judul.height.toDp() })
                        .graphicsLayer {
                            val p = t.p.value
                            val q = bagian(p, 0f, 0.5f)
                            val sk = fJudul + (1f - fJudul) * q
                            val ty0 = jk.y + k.judul.height / 2f - h.judul.height * fJudul / 2f
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                            translationX = jk.x + (jh.x - jk.x) * q
                            translationY = ty0 + (jh.y - ty0) * q
                            scaleX = sk
                            scaleY = sk
                            alpha = bagian(p, 0.1f, 0.45f)
                        },
                )
            }
        }
    }
}

/** Tombol asal layar penuh: catat posisinya dan sembunyikan selama latar layar melebar/menyusut. */
@Composable
fun Modifier.asalTombol(kunci: Any): Modifier {
    val t = LocalTerbang.current ?: return this
    return this
        .onPlaced { t.letakTombol[kunci] = it }
        .graphicsLayer { alpha = if (t.aktifTombol && t.kunciTombol == kunci) 0f else 1f }
}

/**
 * Bidang yang melebar dari tombol [kunci] menjadi layar penuh (pt 0 → 1) dan kembali:
 * warnanya dari plum (tombol) ke putih kartu, sudut dari 14dp ke 0, lalu memudar di paruh akhir
 * sementara isi layar melompat masuk; latar layar sama dengan latar di bawahnya, jadi tanpa sambungan.
 */
@Composable
internal fun TombolMelebar(kunci: Any) {
    val t = LocalTerbang.current ?: return
    val w = GwadoTema.warna
    val d = LocalDensity.current
    val r14 = with(d) { 14.dp.toPx() }
    val asal = remember { floatArrayOf(0f, 0f) }
    Box(
        Modifier.fillMaxSize().onPlaced { c ->
            val o = c.positionInRoot()
            asal[0] = o.x
            asal[1] = o.y
            if (c.isAttached) t.layarSiap(kunci, c.boundsInRoot())
        },
    ) {
        if (!(t.aktifTombol && t.kunciTombol == kunci)) return@Box
        val a = t.tombol ?: return@Box
        val b = t.layar ?: return@Box
        Box(
            Modifier
                .layout { m, _ ->
                    val r = lerpRect(a, b, t.pt.value)
                    val pl = m.measure(Constraints.fixed(max(1, r.width.roundToInt()), max(1, r.height.roundToInt())))
                    layout(pl.width, pl.height) { pl.place(IntOffset((r.left - asal[0]).roundToInt(), (r.top - asal[1]).roundToInt())) }
                }
                .drawBehind {
                    val p = t.pt.value
                    val warna = lerp(w.plum, w.kartu, bagian(p, 0f, 0.35f))
                    drawRoundRect(warna, cornerRadius = CornerRadius(r14 * (1f - p)), alpha = 1f - bagian(p, 0.5f, 1f))
                },
        )
    }
}
