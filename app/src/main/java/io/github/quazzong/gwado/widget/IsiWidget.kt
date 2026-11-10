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
 * Tata letak widget Terdekat: berapa baris, baris ringkas atau rinci, dan apakah progres ikut.
 */
data class TataTerdekat(val baris: Int, val rinci: Boolean, val progres: Boolean)

/**
 * Launcher (misalnya Samsung) melaporkan tinggi widget lebih besar daripada yang digambar,
 * jadi tinggi laporan dikecilkan dulu supaya baris terakhir tidak terpotong.
 */
internal const val KOREKSI_TINGGI = 0.86f
internal const val TINGGI_KEPALA = 26f      // padding atas + label TERDEKAT
internal const val TINGGI_BAWAH = 13f       // padding bawah + bayangan
internal const val BARIS_RINGKAS = 22f      // chip + judul satu baris + garis (diukur ±21,7dp)
internal const val TINGGI_GARIS = 2f        // garis pemisah; baris pertama tidak punya
internal const val BARIS_RINCI = 40f        // + mata kuliah dan tanggal
internal const val TINGGI_PROGRES = 34f

/**
 * Hitung tata letak dari tinggi laporan (dp) dan jumlah tugas yang tersedia. Kalau tugasnya lebih
 * banyak dari yang muat, semua baris ringkas. Kalau semuanya muat, ruang sisa dipakai baris rinci,
 * lalu progres minggu ini di bawah.
 */
fun tataTerdekat(tinggiLapor: Float, tersedia: Int, adaProgres: Boolean): TataTerdekat {
    val ruang = tinggiLapor * KOREKSI_TINGGI - TINGGI_KEPALA - TINGGI_BAWAH
    val muat = floor((ruang + TINGGI_GARIS) / BARIS_RINGKAS).toInt().coerceAtLeast(1)
    if (tersedia >= muat) return TataTerdekat(muat.coerceAtMost(tersedia), rinci = false, progres = false)
    val rinci = tersedia * BARIS_RINCI <= ruang
    val sisa = ruang - tersedia * (if (rinci) BARIS_RINCI else BARIS_RINGKAS)
    return TataTerdekat(tersedia, rinci, progres = adaProgres && sisa >= TINGGI_PROGRES)
}

internal const val PADDING_DATAR = 27f      // padding kiri + kanan kartu (termasuk bayangan)
internal const val PADDING_TEGAK = 23f      // padding atas + bawah kartu (termasuk bayangan)
internal const val TINGGI_CHIP = 24f        // chip 11sp + padding + garis, ditambah jarak di atasnya
internal const val TINGGI_BARIS = 1.2f      // tinggi baris teks dibanding ukuran hurufnya
internal const val LEBAR_DIGIT = 0.6f       // lebar satu angka tebal dibanding ukuran hurufnya
internal const val ANGKA_MAKS = 48f
internal const val ANGKA_MIN = 14f

/**
 * Ukuran angka (sp) widget Tugas yang sempit: sebesar mungkin sampai [ANGKA_MAKS], asal angka dan
 * chip di bawahnya muat di dalam kartu. Angka yang lebih tinggi dari ruangnya ikut terjepit dan
 * hanya potongan tengahnya yang tampak. [skalaHuruf] = ukuran huruf sistem (1 = normal).
 */
fun ukuranAngkaSempit(
    lebarLapor: Float,
    tinggiLapor: Float,
    digit: Int,
    adaChip: Boolean,
    skalaHuruf: Float = 1f,
): Float {
    val tinggi = tinggiLapor * KOREKSI_TINGGI - PADDING_TEGAK - if (adaChip) TINGGI_CHIP * skalaHuruf else 0f
    val lebar = lebarLapor - PADDING_DATAR
    val dp = minOf(tinggi / TINGGI_BARIS, lebar / (LEBAR_DIGIT * digit.coerceAtLeast(1)))
    return (dp / skalaHuruf).coerceIn(ANGKA_MIN, ANGKA_MAKS)
}

internal const val TINGGI_LABEL = 16f       // label TUGAS
internal const val TINGGI_TUGAS = 36f       // judul dua baris 12sp + jarak (tanpa chip)
internal const val ANGKA_TINGGI_MAKS = 96f

/**
 * Ukuran angka (sp) widget Tugas yang tinggi: mengisi ruang di antara label dan tugas pertama,
 * paling kecil 40sp dan paling besar [ANGKA_TINGGI_MAKS] (launcher bisa melaporkan tinggi lebih
 * besar daripada yang digambar, jadi angka tidak dibuat sebesar laporan).
 */
fun ukuranAngkaTinggi(lebarLapor: Float, tinggiLapor: Float, digit: Int, skalaHuruf: Float = 1f): Float {
    val tinggi = tinggiLapor * KOREKSI_TINGGI - PADDING_TEGAK - (TINGGI_LABEL + TINGGI_TUGAS + TINGGI_CHIP) * skalaHuruf
    val lebar = lebarLapor - PADDING_DATAR
    val dp = minOf(tinggi / TINGGI_BARIS, lebar / (LEBAR_DIGIT * digit.coerceAtLeast(1)))
    return (dp / skalaHuruf).coerceIn(40f, ANGKA_TINGGI_MAKS)
}
