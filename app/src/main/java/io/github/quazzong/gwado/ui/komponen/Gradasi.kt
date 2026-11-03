package io.github.quazzong.gwado.ui.komponen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gradasi linear seperti CSS linear-gradient(<sudut>deg, ...): 0 = ke atas, 90 = ke kanan.
 * Panjang garis gradasi menyesuaikan ukuran bidang, jadi ujung warnanya tepat di sudut.
 */
fun gradasiCss(sudut: Float, titik: List<Pair<Float, Color>>): ShaderBrush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val rad = Math.toRadians(sudut.toDouble())
        val dx = sin(rad).toFloat()
        val dy = -cos(rad).toFloat()
        val setengah = (abs(size.width * dx) + abs(size.height * dy)) / 2
        val cx = size.width / 2
        val cy = size.height / 2
        return LinearGradientShader(
            from = Offset(cx - dx * setengah, cy - dy * setengah),
            to = Offset(cx + dx * setengah, cy + dy * setengah),
            colors = titik.map { it.second },
            colorStops = titik.map { it.first },
        )
    }
}
