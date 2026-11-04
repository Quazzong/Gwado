package io.github.quazzong.gwado.ui.komponen

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.ui.tema.GwadoTema
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Bentuk bergelombang: jumlah lekuk n dan kedalaman a. */
private class Lekuk(val n: Int, val a: Float)

private val KUE = Lekuk(9, 0.1f)

/** Lama satu putaran, dalam milidetik. */
private const val PUTARAN_MS = 1150f

/** Lekuk acak 3..11, tidak sama dengan [sebelum]; kedalaman mengecil untuk lekuk banyak. */
private fun acakLekuk(sebelum: Int): Lekuk {
    var n: Int
    do {
        n = 3 + Random.nextInt(9)
    } while (n == sebelum)
    return Lekuk(n, 0.06f + 0.5f / n)
}

/** Kurva ease in-out kubik, u dan hasilnya 0..1. */
private fun lambatCepatLambat(u: Float): Float =
    if (u < 0.5f) 4 * u * u * u else 1 - (-2 * u + 2).pow(3) / 2

/** Bentuk perpaduan dua lekuk dengan bobot e (0 = dari, 1 = ke), diputar [putar] radian. */
private fun pathPemuat(pusat: Offset, r: Float, dari: Lekuk, ke: Lekuk, e: Float, putar: Float, langkah: Int = 96): Path =
    Path().apply {
        for (i in 0..langkah) {
            val t = i.toFloat() / langkah * 2 * PI.toFloat()
            val r1 = r * (1 + dari.a * cos(dari.n * t))
            val r2 = r * (1 + ke.a * cos(ke.n * t))
            val rr = r1 + (r2 - r1) * e
            val x = pusat.x + rr * cos(t + putar)
            val y = pusat.y + rr * sin(t + putar)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

/** Putaran yang sedang berjalan. Diubah dari fase gambar, bukan state Compose. */
private class Putaran(var dari: Lekuk, var ke: Lekuk, var mulai: Long)

/** True kalau animasi sistem dimatikan (skala durasi animator 0). */
@Composable
private fun animasiMati(): Boolean {
    val konteks = LocalContext.current
    return remember {
        Settings.Global.getFloat(konteks.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Pemuat: kue yang berputar sekali per putaran (ease in-out) sambil berubah bentuk; jumlah
 * lekuk diacak tiap putaran. [berjalan] false, atau animasi sistem mati, = kue diam.
 * Hanya fase gambar yang diulang tiap frame.
 */
@Composable
fun Pemuat(modifier: Modifier = Modifier, ukuran: Dp = 40.dp, berjalan: Boolean = true, label: String = "Memuat") {
    val w = GwadoTema.warna
    val isi = w.gradasiAksen()
    val gerak = berjalan && !animasiMati()
    val waktu = remember { mutableLongStateOf(0L) }
    val putaran = remember { Putaran(KUE, acakLekuk(KUE.n), -1L) }
    if (gerak) {
        LaunchedEffect(Unit) {
            putaran.dari = KUE
            putaran.ke = acakLekuk(KUE.n)
            putaran.mulai = -1L
            while (true) withFrameNanos { waktu.longValue = it / 1_000_000L }
        }
    }
    Canvas(modifier.size(ukuran).semantics { contentDescription = label }) {
        val s = size.minDimension / 40f
        var dari = KUE
        var ke = KUE
        var e = 0f
        var putar = -PI.toFloat() / 2
        val t = waktu.longValue
        if (gerak && t > 0L) {
            if (putaran.mulai < 0L) putaran.mulai = t
            var u = (t - putaran.mulai) / PUTARAN_MS
            if (u >= 1f) {
                putaran.dari = putaran.ke
                putaran.ke = acakLekuk(putaran.dari.n)
                putaran.mulai = t
                u = 0f
            }
            e = lambatCepatLambat(u)
            dari = putaran.dari
            ke = putaran.ke
            putar += e * 2 * PI.toFloat()
        }
        val p = pathPemuat(Offset(20 * s, 20 * s), 14 * s, dari, ke, e, putar)
        translate(2 * s, 2 * s) { drawPath(p, w.bayangan) }
        drawPath(p, isi)
        drawPath(p, w.garis, style = Stroke(2.2f * s, join = StrokeJoin.Round))
    }
}
