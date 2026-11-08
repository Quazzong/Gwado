package io.github.quazzong.gwado.widget

import io.github.quazzong.gwado.beranda.Progres
import io.github.quazzong.gwado.segar.jamSingkat
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.ui.layar.beresSitus
import io.github.quazzong.gwado.ui.layar.mendesak
import io.github.quazzong.gwado.waktu.Chip
import io.github.quazzong.gwado.waktu.dariDb
import io.github.quazzong.gwado.waktu.formatTanggal
import io.github.quazzong.gwado.waktu.sisaChip
import java.time.Instant
import kotlin.math.floor

/** Satu baris widget: tugas + chip sisa waktu (null = tanpa deadline) + keterangan untuk baris rinci. */
data class BarisWidget(val id: String, val judul: String, val chip: Chip?, val keterangan: String)

/** Isi kedua widget, dihitung sekali dari database. */
data class IsiWidget(
    val mendesak: Int,
    /** Tugas belum beres, urut seperti beranda (paling banyak sebanyak `maks`). */
    val terdekat: List<BarisWidget>,
    /** Jumlah tugas yang masih harus dikerjakan (belum beres), termasuk yang tidak ikut di [terdekat]. */
    val jumlah: Int,
    /** Label pengambilan sukses terakhir ("10.02"), null kalau belum pernah. */
    val diambil: String?,
    /** Progres minggu ini; null kalau minggu ini tidak ada tugas ber-deadline. */
    val progres: Progres?,
)

/**
 * Isi widget dari daftar tugas biasa (belum dicentang, masih di situs). Urutannya sama dengan
 * beranda: yang mendesak dulu, lalu sisanya; tugas yang sudah beres di situs tidak ikut.
 */
fun isiWidget(
    daftar: List<BarisTugas>,
    sekarang: Instant,
    diambil: Instant?,
    maks: Int = 3,
    progres: Progres? = null,
): IsiWidget {
    val (atas, bawah) = daftar.filterNot(::beresSitus).partition { mendesak(it, sekarang) }
    val urut = atas + bawah
    val baris = urut.take(maks).map { t ->
        // Tanpa deadline sudah tertulis di chip, jadi keterangannya cukup mata kuliah.
        val waktu = t.deadline?.let { formatTanggal(dariDb(it)) }
        BarisWidget(
            t.id,
            t.judul,
            t.deadline?.let { sisaChip(dariDb(it), sekarang) },
            listOfNotNull(t.matkul, waktu).joinToString(" · ").ifEmpty { "Tugas manual" },
        )
    }
    return IsiWidget(
        mendesak = atas.size,
        terdekat = baris,
        jumlah = urut.size,
        diambil = diambil?.let { jamSingkat(it, sekarang) },
        progres = progres?.takeIf { it.total > 0 },
    )
}

/** Teks saat tidak ada tugas: belum pernah diambil, atau semua sudah beres. */
fun pesanKosong(isi: IsiWidget, singkat: Boolean): String = when {
    isi.diambil == null && singkat -> "Belum ada tugas"
    isi.diambil == null -> "Belum ada tugas. Tarik beranda Gwado untuk mengambil."
    singkat -> "Semua beres"
    else -> "Semua tugas beres"
}

/**
 * Tata letak widget Terdekat: berapa baris, baris ringkas atau rinci, apakah progres ikut, dan
 * apakah sisa ruangnya cukup untuk stiker.
 */
data class TataTerdekat(val baris: Int, val rinci: Boolean, val progres: Boolean, val stiker: Boolean = false)

/**
 * Launcher (misalnya Samsung) melaporkan tinggi widget lebih besar daripada yang digambar,
 * jadi tinggi laporan dikecilkan dulu supaya baris terakhir tidak terpotong.
 */
internal const val KOREKSI_TINGGI = 0.86f
internal const val TINGGI_KEPALA = 26f      // padding atas + label TERDEKAT
internal const val TINGGI_BAWAH = 13f       // padding bawah + bayangan
internal const val BARIS_RINGKAS = 24f      // chip + judul satu baris + garis
internal const val BARIS_RINCI = 40f        // + mata kuliah dan tanggal
internal const val TINGGI_PROGRES = 34f
internal const val TINGGI_STIKER = 60f     // stiker 56dp + jarak

/**
 * Hitung tata letak dari tinggi laporan (dp) dan jumlah tugas yang tersedia. Kalau tugasnya lebih
 * banyak dari yang muat, semua baris ringkas. Kalau semuanya muat, ruang sisa dipakai baris rinci,
 * lalu progres minggu ini di bawah, lalu stiker di ruang yang masih kosong.
 */
fun tataTerdekat(tinggiLapor: Float, tersedia: Int, adaProgres: Boolean): TataTerdekat {
    val ruang = tinggiLapor * KOREKSI_TINGGI - TINGGI_KEPALA - TINGGI_BAWAH
    val muat = floor(ruang / BARIS_RINGKAS).toInt().coerceAtLeast(1)
    if (tersedia >= muat) return TataTerdekat(muat.coerceAtMost(tersedia), rinci = false, progres = false)
    val rinci = tersedia * BARIS_RINCI <= ruang
    val sisa = ruang - tersedia * (if (rinci) BARIS_RINCI else BARIS_RINGKAS)
    val progres = adaProgres && sisa >= TINGGI_PROGRES
    val sisaStiker = sisa - if (progres) TINGGI_PROGRES else 0f
    return TataTerdekat(tersedia, rinci, progres, stiker = sisaStiker >= TINGGI_STIKER)
}
