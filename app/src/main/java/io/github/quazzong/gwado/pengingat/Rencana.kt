package io.github.quazzong.gwado.pengingat

import io.github.quazzong.gwado.beranda.beres
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.waktu.WIB
import io.github.quazzong.gwado.waktu.dariDb
import io.github.quazzong.gwado.waktu.formatTanggal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Jenis pengingat, urut dari yang paling awal sebelum deadline. */
enum class Jenis(val kode: String, val sebelum: Duration?) {
    H3("h3", Duration.ofDays(3)),
    H1("h1", Duration.ofDays(1)),
    ALARM("alarm", Duration.ofHours(3)),
    PAGI("pagi", null),
    ;

    companion object {
        fun dariKode(kode: String?): Jenis? = entries.firstOrNull { it.kode == kode }
    }
}

/** Jam ringkasan pagi (WIB). */
val JAM_PAGI: LocalTime = LocalTime.of(7, 0)

/**
 * Pengingat yang waktunya sudah lewat lebih lama dari ini tidak ditampilkan lagi
 * (misalnya HP mati semalaman): cukup untuk alarm yang tertunda, tidak sampai basi.
 */
val TOLERANSI: Duration = Duration.ofHours(2)

private const val AWALAN_TENGGAT = "tenggat:"
private const val AWALAN_PAGI = "pagi:"

/**
 * Satu notifikasi: [tag] mengenali notifikasinya di panel. Tugas dengan deadline di menit yang
 * sama berbagi satu tag, jadi H-1 menggantikan H-3 dan alarm menggantikan H-1. Ringkasan pagi
 * punya tag per tanggal.
 */
data class Pengingat(val jenis: Jenis, val waktu: Instant, val tag: String, val tugas: List<BarisTugas>) {
    /** Penanda "sudah dikirim": jenis + tag. */
    val kunci: String get() = "${jenis.kode}:${tag.substringAfter(':')}"
}

/** Tugas yang perlu diingatkan: belum beres dan punya deadline. */
private fun tertunda(daftar: List<BarisTugas>) = daftar.filter { !beres(it) && it.deadline != null }

private fun menit(t: BarisTugas): Instant = dariDb(t.deadline!!).truncatedTo(ChronoUnit.MINUTES)

private fun tagTenggat(deadline: Instant) = "$AWALAN_TENGGAT${deadline.epochSecond}"
private fun tagPagi(hari: LocalDate) = "$AWALAN_PAGI$hari"
private fun waktuPagi(hari: LocalDate): Instant = hari.atTime(JAM_PAGI).toInstant(WIB)

/** Deadline sebuah tag tenggat; null untuk tag lain. */
fun deadlineTag(tag: String): Instant? =
    tag.removePrefix(AWALAN_TENGGAT).takeIf { it != tag }?.toLongOrNull()?.let(Instant::ofEpochSecond)

/** Tag ini milik pengingat Gwado. */
fun tagPengingat(tag: String?): Boolean =
    tag != null && (tag.startsWith(AWALAN_TENGGAT) || tag.startsWith(AWALAN_PAGI))

/**
 * Tugas yang sekarang termasuk sebuah tag: tenggat = deadline di menit itu; pagi = deadline
 * di tanggal itu setelah jam ringkasan. Urut deadline seperti daftar.
 */
fun anggota(tag: String, daftar: List<BarisTugas>): List<BarisTugas> {
    val tunda = tertunda(daftar)
    deadlineTag(tag)?.let { dl -> return tunda.filter { menit(it) == dl } }
    val hari = runCatching { LocalDate.parse(tag.removePrefix(AWALAN_PAGI)) }.getOrNull() ?: return emptyList()
    val pagi = waktuPagi(hari)
    return tunda.filter { val d = dariDb(it.deadline!!); d.atOffset(WIB).toLocalDate() == hari && d.isAfter(pagi) }
}

/** Semua pengingat dari daftar tugas, tanpa melihat sudah lewat atau belum. */
fun semuaPengingat(daftar: List<BarisTugas>): List<Pengingat> {
    val tunda = tertunda(daftar)
    val tenggat = tunda.groupBy(::menit).flatMap { (dl, tugas) ->
        Jenis.entries.mapNotNull { j -> j.sebelum?.let { Pengingat(j, dl.minus(it), tagTenggat(dl), tugas) } }
    }
    val pagi = tunda.map { dariDb(it.deadline!!).atOffset(WIB).toLocalDate() }.distinct().mapNotNull { hari ->
        val tag = tagPagi(hari)
        anggota(tag, daftar).takeIf { it.isNotEmpty() }?.let { Pengingat(Jenis.PAGI, waktuPagi(hari), tag, it) }
    }
    return (tenggat + pagi).sortedWith(compareBy({ it.waktu }, { it.tag }))
}

