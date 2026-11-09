package io.github.quazzong.gwado.ui.komponen

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.tampilan.Tekstur
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Warna
import kotlin.math.max
import kotlin.math.roundToInt

/** Tekstur yang dipilih pengguna; disediakan MainActivity dari preferences. */
val LocalTekstur = staticCompositionLocalOf { Tekstur.BAWAAN }

/** Ubin berulang satu titik atau satu sudut kotak, dipakai sebagai kuas yang diulang ke seluruh layar. */
private fun kuasUbin(jarak: Int, gambar: Canvas.(Paint) -> Unit, warna: Color): ShaderBrush {
    val ubin = ImageBitmap(jarak, jarak)
    Canvas(ubin).gambar(Paint().apply { color = warna; isAntiAlias = true })
    return ShaderBrush(ImageShader(ubin, TileMode.Repeated, TileMode.Repeated))
}

/**
 * Gambar tekstur [t] di belakang isi. Pola dibuat sekali per ukuran (drawWithCache), lalu diulang
 * lewat satu kuas bitmap, jadi menggulir tidak membuatnya ulang.
 */
fun Modifier.tekstur(t: Tekstur, w: Warna): Modifier = drawWithCache {
    when (t) {
        Tekstur.POLOS -> onDrawBehind {}
        Tekstur.TITIK -> {
            val jarak = max(1, 12.dp.toPx().roundToInt())
            val kuas = kuasUbin(jarak, { p -> drawCircle(Offset(jarak / 2f, jarak / 2f), 1.3.dp.toPx(), p) }, w.pola)
            onDrawBehind { drawRect(kuas) }
        }
        Tekstur.KOTAK -> {
            val jarak = max(1, 14.dp.toPx().roundToInt())
            val tebal = 1.dp.toPx()
            val kuas = kuasUbin(jarak, { p ->
                drawRect(0f, 0f, jarak.toFloat(), tebal, p)
                drawRect(0f, tebal, tebal, jarak.toFloat(), p)
            }, w.pola)
            onDrawBehind { drawRect(kuas) }
        }
    }
}

/** Latar layar penuh: warna latar + tekstur pilihan pengguna. Tidak pernah dipakai di belakang teks kartu. */
@Composable
fun Modifier.latarLayar(): Modifier {
    val w = GwadoTema.warna
    return background(w.latar).tekstur(LocalTekstur.current, w)
}
