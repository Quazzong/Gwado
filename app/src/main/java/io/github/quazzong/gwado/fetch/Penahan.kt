package io.github.quazzong.gwado.fetch

import io.github.quazzong.gwado.sumber.HasilSumber
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// Terjemahan penahan di serper/fetch.py (B7c3): jendela hindari per sumber, lalu cooldown 30 menit.
// Dicek di dalam job. Khusus Gwado: tombol manual hanya kena jeda pendek (JEDA_MANUAL), bukan
// cooldown 30 menit; jendela hindari tetap berlaku untuk semua.

val COOLDOWN: Duration = Duration.ofMinutes(30)

/** Gwado saja: jeda antar-ambil manual, mencegah ketukan beruntun. Bukan bagian kontrak SERPER. */
val JEDA_MANUAL: Duration = Duration.ofMinutes(1)

/** Jam WIB [mulai, selesai), boleh melewati tengah malam. Brone: backup situs, lambat sampai timeout. */
val JENDELA_HINDARI: Map<String, Pair<Int, Int>> = mapOf("brone" to (23 to 2))

private val WIB = ZoneOffset.ofHours(7)

/** Waktu UTC ke detik, bentuk yang sama dengan file status SERPER: 2026-10-21T03:00:05Z. */
internal fun iso(t: Instant): String = DateTimeFormatter.ISO_INSTANT.format(t.truncatedTo(ChronoUnit.SECONDS))

fun diJendelaHindari(sumber: String, sekarang: Instant): Boolean {
    val (mulai, selesai) = JENDELA_HINDARI[sumber] ?: return false
    val jam = sekarang.atOffset(WIB).hour
    return if (mulai > selesai) jam >= mulai || jam < selesai else jam in mulai until selesai
}

/** Waktu sukses terakhir dari teks yang tersimpan, atau null kalau tidak ada/rusak. */
fun suksesTerakhir(teks: String?): Instant? {
    if (teks == null) return null
    return try {
        OffsetDateTime.parse(teks.trim()).toInstant()
    } catch (e: DateTimeParseException) {
        null
    }
}

/** Sukses terakhir kalau belum lewat `batas`, selain itu null. Waktu di masa depan diabaikan. */
fun masihCooldown(statusTeks: String?, sekarang: Instant, batas: Duration = COOLDOWN): Instant? {
    val t = suksesTerakhir(statusTeks) ?: return null
    val selisih = Duration.between(t, sekarang)
    return if (!selisih.isNegative && selisih < batas) t else null
}

enum class SebabTertahan(val kode: String) {
    JENDELA_HINDARI("jendela_hindari"),
    COOLDOWN("cooldown"),
}

sealed interface HasilJob {
    /** bolehLagi: kapan boleh dicoba lagi; null untuk jendela hindari (tunggu jendelanya selesai). */
    data class Tertahan(val sebab: SebabTertahan, val suksesTerakhir: Instant?, val bolehLagi: Instant?) : HasilJob
    data class Selesai(val hasil: HasilSumber) : HasilJob
}

/** Penyimpanan waktu sukses per sumber (SERPER: satu file per sumber; Gwado: SharedPreferences). */
interface CatatanSukses {
    fun baca(sumber: String): String?
    fun tulis(sumber: String, waktuTeks: String)
}

/** Jendela hindari dulu, lalu cooldown (otomatis) atau jeda manual. null = boleh menghubungi situs. */
fun tertahan(sumber: String, statusTeks: String?, sekarang: Instant, manual: Boolean = false): HasilJob.Tertahan? {
    if (diJendelaHindari(sumber, sekarang)) return HasilJob.Tertahan(SebabTertahan.JENDELA_HINDARI, null, null)
    val batas = if (manual) JEDA_MANUAL else COOLDOWN
    val t = masihCooldown(statusTeks, sekarang, batas) ?: return null
    return HasilJob.Tertahan(SebabTertahan.COOLDOWN, t, t.plus(batas))
}

/**
 * job() di fetch.py: tertahan → situs tidak dihubungi. Gagal (FetchError) diteruskan dan tidak
 * dicatat, jadi bisa langsung dicoba lagi. Waktu sukses dicatat setelah run sukses, manual maupun otomatis.
 */
fun job(
    sumber: String,
    catatan: CatatanSukses,
    jam: () -> Instant = { Instant.now() },
    manual: Boolean = false,
    jalankan: () -> HasilSumber,
): HasilJob {
    tertahan(sumber, catatan.baca(sumber), jam(), manual)?.let { return it }
    val hasil = jalankan()
    catatan.tulis(sumber, iso(jam()))
    return HasilJob.Selesai(hasil)
}
