package io.github.quazzong.gwado.tampilan

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Token warna satu tema sebagai ARGB. Kunci = nama token tampilan (`latar`, `aksen`, …);
 * `strip0`–`strip2` = tiga titik gradasi strip judul.
 */
typealias Token = Map<String, Int>

/** Posisi titik gradasi strip judul (0..1) untuk tema terang dan gelap. */
val POSISI_STRIP_TERANG = listOf(0f, 0.45f, 1f)
val POSISI_STRIP_GELAP = listOf(0f, 0.5f, 1f)

private fun token(vararg isi: Pair<String, Long>): Token = isi.associate { (k, v) -> k to v.toInt() }

/** Palet bawaan "Permen", tema terang. */
val PERMEN_TERANG: Token = token(
    "latar" to 0xFFFBEFF4, "kartu" to 0xFFFFFFFF, "ubin" to 0xFFF1ECFA, "tinta" to 0xFF3A1D36,
    "tintaRedup" to 0xFF7A5872, "garis" to 0xFF4A2545, "bayangan" to 0xFF4A2545, "garisHalus" to 0xFFF1E4EE,
    "kilau" to 0xF2FFFFFF, "aksen" to 0xFFF7A8D8, "aksenTerang" to 0xFFFFCDEA, "aksenGelap" to 0xFFEA8BC5,
    "diAtasAksen" to 0xFF3A1D36, "lavender" to 0xFFAEA5D6, "plum" to 0xFF4A2545, "diAtasPlum" to 0xFFFBEFF4,
    "strip0" to 0xFFFAB9DF, "strip1" to 0xFFF7A8D8, "strip2" to 0xFFDDB4EC,
    "stiker" to 0xFF4A2545, "garisStiker" to 0xFFFFFFFF, "taburan" to 0x9EFFFFFF, "pola" to 0x264A2545,
    "galat" to 0xFFC8303B, "minggu" to 0xFFC8303B, "luarBulan" to 0xFF9A8296, "ikonLayanan" to 0xFFF1ECFA,
    "latarLewat" to 0xFFFFE4E7, "latarMepet" to 0xFFFFF4D3,
)

/** Palet bawaan "Permen", tema gelap. */
val PERMEN_GELAP: Token = token(
    "latar" to 0xFF1C1220, "kartu" to 0xFF2A1B30, "ubin" to 0xFF33233B, "tinta" to 0xFFF6E9F2,
    "tintaRedup" to 0xFFC4ABBF, "garis" to 0xFFEBC2E0, "bayangan" to 0xFFEBC2E0, "garisHalus" to 0xFF3A2842,
    "kilau" to 0x17FFFFFF, "aksen" to 0xFFF7A8D8, "aksenTerang" to 0xFFFFCDEA, "aksenGelap" to 0xFFDE84BC,
    "diAtasAksen" to 0xFF2A1B30, "lavender" to 0xFF7D72B8, "plum" to 0xFFEBC2E0, "diAtasPlum" to 0xFF2A1B30,
    "strip0" to 0xFF7A3A6F, "strip1" to 0xFF5A2C5E, "strip2" to 0xFF3E2A5C,
    "stiker" to 0xFFFBD3EC, "garisStiker" to 0xFF2A1B30, "taburan" to 0x66F7A8D8, "pola" to 0x1FEBC2E0,
    "galat" to 0xFFFF8A8F, "minggu" to 0xFFFF8A8F, "luarBulan" to 0xFF9A84A0, "ikonLayanan" to 0xFF3A2842,
    "latarLewat" to 0xFF3D1B26, "latarMepet" to 0xFF3A2F1C,
)

private fun permen(gelap: Boolean) = if (gelap) PERMEN_GELAP else PERMEN_TERANG

/** Contoh warna di mode Simpel; [benih] = warna aksen persis. */
data class ContohWarna(val kode: String, val label: String, val benih: Int)

