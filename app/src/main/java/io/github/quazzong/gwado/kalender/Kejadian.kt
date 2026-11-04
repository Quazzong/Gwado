package io.github.quazzong.gwado.kalender

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.tugas.keDb
import io.github.quazzong.gwado.waktu.WIB
import io.github.quazzong.gwado.waktu.dariDb
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Batas panjang rentang kalender, dalam hari. */
const val MAKS_HARI = 366

class KalenderGalat(pesan: String) : IllegalArgumentException(pesan)

/**
 * Satu kejadian kalender: deadline tugas, pertemuan kuliah, atau acara. Waktu berupa teks UTC
 * database. status: normal, batal, bekas_pindah (slot asli kuliah yang dipindah), dipindah.
 */
data class Kejadian(
    val sumber: String,
    val ref: String,
    val tanggalAsli: String = "",
    val status: String = "normal",
    val seharian: Boolean = false,
    val mulai: String,
    val selesai: String,
    val judul: String = "",
    val lokasi: String? = null,
    val kalender: String? = null,
    val warna: Int? = null,
    val peran: String? = null,
    val kodeMk: String? = null,
    val pindahKe: String? = null,
)

private typealias Berlaku = Pair<LocalDate?, LocalDate?>

private typealias Baris = Map<String, Any?>

private const val SQL_ACARA = """
SELECT a.*, k.kode AS kal_kode, k.warna AS kal_warna, k.peran AS kal_peran
FROM acara a JOIN kalender k ON k.id = a.kalender_id
"""

internal fun periksaRentang(dari: LocalDate, sampai: LocalDate) {
    if (sampai < dari) throw KalenderGalat("tanggal sampai tidak boleh sebelum tanggal dari")
    if (ChronoUnit.DAYS.between(dari, sampai) + 1 > MAKS_HARI) throw KalenderGalat("rentang paling panjang $MAKS_HARI hari")
}

/** Tanggal + jam dinding WIB → teks UTC. */
private fun utc(tanggal: LocalDate, hhmm: String = "00:00"): String {
    val (j, m) = hhmm.split(":").map { it.toInt() }
    return keDb(tanggal.atTime(LocalTime.of(j, m)).atOffset(WIB).toInstant())
}

private fun tanggalWib(utc: String): String = dariDb(utc).atOffset(WIB).toLocalDate().toString()

/** Kolom tanggal lama boleh '' (class_schedule): sama dengan tidak ada. */
private fun opsional(teks: String?): LocalDate? = if (teks.isNullOrEmpty()) null else LocalDate.parse(teks)

/** tgl memang kejadian seri: hari sama dan di dalam masa berlaku. */
private fun cocok(tgl: LocalDate, hari: Int, berlaku: Berlaku): Boolean {
    val (dari, sampai) = berlaku
    return tgl.dayOfWeek.value == hari && (dari == null || tgl >= dari) && (sampai == null || tgl <= sampai)
}

/** Tanggal di [d0, d1] ∩ masa berlaku yang harinya = hari (1 = Senin). */
private fun tanggalSeri(hari: Int, berlaku: Berlaku, d0: LocalDate, d1: LocalDate): Sequence<LocalDate> = sequence {
    val (dari, sampai) = berlaku
    val awal = if (dari != null && dari > d0) dari else d0
    val akhir = if (sampai != null && sampai < d1) sampai else d1
    var d = awal.plusDays(Math.floorMod(hari - awal.dayOfWeek.value, 7).toLong())
    while (d <= akhir) {
        yield(d)
        d = d.plusDays(7)
    }
}

/** id induk ke (tanggal_asli ke baris pengecualian); kolom = "acara_id" atau "class_id". */
private fun Db.pengecualian(kolom: String): Map<Long, Map<String, Baris>> {
    val hasil = LinkedHashMap<Long, LinkedHashMap<String, Baris>>()
    for (p in semua("SELECT * FROM acara_pengecualian WHERE $kolom IS NOT NULL")) {
        hasil.getOrPut(p[kolom] as Long) { LinkedHashMap() }[p["tanggal_asli"] as String] = p
    }
    return hasil
}

/**
 * Kejadian satu seri mingguan. Acara: batal hilang, pindah hanya tampil di tempat baru.
 * Kuliah: batal dan slot asli yang dipindah tetap ada, dengan statusnya masing-masing.
 */
private fun seri(
    dasar: Kejadian,
    hari: Int,
    jamMulai: String,
    jamSelesai: String,
    berlaku: Berlaku,
    pengecualian: Map<String, Baris>,
    d0: LocalDate,
    d1: LocalDate,
    kuliah: Boolean,
): List<Kejadian> {
    val hasil = mutableListOf<Kejadian>()
    for (d in tanggalSeri(hari, berlaku, d0, d1)) {
        val t = d.toString()
        val p = pengecualian[t]
        if (p == null) {
            hasil += dasar.copy(tanggalAsli = t, mulai = utc(d, jamMulai), selesai = utc(d, jamSelesai))
        } else if (kuliah) {
            hasil += dasar.copy(
                tanggalAsli = t,
                mulai = utc(d, jamMulai),
                selesai = utc(d, jamSelesai),
                status = if (p["jenis"] == "batal") "batal" else "bekas_pindah",
                pindahKe = p["mulai"] as String?,
            )
        }
    }
    // Kejadian pindah tetap terikat tanggal_asli, tapi tempat barunya bisa di dalam rentang
    // walau tanggal aslinya di luar (atau sebaliknya). Saringan rentang ada di kejadian().
    for ((t, p) in pengecualian) {
        if (p["jenis"] == "pindah" && cocok(LocalDate.parse(t), hari, berlaku)) {
            hasil += dasar.copy(
                judul = (p["judul"] as String?).takeUnless { it.isNullOrEmpty() } ?: dasar.judul,
                lokasi = (p["lokasi"] as String?).takeUnless { it.isNullOrEmpty() } ?: dasar.lokasi,
                tanggalAsli = t,
                mulai = p["mulai"] as String,
                selesai = p["selesai"] as String,
                status = "dipindah",
            )
        }
    }
    return hasil
}

