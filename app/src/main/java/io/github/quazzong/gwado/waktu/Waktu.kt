package io.github.quazzong.gwado.waktu

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** WIB = UTC+7 tetap, tanpa pergeseran musim panas. */
val WIB: ZoneOffset = ZoneOffset.ofHours(7)

private val HARI = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
private val BULAN = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

/** Makna chip sisa waktu: lewat (merah), mepet (kuning), normal. */
enum class Makna { LEWAT, MEPET, NORMAL }

data class Chip(val label: String, val makna: Makna)

/** Teks waktu database ("2026-10-18T13:45:00Z") menjadi Instant. */
fun dariDb(teks: String): Instant = Instant.parse(teks)

private const val SEHARI = 24 * 3600L

/** Sisa kurang dari ini = mepet, apa pun tanggalnya. */
private const val BATAS_MEPET = 2 * SEHARI

/**
 * Label chip sisa waktu. Di bawah 1 jam dalam menit, di bawah 24 jam dalam jam (juga yang
 * melewati tengah malam, jadi deadline 00.03 tidak pernah tertulis "besok"); selebihnya per
 * tanggal WIB ("besok", "N hari", "lewat N hari"). Makna mepet = sisa kurang dari 48 jam,
 * dihitung dari waktu, bukan tanggal. Label huruf kecil; tampilan yang mengkapitalkan.
 */
fun sisaChip(deadline: Instant, sekarang: Instant): Chip {
    val hari = ChronoUnit.DAYS.between(sekarang.atOffset(WIB).toLocalDate(), deadline.atOffset(WIB).toLocalDate())
    val detik = Duration.between(sekarang, deadline).abs().seconds
    val lewat = deadline.isBefore(sekarang)
    if (detik < 3600) {
        val durasi = if (detik >= 60) "${detik / 60} menit" else "< 1 menit"
        return if (lewat) Chip("lewat $durasi", Makna.LEWAT) else Chip(durasi, Makna.MEPET)
    }
    return when {
        lewat -> Chip(if (detik < SEHARI) "lewat ${detik / 3600} jam" else "lewat ${-hari} hari", Makna.LEWAT)
        detik < SEHARI -> Chip("${detik / 3600} jam", Makna.MEPET)
        else -> Chip(if (hari == 1L) "besok" else "$hari hari", if (detik < BATAS_MEPET) Makna.MEPET else Makna.NORMAL)
    }
}

/** "Sen 3 Nov 23.59", atau "Sen 3 Nov 2026" tanpa jam (WIB, tanggal tanpa nol di depan). */
fun formatTanggal(waktu: Instant, jam: Boolean = true): String {
    val w = waktu.atOffset(WIB)
    val teks = "${HARI[w.dayOfWeek.value - 1]} ${w.dayOfMonth} ${BULAN[w.monthValue - 1]}"
    if (!jam) return "$teks ${w.year}"
    val hh = w.hour.toString().padStart(2, '0')
    val mm = w.minute.toString().padStart(2, '0')
    return "$teks $hh.$mm"
}
