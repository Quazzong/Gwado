package io.github.quazzong.gwado.beranda

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.waktu.WIB
import io.github.quazzong.gwado.waktu.dariDb
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Kunci preferences untuk tanggal kuliah pertama semester (YYYY-MM-DD, WIB). */
const val KUNCI_KULIAH_MULAI = "kuliah_mulai"

private fun senin(tanggal: LocalDate): LocalDate =
    tanggal.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Tanggal kuliah pertama yang tersimpan, atau null kalau belum diisi atau isinya rusak. */
fun Db.kuliahMulai(): LocalDate? =
    (satu("SELECT nilai FROM preferences WHERE kunci = ?", KUNCI_KULIAH_MULAI)?.get("nilai") as String?)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** Simpan tanggal kuliah pertama; null menghapusnya. */
fun Db.simpanKuliahMulai(tanggal: LocalDate?) {
    if (tanggal == null) {
        exec("DELETE FROM preferences WHERE kunci = ?", KUNCI_KULIAH_MULAI)
    } else {
        exec(
            "INSERT INTO preferences (kunci, nilai) VALUES (?, ?) " +
                "ON CONFLICT (kunci) DO UPDATE SET nilai = excluded.nilai",
            KUNCI_KULIAH_MULAI, tanggal.toString(),
        )
    }
}

/**
 * Minggu kuliah ke-n: minggu Senin–Minggu yang memuat kuliah pertama adalah minggu 1.
 * Null sebelum kuliah dimulai.
 */
fun mingguKe(kuliahMulai: LocalDate, hariIni: LocalDate): Int? {
    if (hariIni < kuliahMulai) return null
    return (ChronoUnit.DAYS.between(senin(kuliahMulai), senin(hariIni)) / 7 + 1).toInt()
}

data class Progres(val selesai: Int, val total: Int)

/** Status situs yang berarti tugas sudah beres di sana: terkumpul, atau ditandai selesai. */
fun beresDiSitus(statusKumpulSitus: String?): Boolean =
    statusKumpulSitus == "terkumpul" || statusKumpulSitus == "selesai"

/**
 * Tugas yang deadline-nya (tanggal WIB) jatuh di minggu ini, Senin–Minggu. Selesai =
 * dicentang atau beres di situs. Tugas yang hilang dari situs dan belum dicentang
 * tidak dihitung. Masukan: daftarTugas(semua = true).
 */
fun progresMinggu(semua: List<BarisTugas>, hariIni: LocalDate): Progres {
    val awal = senin(hariIni)
    val akhir = awal.plusDays(6)
    val mingguIni = semua.filter { t ->
        val dl = t.deadline ?: return@filter false
        val tanggal = dariDb(dl).atOffset(WIB).toLocalDate()
        tanggal in awal..akhir && (t.statusSitus == "aktif" || t.status == "done")
    }
    return Progres(
        selesai = mingguIni.count { it.status == "done" || beresDiSitus(it.statusKumpulSitus) },
        total = mingguIni.size,
    )
}

/**
 * Tugas yang beres: dicentang pengguna, atau beres di situs dan masih ada di situs.
 * Centang tetap milik pengguna; status situs hanya ikut menentukan tampilan.
 */
fun beres(t: BarisTugas): Boolean =
    t.status == "done" || (beresDiSitus(t.statusKumpulSitus) && t.statusSitus == "aktif")

/**
 * Tugas beres yang deadline-nya (tanggal WIB) jatuh di minggu ini, Senin–Minggu, untuk
 * bagian SELESAI di beranda. Sama dengan yang dihitung selesai oleh [progresMinggu].
 * Masukan: daftarTugas(semua = true).
 */
fun selesaiMinggu(semua: List<BarisTugas>, hariIni: LocalDate): List<BarisTugas> {
    val awal = senin(hariIni)
    val akhir = awal.plusDays(6)
    return semua.filter { t ->
        beres(t) && t.deadline?.let { dariDb(it).atOffset(WIB).toLocalDate() in awal..akhir } == true
    }
}

/** Semua tugas beres, deadline terbaru dulu, tanpa deadline di akhir. Masukan: daftarTugas(semua = true). */
fun semuaSelesai(semua: List<BarisTugas>): List<BarisTugas> =
    semua.filter(::beres).sortedWith(
        compareBy<BarisTugas> { it.deadline == null }.thenByDescending { it.deadline?.let(::dariDb) },
    )

/**
 * Saring daftar tugas: layanan (null = semua; tugas manual bernama "Manual") dan kata yang dicari
 * di judul atau mata kuliah, tanpa membedakan huruf besar-kecil. Kata kosong = tanpa pencarian.
 */
fun saring(daftar: List<BarisTugas>, kata: String, layanan: String?): List<BarisTugas> {
    val k = kata.trim()
    return daftar.filter { t ->
        (layanan == null || (t.sumber ?: "Manual") == layanan) &&
            (k.isEmpty() || t.judul.contains(k, ignoreCase = true) || t.matkul?.contains(k, ignoreCase = true) == true)
    }
}
