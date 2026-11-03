package io.github.quazzong.gwado.segar

import io.github.quazzong.gwado.waktu.WIB
import java.time.Instant
import java.time.temporal.ChronoUnit

private val HARI3 = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
private val BULAN3 = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

/** Label singkat waktu pengambilan (WIB): hari ini "10.02", minggu ini "Sel 20.01", lebih lama "14 Okt 20.01". */
fun jamSingkat(waktu: Instant, sekarang: Instant): String {
    val d = waktu.atOffset(WIB)
    val n = sekarang.atOffset(WIB)
    val jam = d.hour.toString().padStart(2, '0') + "." + d.minute.toString().padStart(2, '0')
    val selisih = ChronoUnit.DAYS.between(d.toLocalDate(), n.toLocalDate())
    return when {
        selisih == 0L -> jam
        selisih < 7 -> "${HARI3[d.dayOfWeek.value - 1]} $jam"
        else -> "${d.dayOfMonth} ${BULAN3[d.monthValue - 1]} $jam"
    }
}
