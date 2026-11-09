package io.github.quazzong.gwado.ui.tema

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Token gerak: durasi (ms) dan kurva yang dipakai semua komponen.
 * Pegas = isi yang masuk (memantul sedikit); mulus = bidang yang melebar/menyusut (cepat lalu
 * melambat); keluar = isi yang pergi (mempercepat); letup = benda berbentuk kue.
 */
object Gerak {
    val Pegas: Easing = CubicBezierEasing(0.2f, 1.4f, 0.35f, 1f)
    val Mulus: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)
    val Letup: Easing = CubicBezierEasing(0.34f, 1.6f, 0.64f, 1f)
    val Pindah: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val Keluar: Easing = CubicBezierEasing(0.42f, 0f, 1f, 1f)

    const val TEKAN = 80
    const val CEPAT = 120
    const val PUDAR = 150
    const val PINDAH = 220
    const val LETUP = 260
    const val BUTIR = 380
    const val LOMPAT_IKON = 380
    const val TUTUP = 380
    const val MULUS = 440
    const val ISI_MASUK = 480
    const val PEGAS = 520
    const val RAYAKAN = 520

    /** Jeda antar elemen yang masuk/keluar berurutan. */
    const val JEDA_TAB = 60
    const val JEDA_ISI = 55
    const val JEDA_KELUAR = 14
    const val JEDA_BUTIR = 10

    /** Elemen ke-n setelah ini masuk bersamaan dengan elemen terakhir yang berjeda. */
    const val MAKS_BERJEDA = 6

    /**
     * Skala durasi animasi sistem (Opsi developer / aksesibilitas). Compose mengalikan semua durasi
     * dengan skala ini; Gwado membaginya kembali supaya gerak selalu secepat rancangan, apa pun
     * pengaturan HP. Skala 0 (animasi dimatikan) tetap dihormati: gerak langsung ke akhir.
     */
    @Volatile
    var skala: Float = 1f

    /** Durasi/jeda [ms] yang sudah dikompensasi skala animasi sistem. */
    fun ms(ms: Int): Int = if (skala > 0f && skala != 1f) (ms / skala).roundToInt() else ms
}

/** Baca ulang skala animasi sistem (dipanggil saat aplikasi dibuka/kembali tampil). */
fun bacaSkalaGerak(konteks: android.content.Context) {
    Gerak.skala = Settings.Global.getFloat(konteks.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
}

/** Seperti tween(), tetapi durasi dan jeda tidak terpengaruh skala animasi sistem (lihat [Gerak.ms]). */
fun <T> gtween(durationMillis: Int = 300, delayMillis: Int = 0, easing: Easing = FastOutSlowInEasing): TweenSpec<T> =
    tween(Gerak.ms(durationMillis), Gerak.ms(delayMillis), easing)

/** True kalau animasi sistem dimatikan (skala durasi animator 0): semua gerak langsung ke akhir. */
@Composable
fun animasiMati(): Boolean {
    val konteks = LocalContext.current
    return remember {
        Settings.Global.getFloat(konteks.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
