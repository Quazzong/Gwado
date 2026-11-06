package io.github.quazzong.gwado.segar

import io.github.quazzong.gwado.db.Db
import io.github.quazzong.gwado.waktu.WIB
import io.github.quazzong.gwado.waktu.dariDb
import java.time.Duration
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

/** Run terakhir satu sumber aktif dan waktu sukses terakhirnya (teks UTC database). */
data class SumberTerakhir(
    val id: String,
    val nama: String,
    val status: String?,
    val selesai: String?,
    val errorSingkat: String?,
    val suksesTerakhir: String?,
)

private const val SQL_SUMBER = """
SELECT s.id, s.nama, r.status, r.selesai, r.error_singkat,
       (SELECT max(selesai) FROM source_runs
         WHERE source_id = s.id AND status = 'success') AS sukses_terakhir
FROM sources s
LEFT JOIN source_runs r ON r.id = (SELECT id FROM source_runs WHERE source_id = s.id
                                   ORDER BY mulai DESC, id DESC LIMIT 1)
WHERE s.aktif = 1
ORDER BY s.id
"""

/** Run terakhir tiap sumber aktif, urut id sumber. */
fun Db.sumberTerakhir(): List<SumberTerakhir> = semua(SQL_SUMBER).map {
    SumberTerakhir(
        id = it["id"] as String,
        nama = it["nama"] as String,
        status = it["status"] as String?,
        selesai = it["selesai"] as String?,
        errorSingkat = it["error_singkat"] as String?,
        suksesTerakhir = it["sukses_terakhir"] as String?,
    )
}

/** Tingkat status sumber; kode = nama warnanya ("" untuk normal). */
enum class Tingkat(val kode: String) { NORMAL(""), KUNING("kuning"), MERAH("merah") }

/** Status satu sumber: tingkat, label singkat, dan pesan penjelasan (null kalau normal). */
data class StatusSumber(val id: String, val nama: String, val tingkat: Tingkat, val label: String, val pesan: String?)

/** Pesan semua sumber yang tidak normal; merah kalau ada yang merah, selain itu kuning. */
data class Banner(val tingkat: Tingkat, val pesan: List<String>)

/** Jam ambil terjadwal (WIB); dipakai juga penjadwal pengambilan otomatis. */
internal val JADWAL_AMBIL = listOf(5, 10, 15, 20)

/** Jadwal dianggap sudah lewat setelah jamnya ditambah tenggang ini. */
private val TENGGANG = Duration.ofMinutes(15)

private val PETUNJUK_TOKEN = mapOf("classroom" to "login ulang Google")
private val NAMA_PENDEK = mapOf("classroom" to "Classroom")

/** n jadwal ambil terakhir yang sudah lewat (ditambah tenggang), terbaru dulu. */
internal fun jadwalLewat(sekarang: Instant, n: Int = 2): List<Instant> {
    val hasil = mutableListOf<Instant>()
    var hari = sekarang.atOffset(WIB).toLocalDate()
    while (hasil.size < n) {
        for (jam in JADWAL_AMBIL.reversed()) {
            val t = hari.atTime(jam, 0).atOffset(WIB).toInstant()
            if (!t.plus(TENGGANG).isAfter(sekarang) && hasil.size < n) hasil += t
        }
        hari = hari.minusDays(1)
    }
    return hasil
}

/**
 * Status tiap sumber. Merah = run terakhir gagal. Kuning = run terakhir partial, belum
 * pernah ada data, atau tidak ada sukses sejak dua jadwal ambil terakhir (basi).
 */
fun statusSumber(baris: List<SumberTerakhir>, sekarang: Instant): List<StatusSumber> {
    val batasBasi = jadwalLewat(sekarang)[1]
    return baris.map { s ->
        val nama = NAMA_PENDEK[s.id] ?: s.nama
        val sukses = s.suksesTerakhir?.let(::dariDb)
        val data = sukses?.let { jamSingkat(it, sekarang) } ?: "belum ada"
        val galat = s.errorSingkat.orEmpty()
        fun jamSelesai() = jamSingkat(dariDb(requireNotNull(s.selesai)), sekarang)
        when {
            s.status == null ->
                StatusSumber(s.id, nama, Tingkat.KUNING, "belum ada data", "$nama · belum ada data")
            s.status == "failed" && galat.startsWith("token_ditolak") -> {
                val petunjuk = PETUNJUK_TOKEN[s.id] ?: "ambil ulang token lewat browser"
                StatusSumber(s.id, nama, Tingkat.MERAH, "token ditolak", "$nama · token ditolak — $petunjuk")
            }
            s.status == "failed" ->
                StatusSumber(s.id, nama, Tingkat.MERAH, "gagal", "$nama · gagal diambil ${jamSelesai()} · data terakhir $data")
            s.status == "partial" ->
                StatusSumber(s.id, nama, Tingkat.KUNING, "perlu dicek",
                    "$nama · hasil ${jamSelesai()} mencurigakan · data lama tetap dipakai")
            sukses == null || sukses.isBefore(batasBasi) ->
                StatusSumber(s.id, nama, Tingkat.KUNING, "basi", "$nama · data basi · sukses terakhir $data")
            else -> StatusSumber(s.id, nama, Tingkat.NORMAL, data, null)
        }
    }
}

/** null kalau semua normal. tambahan = pesan lain di luar status sumber. */
fun banner(status: List<StatusSumber>, tambahan: List<String> = emptyList()): Banner? {
    val pesan = status.mapNotNull { it.pesan } + tambahan
    if (pesan.isEmpty()) return null
    return Banner(if (status.any { it.tingkat == Tingkat.MERAH }) Tingkat.MERAH else Tingkat.KUNING, pesan)
}
