package io.github.quazzong.gwado.kalender

import io.github.quazzong.gwado.waktu.WIB
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Deadline pada satu hari, menit = menit sejak 00.00 WIB. */
data class DeadlineHari(val kejadian: Kejadian, val menit: Int)

/**
 * Potongan kejadian berjam pada satu hari (menit sejak 00.00 WIB). Kejadian yang
 * bertabrakan dibagi ke lajur 0 until jumlahLajur dalam klasternya.
 */
data class Berjam(
    val kejadian: Kejadian,
    val menitMulai: Int,
    val menitSelesai: Int,
    val dariKemarin: Boolean,
    val keBesok: Boolean,
    val lajur: Int,
    val jumlahLajur: Int,
)

data class Hari(
    val tanggal: LocalDate,
    val seharian: List<Kejadian>,
    val deadline: List<DeadlineHari>,
    val berjam: List<Berjam>,
)

/** Teks UTC database → jam dinding WIB. */
private fun keWib(utc: String): LocalDateTime = LocalDateTime.ofInstant(Instant.parse(utc), WIB)

private fun menit(dari: LocalDateTime, ke: LocalDateTime): Int =
    Math.floorDiv(Duration.between(dari, ke).seconds, 60L).toInt()

private class Potongan(
    val kejadian: Kejadian,
    val mulai: Int,
    val selesai: Int,
    val dariKemarin: Boolean,
    val keBesok: Boolean,
) {
    var lajur = 0
    var jumlah = 0
}

/** Isi lajur dan jumlah lajur per klaster potongan yang saling bersinggungan. */
private fun lajur(potongan: List<Potongan>): List<Potongan> {
    val urut = potongan.sortedBy { it.mulai }   // stabil: urutan kejadian() tetap
    var klaster = mutableListOf<Potongan>()
    var ujungLajur = mutableListOf<Int>()
    var ujung = 0
    for (p in urut + listOf<Potongan?>(null)) {
        if (p == null || (klaster.isNotEmpty() && p.mulai >= ujung)) {
            klaster.forEach { it.jumlah = ujungLajur.size }
            klaster = mutableListOf()
            ujungLajur = mutableListOf()
        }
        if (p == null) break
        val i = ujungLajur.indexOfFirst { it <= p.mulai }
        if (i >= 0) {
            p.lajur = i
            ujungLajur[i] = p.selesai
        } else {
            p.lajur = ujungLajur.size
            ujungLajur += p.selesai
        }
        ujung = if (klaster.isNotEmpty()) maxOf(ujung, p.selesai) else p.selesai
        klaster += p
    }
    return urut
}

/**
 * Kejadian per tanggal WIB untuk tampilan: seharian, deadline, berjam. Kejadian berjam yang
 * melewati tengah malam dipecah per hari (dariKemarin/keBesok).
 */
fun perHari(daftar: List<Kejadian>, dari: LocalDate, sampai: LocalDate): List<Hari> {
    periksaRentang(dari, sampai)
    return (0..ChronoUnit.DAYS.between(dari, sampai)).map { n ->
        val t = dari.plusDays(n)
        val awal = t.atStartOfDay()
        val akhir = awal.plusDays(1)
        val seharian = mutableListOf<Kejadian>()
        val deadline = mutableListOf<DeadlineHari>()
        val berjam = mutableListOf<Potongan>()
        for (k in daftar) {
            val m = keWib(k.mulai)
            val s = keWib(k.selesai)
            if (k.sumber == "deadline") {
                if (m >= awal && m < akhir) deadline += DeadlineHari(k, menit(awal, m))
            } else if (m < akhir && s > awal) {
                if (k.seharian) {
                    seharian += k
                } else {
                    berjam += Potongan(
                        k,
                        menit(awal, if (m > awal) m else awal),
                        menit(awal, if (s < akhir) s else akhir),
                        dariKemarin = m < awal,
                        keBesok = s > akhir,
                    )
                }
            }
        }
        Hari(
            t, seharian, deadline,
            lajur(berjam).map { Berjam(it.kejadian, it.mulai, it.selesai, it.dariKemarin, it.keBesok, it.lajur, it.jumlah) },
        )
    }
}