/** Lapisan Deadline: tugas terbuka yang punya deadline (aturan "terbuka" sama dengan daftar tugas). */
private fun Db.dariDeadline(): List<Kejadian> = daftarTugas().mapNotNull { t ->
    t.deadline?.let { dl ->
        Kejadian(
            sumber = "deadline", ref = t.id, tanggalAsli = tanggalWib(dl), mulai = dl, selesai = dl,
            judul = t.judul, kalender = "deadline", warna = null, peran = "info",
        )
    }
}

private fun Db.dariKuliah(d0: LocalDate, d1: LocalDate): List<Kejadian> {
    val baris = semua("SELECT * FROM class_schedule")
    fun kunci(k: Baris) = (k["kode_mk"] as String?).takeUnless { it.isNullOrEmpty() } ?: (k["matkul"] as String)
    val urut = baris.map(::kunci).toSortedSet().toList()   // warna sama untuk satu mata kuliah
    val pengecualian = pengecualian("class_id")
    return baris.flatMap { k ->
        val id = k["id"] as Long
        val dasar = Kejadian(
            sumber = "kuliah", ref = id.toString(), mulai = "", selesai = "", judul = k["matkul"] as String,
            lokasi = k["ruang"] as String?, kalender = "kuliah", warna = urut.indexOf(kunci(k)) % 7 + 1,
            peran = "sibuk", kodeMk = k["kode_mk"] as String?,
        )
        val berlaku = Berlaku(opsional(k["berlaku_dari"] as String?), opsional(k["berlaku_sampai"] as String?))
        seri(
            dasar, (k["hari"] as Long).toInt(), k["mulai"] as String, k["selesai"] as String, berlaku,
            pengecualian[id].orEmpty(), d0, d1, kuliah = true,
        )
    }
}

private fun Db.dariAcara(d0: LocalDate, d1: LocalDate): List<Kejadian> {
    val pengecualian = pengecualian("acara_id")
    val hasil = mutableListOf<Kejadian>()
    for (a in semua(SQL_ACARA)) {
        val id = a["id"] as Long
        val dasar = Kejadian(
            sumber = "acara", ref = id.toString(), mulai = "", selesai = "", judul = a["judul"] as String,
            lokasi = a["lokasi"] as String?, kalender = a["kal_kode"] as String, warna = null,
            peran = a["kal_peran"] as String,
        )
        when (a["jenis"]) {
            "sekali" -> {
                val m = a["mulai"] as String
                hasil += dasar.copy(tanggalAsli = tanggalWib(m), mulai = m, selesai = a["selesai"] as String)
            }
            "seharian" -> {
                val t0 = LocalDate.parse(a["tgl_mulai"] as String)
                val t1 = LocalDate.parse(a["tgl_selesai"] as String)
                hasil += dasar.copy(
                    tanggalAsli = a["tgl_mulai"] as String, mulai = utc(t0), selesai = utc(t1.plusDays(1)), seharian = true,
                )
            }
            else -> {
                val berlaku = Berlaku(LocalDate.parse(a["tgl_mulai"] as String), opsional(a["tgl_selesai"] as String?))
                hasil += seri(
                    dasar, (a["hari"] as Long).toInt(), a["jam_mulai"] as String, a["jam_selesai"] as String,
                    berlaku, pengecualian[id].orEmpty(), d0, d1, kuliah = false,
                )
            }
        }
    }
    return hasil
}

/**
 * Semua kejadian yang bersinggungan dengan tanggal WIB [dari, sampai] (inklusif), urut mulai,
 * seharian lebih dulu, judul, lalu sumber/ref/tanggal_asli supaya urutan selalu tetap. Acara
 * yang melewati tengah malam dikembalikan utuh sekali; memecahnya per hari ada di [perHari].
 */
fun Db.kejadian(dari: LocalDate, sampai: LocalDate): List<Kejadian> {
    periksaRentang(dari, sampai)
    val awal = utc(dari)
    val akhir = utc(sampai.plusDays(1))
    val gabungan = dariAcara(dari, sampai) + dariKuliah(dari, sampai) + dariDeadline()
    // Bersinggungan: mulai sebelum akhir, selesai sesudah awal. Deadline (titik waktu)
    // cukup mulai >= awal, supaya deadline tepat 00.00 WIB tetap masuk hari itu.
    return gabungan
        .filter { it.mulai < akhir && (it.selesai > awal || it.mulai >= awal) }
        .sortedWith(compareBy({ it.mulai }, { !it.seharian }, { it.judul }, { it.sumber }, { it.ref }, { it.tanggalAsli }))
}