/** Hasil pengecekan: notifikasi yang ditampilkan sekarang + penanda terkirim yang baru. */
data class Tempo(val tampil: List<Pengingat>, val terkirim: Set<String>)

/**
 * Pengingat yang waktunya sudah tiba (paling lama [TOLERANSI] lalu) dan belum dikirim. Per tag
 * hanya yang terbaru, dan tenggat yang deadline-nya sudah lewat dilewati. Semua jenis yang sudah
 * tiba di tag yang ditampilkan ikut ditandai terkirim. Penanda yang waktunya lewat dari
 * toleransi dibuang, karena tidak mungkin tiba lagi.
 */
fun jatuhTempo(daftar: List<BarisTugas>, sekarang: Instant, terkirim: Set<String>): Tempo {
    val batas = sekarang.minus(TOLERANSI)
    val semua = semuaPengingat(daftar)
    val tampil = semua
        .filter { it.waktu.isAfter(batas) && !it.waktu.isAfter(sekarang) && it.kunci !in terkirim }
        .filter { p -> deadlineTag(p.tag)?.isAfter(sekarang) ?: true }
        .groupBy { it.tag }.values.map { it.maxBy(Pengingat::waktu) }
        .sortedWith(compareBy({ it.waktu }, { it.tag }))
    val tagTampil = tampil.map { it.tag }.toSet()
    val baru = semua.filter { it.tag in tagTampil && !it.waktu.isAfter(sekarang) }.map { it.kunci }
    val tetap = (terkirim + baru).filter { k -> waktuKunci(k)?.isAfter(batas) ?: false }.toSet()
    return Tempo(tampil, tetap)
}

/** Waktu pengingat dari penandanya; null kalau penanda tidak dikenal. */
internal fun waktuKunci(kunci: String): Instant? {
    val jenis = Jenis.dariKode(kunci.substringBefore(':')) ?: return null
    val isi = kunci.substringAfter(':')
    return if (jenis == Jenis.PAGI) {
        runCatching { waktuPagi(LocalDate.parse(isi)) }.getOrNull()
    } else {
        isi.toLongOrNull()?.let { Instant.ofEpochSecond(it).minus(jenis.sebelum) }
    }
}

/** Waktu pengingat berikutnya setelah [sekarang]; null kalau tidak ada. */
fun berikutnya(daftar: List<BarisTugas>, sekarang: Instant): Pengingat? =
    semuaPengingat(daftar).firstOrNull { it.waktu.isAfter(sekarang) }

/** Isi notifikasi: judul, satu baris ringkas, dan baris per tugas untuk tampilan yang dibuka. */
data class TeksPengingat(val judul: String, val isi: String, val baris: List<String>)

private fun jam(t: BarisTugas): String = formatTanggal(dariDb(t.deadline!!)).substringAfterLast(' ')

/** Teks notifikasi sebuah pengingat. */
fun teks(p: Pengingat): TeksPengingat {
    val n = p.tugas.size
    if (p.jenis == Jenis.PAGI) {
        val baris = p.tugas.map { "${jam(it)} ${it.judul}" }
        return TeksPengingat("$n tugas deadline hari ini", baris.joinToString(", "), baris)
    }
    val label = when (p.jenis) {
        Jenis.H3 -> "3 hari lagi"
        Jenis.H1 -> "besok"
        else -> "3 jam lagi"
    }
    val tanggal = formatTanggal(deadlineTag(p.tag) ?: dariDb(p.tugas.first().deadline!!))
    if (n == 1) {
        val t = p.tugas.first()
        val isi = listOfNotNull("Deadline $label, $tanggal", t.matkul).joinToString(" · ")
        return TeksPengingat(t.judul, isi, listOf(isi))
    }
    val baris = p.tugas.map { t -> listOfNotNull(t.judul, t.matkul).joinToString(" · ") }
    return TeksPengingat("$n tugas deadline $label", "$tanggal: ${p.tugas.joinToString(", ") { it.judul }}", baris)
}
