package io.github.quazzong.gwado.ui.komponen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Waktu sekarang yang diperbarui tiap pergantian menit selama layar terlihat.
 * Saat kembali dari latar belakang langsung diperbarui.
 */
@Composable
fun rememberSekarang(): Instant {
    val pemilik = LocalLifecycleOwner.current
    val sekarang = remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(pemilik) {
        pemilik.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                sekarang.value = Instant.now()
                delay(60_000 - System.currentTimeMillis() % 60_000)
            }
        }
    }
    return sekarang.value
}