val CONTOH_WARNA = listOf(
    ContohWarna("permen", "Permen", 0xFFF7A8D8.toInt()),
    ContohWarna("lavender", "Lavender", 0xFFB9A8F0.toInt()),
    ContohWarna("langit", "Langit", 0xFF98C6F4.toInt()),
    ContohWarna("mint", "Mint", 0xFF8FDDBB.toInt()),
    ContohWarna("lemon", "Lemon", 0xFFF1DA74.toInt()),
    ContohWarna("jeruk", "Jeruk", 0xFFFFB68A.toInt()),
    ContohWarna("koral", "Koral", 0xFFFF9F9F.toInt()),
    ContohWarna("abu", "Abu", 0xFFC8C0C6.toInt()),
)

/** Kelompok warna mode Lanjutan (kunci token, label). */
val KELOMPOK_WARNA = listOf(
    "latar" to "Latar layar", "kartu" to "Kartu dan panel", "tinta" to "Teks", "garis" to "Garis dan bayangan",
    "aksen" to "Aksen", "lavender" to "Aksen kedua", "plum" to "Tombol gelap",
)

/** Satu pasangan warna yang diuji kontrasnya: [depan] di atas [belakang], minimal [min] : 1. */
data class UjiKontras(val label: String, val depan: String, val belakang: String, val min: Double)

val UJI_KONTRAS = listOf(
    UjiKontras("Teks di latar", "tinta", "latar", 4.5),
    UjiKontras("Teks di kartu lewat", "tinta", "latarLewat", 4.5),
    UjiKontras("Teks di kartu mepet", "tinta", "latarMepet", 4.5),
    UjiKontras("Teks di kartu", "tinta", "kartu", 4.5),
    UjiKontras("Teks kedua di kartu", "tintaRedup", "kartu", 4.5),
    UjiKontras("Teks di tombol utama", "diAtasAksen", "aksen", 4.5),
    UjiKontras("Teks di tombol gelap", "diAtasPlum", "plum", 4.5),
    UjiKontras("Judul di strip", "stiker", "strip1", 4.5),
    UjiKontras("Garis di latar", "garis", "latar", 3.0),
)

/** Hasil satu uji kontras. */
data class HasilKontras(val uji: UjiKontras, val rasio: Double) {
    val lolos get() = rasio >= uji.min
}

fun ujiKontras(t: Token): List<HasilKontras> = UJI_KONTRAS.map { HasilKontras(it, kontras(t.getValue(it.depan), t.getValue(it.belakang))) }

// ---------- Ruang warna: sRGB ↔ OKLab/OKLCH ----------

private fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
private fun gam(c: Double) = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055
private fun saluran(argb: Int) = doubleArrayOf(((argb shr 16) and 0xFF) / 255.0, ((argb shr 8) and 0xFF) / 255.0, (argb and 0xFF) / 255.0)
private fun keArgb(rgb: DoubleArray): Int {
    val (r, g, b) = rgb.map { (min(1.0, max(0.0, it)) * 255).roundToInt() }
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

private fun keLab(argb: Int): DoubleArray {
    val (r, g, b) = saluran(argb).map(::lin)
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    return doubleArrayOf(
        0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
    )
}

private fun dariLab(lL: Double, a: Double, b: Double): DoubleArray {
    val l = (lL + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m = (lL - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s = (lL - 0.0894841775 * a - 1.291485548 * b).pow(3)
    return doubleArrayOf(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
    ).map(::gam).toDoubleArray()
}

/** Kecerahan, kepekatan, rona (derajat) OKLCH. */
data class Lch(val l: Double, val c: Double, val h: Double)

fun keLch(argb: Int): Lch {
    val (l, a, b) = keLab(argb)
    return Lch(l, hypot(a, b), (atan2(b, a) * 180 / PI + 360) % 360)
}

/** Warna OKLCH ke ARGB; kepekatan dipotong sampai masuk sRGB (rona dan kecerahan tetap). */
fun dariLch(l: Double, c: Double, h: Double): Int {
    val co = cos(h * PI / 180)
    val si = sin(h * PI / 180)
    fun ok(rgb: DoubleArray) = rgb.all { it >= -1e-4 && it <= 1 + 1e-4 }
    val rgb = dariLab(l, c * co, c * si)
    if (ok(rgb)) return keArgb(rgb)
    var lo = 0.0
    var hi = c
    repeat(22) {
        val m = (lo + hi) / 2
        if (ok(dariLab(l, m * co, m * si))) lo = m else hi = m
    }
    return keArgb(dariLab(l, lo * co, lo * si))
}

fun dariLch(w: Lch) = dariLch(w.l, w.c, w.h)

fun luminans(argb: Int): Double {
    val (r, g, b) = saluran(argb).map(::lin)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** Rasio kontras WCAG antara dua warna (alfa diabaikan). */
fun kontras(a: Int, b: Int): Double {
    val x = luminans(a)
    val y = luminans(b)
    return (max(x, y) + 0.05) / (min(x, y) + 0.05)
}

/** Campuran [a] → [b] sebanyak [t] di sRGB. */
fun campur(a: Int, b: Int, t: Double): Int {
    val x = saluran(a)
    val y = saluran(b)
    return keArgb(DoubleArray(3) { x[it] + (y[it] - x[it]) * t })
}

private fun denganAlfa(argb: Int, alfa: Double) = ((alfa * 255).roundToInt() shl 24) or (argb and 0xFFFFFF)

private const val PUTIH = 0xFFFFFFFF.toInt()

// ---------- Mode Simpel ----------

/**
 * Resep palet, diukur dari palet Permen (benih #F7A8D8): per token kecerahan (atau selisih dari
 * kecerahan benih kalau [relatif]), kepekatan × kepekatan benih, geser rona (negatif = ke arah ungu,
 * seperti pink → plum; positif = menjauh).
 */
private data class Resep(val l: Double, val c: Double, val d: Double, val relatif: Boolean = false)

private val RESEP_TERANG = mapOf(
    "latar" to Resep(0.963, 0.13, 7.0), "kartu" to Resep(1.0, 0.0, 0.0), "ubin" to Resep(0.951, 0.18, -40.0),
    "tinta" to Resep(0.281, 0.54, -10.0), "tintaRedup" to Resep(0.505, 0.53, -8.0), "garis" to Resep(0.327, 0.67, -10.0),
    "garisHalus" to Resep(0.932, 0.17, -9.0), "lavender" to Resep(0.746, 0.64, -50.0),
    "aksenTerang" to Resep(0.078, 0.61, 0.0, true), "aksenGelap" to Resep(-0.068, 1.22, 1.0, true),
    "luarBulan" to Resep(0.635, 0.37, -11.0),
    "strip0" to Resep(0.034, 0.81, 0.0, true), "strip2" to Resep(0.002, 0.8, -26.0, true),
)

private val RESEP_GELAP = mapOf(
    "latar" to Resep(0.202, 0.28, -27.0), "kartu" to Resep(0.251, 0.4, -27.0), "ubin" to Resep(0.286, 0.43, -29.0),
    "tinta" to Resep(0.947, 0.17, -6.0), "tintaRedup" to Resep(0.769, 0.36, -10.0), "garis" to Resep(0.858, 0.55, -7.0),
    "garisHalus" to Resep(0.31, 0.46, -28.0), "lavender" to Resep(0.591, 0.96, -53.0),
    "aksenTerang" to Resep(0.078, 0.61, 0.0, true), "aksenGelap" to Resep(-0.096, 1.18, 0.0, true),
    "luarBulan" to Resep(0.643, 0.44, -24.0), "stiker" to Resep(0.908, 0.5, -2.0),
    "strip0" to Resep(0.451, 1.03, -8.0), "strip1" to Resep(0.375, 0.89, -18.0), "strip2" to Resep(0.334, 0.79, -42.0),
)

/** Kepekatan aksen Permen; benih yang cukup berwarna diperlakukan minimal sepekat ini. */
private const val ACUAN_C = 0.11

/** Selisih rona terpendek dari [h] ke [tujuan], −180..180. */
private fun selisihRona(h: Double, tujuan: Double) = ((tujuan - h + 540) % 360) - 180

/** Geser rona [h] sejauh |d|: d < 0 ke arah ungu (290°), d > 0 menjauh; benih keunguan memakai pink (350°). */
private fun geserResep(h: Double, d: Double): Double {
    val tujuan = if (h in 250.0..330.0) 350.0 else 290.0
    val arah = if (selisihRona(h, tujuan) >= 0) 1 else -1
    return (h + (if (d < 0) arah else -arah) * abs(d) + 360) % 360
}

/** Nada gelap kuning-hijau jadi zaitun kalau hanya digelapkan; geser tambahan ke oranye (cokelat madu). */
private fun tambahGelap(h: Double): Double {
    val jarak = abs(selisihRona(h, 100.0))
    return if (jarak >= 45) 0.0 else 22 * cos(jarak / 45 * PI / 2)
}

/**
 * Seberapa cocok rona [h] untuk nada gelap (teks, garis): 1 untuk ungu–biru–merah (260°–20°),
 * 0 untuk oranye–kuning–hijau (60°–220°) yang nada gelapnya jadi cokelat/zaitun/hijau tua.
 */
private fun cocokGelap(h: Double): Double {
    val d = abs(selisihRona(h, 320.0))
    return when {
        d <= 60 -> 1.0
        d >= 100 -> 0.0
        else -> (100 - d) / 40
    }
}

/** Calon berurutan (warna khas tema dulu): yang pertama lolos 4,5 : 1, kalau tidak ada yang terkuat. */
private fun pilihTeks(belakang: Int, vararg calon: Int): Int =
    calon.firstOrNull { kontras(it, belakang) >= 4.5 } ?: calon.maxBy { kontras(it, belakang) }

/** Geser kecerahan teks sedikit demi sedikit (menjauh dari latarnya) sampai kontrasnya cukup. */
private fun jagaKontras(t: MutableMap<String, Int>) {
    for (u in UJI_KONTRAS) {
        if (u.depan == "garis") continue
        val b = t.getValue(u.belakang)
        val awal = keLch(t.getValue(u.depan))
        var l = awal.l
        val arah = if (luminans(t.getValue(u.depan)) < luminans(b)) -1 else 1
        var i = 0
        while (i < 60 && kontras(t.getValue(u.depan), b) < u.min) {
            l = min(0.99, max(0.02, l + arah * 0.01))
            t[u.depan] = dariLch(l, awal.c, awal.h)
            i++
        }
    }
}

/**
 * Palet mode Simpel dari satu warna benih (= aksen persis), terang dan gelap. Token lain mengikuti
 * resep Permen (benih Permen menghasilkan palet Permen). Benih yang cukup berwarna diperlakukan
 * minimal sepekat Permen; benih pucat tetap pucat. Nada gelap tema terang ikut rona benih sebanyak
 * [cocokGelap], sisanya plum Permen. Tema gelap: latar, kartu, ubin, dan teks tetap Permen; yang
 * berganti hanya warna inti (aksen, strip, garis terang, aksen kedua). Kontras teks dijaga.
 */
fun paletDariBenih(benih: Int): Pair<Token, Token> {
    val s = keLch(benih)
    val ce = if (s.c < 0.06) ACUAN_C * s.c / 0.06 else min(0.14, max(s.c, ACUAN_C))
    val wg = cocokGelap(s.h)
    fun geserTetangga(d: Double) = if (d <= -20) d * wg + (-8) * (1 - wg) else d
    fun buat(resep: Map<String, Resep>, k: String): Int {
        val r = resep.getValue(k)
        val l = if (r.relatif) min(0.97, max(0.05, s.l + r.l)) else r.l
        val d = if (k == "lavender" || k == "strip2") geserTetangga(r.d) else r.d
        val dd = if (d < 0 && l < 0.5) d - tambahGelap(s.h) else d
        return dariLch(l, ce * r.c, geserResep(s.h, dd))
    }
    val hitam = dariLch(0.12, min(0.03, ce * 0.3), geserResep(s.h, -10 - tambahGelap(s.h)))

    val terang = PERMEN_TERANG.toMutableMap()
    RESEP_TERANG.keys.forEach { terang[it] = buat(RESEP_TERANG, it) }
    terang["aksen"] = benih
    terang["strip1"] = benih
    terang["kartu"] = dariLch(0.995, ce * 0.08, geserResep(s.h, -10.0))
    for (k in listOf("tinta", "tintaRedup", "garis", "luarBulan")) terang[k] = campur(PERMEN_TERANG.getValue(k), terang.getValue(k), wg)
    terang["bayangan"] = terang.getValue("garis")
    terang["plum"] = terang.getValue("garis")
    terang["diAtasPlum"] = terang.getValue("latar")
    terang["ikonLayanan"] = terang.getValue("ubin")
    terang["pola"] = denganAlfa(terang.getValue("garis"), 0.15)
    terang["diAtasAksen"] = pilihTeks(benih, terang.getValue("tinta"), PUTIH, hitam)
    terang["latarLewat"] = campur(PERMEN_TERANG.getValue("latarLewat"), terang.getValue("latar"), 0.4)
    terang["latarMepet"] = campur(PERMEN_TERANG.getValue("latarMepet"), terang.getValue("latar"), 0.4)

    val gelap = PERMEN_GELAP.toMutableMap()
    for (k in listOf("garis", "lavender", "aksenTerang", "aksenGelap", "stiker", "strip0", "strip1", "strip2")) gelap[k] = buat(RESEP_GELAP, k)
    gelap["aksen"] = benih
    gelap["bayangan"] = gelap.getValue("garis")
    gelap["plum"] = gelap.getValue("garis")
    gelap["pola"] = denganAlfa(gelap.getValue("garis"), 0.12)
    gelap["taburan"] = denganAlfa(benih, 0.4)
    gelap["diAtasAksen"] = pilihTeks(benih, gelap.getValue("kartu"), PUTIH, hitam)

    // Judul strip: teks gelap bergaris putih di strip terang, teks terang bergaris gelap di strip gelap.
    for ((t, gel) in listOf(terang to false, gelap to true)) {
        val m = t.getValue("strip1")
        val khas = if (gel) t.getValue("stiker") else t.getValue("plum")
        val lawan = if (gel) t.getValue("kartu") else PUTIH
        val x = pilihTeks(m, khas, if (gel) t.getValue("kartu") else PUTIH, hitam)
        t["stiker"] = x
        t["garisStiker"] = when {
            x == khas -> lawan
            luminans(x) > 0.4 -> if (gel) t.getValue("kartu") else t.getValue("plum")
            else -> PUTIH
        }
    }
    jagaKontras(terang)
    jagaKontras(gelap)
    return terang to gelap
}

// ---------- Mode Lanjutan ----------

/** Kelompok bawaan mode Lanjutan untuk satu tema, diambil dari palet [t]. */
fun kelompokDari(t: Token): Map<String, Int> = KELOMPOK_WARNA.associate { (k, _) -> k to t.getValue(k) }

/**
 * Palet mode Lanjutan: tujuh kelompok pilihan pengguna, token lain diturunkan. Kontras tidak dijaga
 * (hanya diperingatkan lewat [ujiKontras]).
 */
fun turunkan(g: Map<String, Int>, gelap: Boolean): Token {
    fun geserL(argb: Int, d: Double) = keLch(argb).let { dariLch(min(0.98, max(0.05, it.l + d)), it.c, it.h) }
    fun terkuat(belakang: Int, vararg calon: Int) = calon.maxBy { kontras(it, belakang) }
    val latar = g.getValue("latar")
    val kartu = g.getValue("kartu")
    val tinta = g.getValue("tinta")
    val garis = g.getValue("garis")
    val aksen = g.getValue("aksen")
    val lav = g.getValue("lavender")
    val plum = g.getValue("plum")
    val aT = geserL(aksen, 0.08)
    val aG = geserL(aksen, -0.06)
    val dasar = permen(gelap)
    return dasar + mapOf(
        "latar" to latar, "kartu" to kartu, "tinta" to tinta, "garis" to garis, "bayangan" to garis,
        "aksen" to aksen, "lavender" to lav, "plum" to plum,
        "ubin" to campur(kartu, lav, if (gelap) 0.22 else 0.16),
        "tintaRedup" to campur(tinta, kartu, 0.36),
        "garisHalus" to campur(kartu, garis, if (gelap) 0.12 else 0.08),
        "aksenTerang" to aT, "aksenGelap" to aG,
        "diAtasAksen" to terkuat(aksen, tinta, kartu, latar),
        "diAtasPlum" to terkuat(plum, latar, kartu, tinta),
        "strip0" to if (gelap) campur(aksen, latar, 0.5) else campur(aksen, aT, 0.4),
        "strip1" to if (gelap) campur(aksen, latar, 0.62) else aksen,
        "strip2" to if (gelap) campur(lav, latar, 0.55) else campur(aksen, lav, 0.55),
        "stiker" to if (gelap) aT else plum,
        "garisStiker" to if (gelap) kartu else PUTIH,
        "taburan" to if (gelap) denganAlfa(aksen, 0.4) else dasar.getValue("taburan"),
        "pola" to denganAlfa(garis, if (gelap) 0.12 else 0.15),
        "latarLewat" to campur(dasar.getValue("latarLewat"), if (gelap) kartu else latar, if (gelap) 0.35 else 0.4),
        "latarMepet" to campur(dasar.getValue("latarMepet"), if (gelap) kartu else latar, if (gelap) 0.35 else 0.4),
        "luarBulan" to campur(tinta, kartu, 0.5),
        "ikonLayanan" to if (gelap) campur(kartu, garis, 0.12) else campur(kartu, lav, 0.16),
    )
}

// ---------- Pilihan pengguna ----------

enum class ModeWarna(val kode: String) { SIMPEL("simpel"), LANJUT("lanjut") }

/**
 * Pilihan warna pengguna. [contoh] = kode [CONTOH_WARNA] atau "bebas" ([bebas] = benihnya dalam
 * OKLCH). [lanjutTerang]/[lanjutGelap] = kelompok mode Lanjutan (kosong = belum pernah diatur).
 */
data class PilihanWarna(
    val mode: ModeWarna = ModeWarna.SIMPEL,
    val contoh: String = "permen",
    val bebas: Lch = keLch(CONTOH_WARNA[2].benih),
    val lanjutTerang: Map<String, Int> = emptyMap(),
    val lanjutGelap: Map<String, Int> = emptyMap(),
) {
    /** Warna benih mode Simpel. */
    val benih: Int get() = if (contoh == KODE_BEBAS) dariLch(bebas) else (CONTOH_WARNA.firstOrNull { it.kode == contoh } ?: CONTOH_WARNA[0]).benih

    /** Palet Simpel (terang, gelap) untuk pilihan ini; Permen = palet bawaan persis. */
    fun paletSimpel(): Pair<Token, Token> =
        if (contoh == CONTOH_WARNA[0].kode) PERMEN_TERANG to PERMEN_GELAP else paletDariBenih(benih)

    /** Palet yang dipakai tampilan (terang, gelap). */
    fun palet(): Pair<Token, Token> {
        if (mode == ModeWarna.SIMPEL) return paletSimpel()
        val (t, g) = paletSimpel()
        return turunkan(kelompokDari(t) + lanjutTerang, false) to turunkan(kelompokDari(g) + lanjutGelap, true)
    }

    companion object {
        const val KODE_BEBAS = "bebas"
    }
}

/** Kelompok Lanjutan sebagai teks `kunci=RRGGBB;…`. */
fun kodeKelompok(g: Map<String, Int>): String =
    g.entries.joinToString(";") { (k, v) -> k + "=" + "%06X".format(v and 0xFFFFFF) }

/** Kebalikan [kodeKelompok]; bagian rusak atau kunci tak dikenal dilewati. */
fun dariKodeKelompok(teks: String?): Map<String, Int> {
    if (teks.isNullOrBlank()) return emptyMap()
    val dikenal = KELOMPOK_WARNA.map { it.first }.toSet()
    return teks.split(";").mapNotNull { bagian ->
        val (k, v) = bagian.split("=").takeIf { it.size == 2 } ?: return@mapNotNull null
        val angka = v.trim().takeIf { it.length == 6 }?.toLongOrNull(16) ?: return@mapNotNull null
        if (k !in dikenal) null else k to (0xFF000000 or angka).toInt()
    }.toMap()
}

/** Benih Bebas sebagai teks `kecerahan,kepekatan,rona`. */
fun kodeLch(w: Lch): String = "%.3f,%.4f,%.1f".format(java.util.Locale.ROOT, w.l, w.c, w.h)

fun dariKodeLch(teks: String?): Lch? {
    val b = teks?.split(",")?.mapNotNull { it.trim().toDoubleOrNull() } ?: return null
    if (b.size != 3) return null
    return Lch(b[0].coerceIn(0.0, 1.0), b[1].coerceIn(0.0, 0.4), ((b[2] % 360) + 360) % 360)
}
