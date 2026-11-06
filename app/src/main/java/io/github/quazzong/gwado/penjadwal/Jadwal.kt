package io.github.quazzong.gwado.penjadwal

import io.github.quazzong.gwado.segar.JADWAL_AMBIL
import io.github.quazzong.gwado.waktu.WIB
import java.time.Instant

/** Jam ambil terjadwal terakhir yang sudah tiba (WIB), tepat pada jamnya sudah dihitung. */
fun jadwalTerakhir(sekarang: Instant): Instant {
    var hari = sekarang.atOffset(WIB).toLocalDate()
    while (true) {
        for (jam in JADWAL_AMBIL.reversed()) {
            val t = hari.atTime(jam, 0).atOffset(WIB).toInstant()
            if (!t.isAfter(sekarang)) return t
        }
        hari = hari.minusDays(1)
    }
}

/**
 * Sumber perlu diambil otomatis kalau belum ada run (sukses atau gagal) sejak jadwal terakhir.
 * Run gagal tidak diulang sampai jadwal berikutnya; yang ditahan penahan tidak tercatat,
 * jadi dicoba lagi pada pengecekan berikutnya.
 */
fun perluAmbil(selesaiTerakhir: Instant?, sekarang: Instant): Boolean =
    selesaiTerakhir == null || selesaiTerakhir.isBefore(jadwalTerakhir(sekarang))
