package io.github.quazzong.gwado.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.quazzong.gwado.tampilan.PilihanWarna
import io.github.quazzong.gwado.tampilan.Tema
import io.github.quazzong.gwado.tampilan.Token

private val LokalWarna = staticCompositionLocalOf { WarnaTerang }
private val LokalGelap = staticCompositionLocalOf { false }

/** Pilihan tema pengguna (Ikut sistem/Terang/Gelap); disediakan MainActivity. */
val LocalPilihanTema = staticCompositionLocalOf { Tema.BAWAAN }

/**
 * Pemegang pilihan warna yang sedang dipakai. Mengubah [pilihan] langsung mengganti warna seluruh
 * aplikasi (live, misalnya selama slider digeser); menyimpannya urusan pemanggil.
 */
class PengaturWarna(awal: PilihanWarna) {
    var pilihan by mutableStateOf(awal)
    /** Palet (terang, gelap) dari [pilihan], dihitung ulang hanya saat pilihan berubah. */
    val palet: Pair<Token, Token> by derivedStateOf { pilihan.palet() }
}

/** Pengatur warna aplikasi; disediakan MainActivity. */
val LocalPengaturWarna = staticCompositionLocalOf { PengaturWarna(PilihanWarna()) }

/** Pintu baca token: `GwadoTema.warna.aksen`. */
object GwadoTema {
    val warna: Warna
        @Composable @ReadOnlyComposable get() = LokalWarna.current

    /** Tampilan sedang gelap (dari pilihan tema, bukan selalu dari sistem). */
    val gelap: Boolean
        @Composable @ReadOnlyComposable get() = LokalGelap.current
}

/**
 * Tema aplikasi: [palet] (terang, gelap; bawaan Permen), versi terang atau gelap (bawaan ikut sistem).
 * Warna juga dipetakan ke Material 3.
 */
@Composable
fun GwadoTema(gelap: Boolean = isSystemInDarkTheme(), palet: Pair<Token, Token>? = null, isi: @Composable () -> Unit) {
    val warna = remember(gelap, palet) {
        when {
            palet == null -> if (gelap) WarnaGelap else WarnaTerang
            gelap -> warnaDari(palet.second, gelap = true)
            else -> warnaDari(palet.first, gelap = false)
        }
    }
    CompositionLocalProvider(LokalWarna provides warna, LokalGelap provides gelap, LocalContentColor provides warna.tinta) {
        MaterialTheme(colorScheme = skemaM3(warna, gelap), typography = TipografiM3, content = isi)
    }
}

private fun skemaM3(w: Warna, gelap: Boolean): ColorScheme =
    (if (gelap) darkColorScheme() else lightColorScheme()).copy(
        primary = w.aksen,
        onPrimary = w.diAtasAksen,
        secondary = w.plum,
        onSecondary = w.diAtasPlum,
        background = w.latar,
        onBackground = w.tinta,
        surface = w.kartu,
        onSurface = w.tinta,
        surfaceVariant = w.ubin,
        onSurfaceVariant = w.tintaRedup,
        surfaceTint = w.kartu,
        surfaceContainerLowest = w.kartu,
        surfaceContainerLow = w.kartu,
        surfaceContainer = w.kartu,
        surfaceContainerHigh = w.kartu,
        surfaceContainerHighest = w.ubin,
        outline = w.garis,
        outlineVariant = w.garisHalus,
        error = w.galat,
        onError = w.kartu,
    )

private val TipografiM3 = Typography(
    titleLarge = Huruf.judulStrip,
    titleMedium = Huruf.judulBagian,
    bodyLarge = Huruf.isi,
    bodyMedium = Huruf.isi,
    bodySmall = Huruf.meta,
    labelLarge = Huruf.tombol,
    labelMedium = Huruf.meta,
    labelSmall = Huruf.chip,
)
