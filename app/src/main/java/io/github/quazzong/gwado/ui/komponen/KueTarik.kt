package io.github.quazzong.gwado.ui.komponen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Indikator tarik untuk menyegarkan: pemuat yang turun, membesar, dan berputar mengikuti
 * tarikan, lalu berubah bentuk sambil berputar selama penyegaran berjalan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KueTarik(state: PullToRefreshState, menyegarkan: Boolean, modifier: Modifier = Modifier) {
    val tarik = if (menyegarkan) 1f else state.distanceFraction.coerceIn(0f, 1f)
    if (tarik == 0f) return
    Pemuat(
        modifier.graphicsLayer {
            translationY = (-52f + 68f * tarik) * density
            scaleX = 0.6f + 0.4f * tarik
            scaleY = 0.6f + 0.4f * tarik
            rotationZ = if (menyegarkan) 0f else tarik * 270f
            alpha = tarik
        },
        ukuran = 44.dp,
        berjalan = menyegarkan,
        label = if (menyegarkan) "Mengambil tugas" else "Tarik untuk mengambil tugas",
    )
}
