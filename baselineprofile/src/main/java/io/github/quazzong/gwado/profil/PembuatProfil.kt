package io.github.quazzong.gwado.profil

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Merekam kode yang dipakai saat Gwado dibuka dan dipakai sebentar: beranda digulir,
 * detail tugas dibuka lalu ditutup, tab Kalender dan Layanan dibuka. Hasilnya menjadi
 * Baseline Profile yang dikompilasi lebih dulu saat aplikasi dipasang.
 *
 * Butuh Gwado yang sudah disiapkan (layanan tersambung, tugas sudah diambil) di perangkat.
 * Data aplikasi tidak dihapus dan tidak ikut ke profil.
 */
@RunWith(AndroidJUnit4::class)
class PembuatProfil {

    @get:Rule
    val aturan = BaselineProfileRule()

    @Test
    fun buat() = aturan.collect(packageName = PAKET, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()

        val lebar = device.displayWidth
        val tinggi = device.displayHeight

        // Beranda digulir turun lalu naik.
        repeat(2) {
            device.swipe(lebar / 2, tinggi * 3 / 4, lebar / 2, tinggi / 4, 20)
            device.waitForIdle()
        }
        repeat(2) {
            device.swipe(lebar / 2, tinggi / 4, lebar / 2, tinggi * 3 / 4, 20)
            device.waitForIdle()
        }

        // Kartu tugas pertama di beranda (dicari lewat chip sisa waktunya) → detail → kembali.
        val chip = device.wait(Until.findObject(By.text(CHIP_SISA)), 5_000)
            ?: error("Tidak ada kartu tugas di beranda: siapkan Gwado dan ambil tugas dulu")
        chip.click()
        device.waitForIdle()
        Thread.sleep(800)
        device.pressBack()
        device.waitForIdle()
        Thread.sleep(800)

        bukaTab("Kalender")
        // Ganti bulan dengan geser di grid, lalu kembali.
        device.swipe(lebar * 4 / 5, tinggi / 3, lebar / 5, tinggi / 3, 15)
        device.waitForIdle()
        device.swipe(lebar / 5, tinggi / 3, lebar * 4 / 5, tinggi / 3, 15)
        device.waitForIdle()

        bukaTab("Layanan")
        bukaTab("Tugas")
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.bukaTab(nama: String) {
        device.wait(Until.findObject(By.text(nama)), 3_000)?.click()
        device.waitForIdle()
        Thread.sleep(600)
    }

    private companion object {
        const val PAKET = "io.github.quazzong.gwado"
        /** Teks chip sisa waktu di kartu tugas, misalnya "6 HARI", "LEWAT 3 JAM", "BESOK". */
        val CHIP_SISA: Pattern = Pattern.compile(".*(HARI|JAM|MENIT|BESOK).*")
    }
}
