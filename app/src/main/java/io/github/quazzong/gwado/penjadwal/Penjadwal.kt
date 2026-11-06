package io.github.quazzong.gwado.penjadwal

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.fetch.HasilJob
import io.github.quazzong.gwado.fetch.ambilClassroom
import io.github.quazzong.gwado.fetch.ambilMoodle
import io.github.quazzong.gwado.fetch.iso
import io.github.quazzong.gwado.layanan.CatatanPrefs
import io.github.quazzong.gwado.layanan.ambilDanCatat
import io.github.quazzong.gwado.layanan.daftarTersambung
import io.github.quazzong.gwado.layanan.prefsGwado
import io.github.quazzong.gwado.layanan.tokenGoogleDiam
import io.github.quazzong.gwado.masuk.BrankasKeystore
import io.github.quazzong.gwado.semester.semesterTersimpan
import io.github.quazzong.gwado.waktu.dariDb
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException

private const val NAMA_KERJA = "ambil_otomatis"
private const val TAG = "Gwado"

/** Kunci SharedPreferences: ringkasan putaran Ambil otomatis terakhir (tanpa token). */
internal const val KUNCI_OTOMATIS = "otomatis_terakhir"

/** Selang pengecekan. Situs hanya dihubungi kalau sudah ada jadwal ambil yang terlewat. */
private val SELANG = Duration.ofHours(1)

/** Pasang pengecekan berkala Ambil otomatis (butuh internet). Yang sudah terpasang dibiarkan. */
fun pasangPenjadwal(context: Context) {
    val permintaan = PeriodicWorkRequestBuilder<PekerjaAmbil>(SELANG)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(NAMA_KERJA, ExistingPeriodicWorkPolicy.KEEP, permintaan)
}

/** Pekerjaan latar: satu putaran Ambil otomatis, hasilnya dicatat singkat di log dan SharedPreferences. */
class PekerjaAmbil(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val catatan = try {
            ambilOtomatis(applicationContext, Instant.now())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            listOf("gagal ${e.javaClass.simpleName}")
        }
        val ringkas = catatan.joinToString("; ")
        Log.i(TAG, "Ambil otomatis: $ringkas")
        prefsGwado(applicationContext).edit()
            .putString(KUNCI_OTOMATIS, "${iso(Instant.now())} $ringkas").apply()
        return Result.success()
    }
}

/**
 * Satu putaran: tiap layanan tersambung yang belum diambil sejak jadwal terakhir, berurutan,
 * dengan cooldown 30 menit. Tidak pernah membuka browser atau layar izin: Moodle tanpa token
 * dan Classroom yang butuh persetujuan dilewati. Hasilnya satu catatan per layanan.
 */
internal suspend fun ambilOtomatis(context: Context, sekarang: Instant): List<String> {
    if (Gudang.pakai(context) { it.semesterTersimpan() } == null) return listOf("semester belum dikonfirmasi")
    val prefs = prefsGwado(context)
    val brankas = BrankasKeystore(context)
    val catatan = CatatanPrefs(prefs)
    val hasil = daftarTersambung(context, brankas, prefs).map { info ->
        val l = info.layanan
        val situs = l.situs
        val teks = try {
            when {
                !perluAmbil(info.baris.selesai?.let(::dariDb), sekarang) -> "belum waktunya"
                situs != null -> {
                    val token = brankas.baca(l.id)
                    if (token == null) {
                        "perlu login"
                    } else {
                        tercatat(ambilDanCatat(context, l, catatan, brankas, manual = false) {
                            ambilMoodle(situs, token, Instant.now())
                        })
                    }
                }
                else -> {
                    val akses = tokenGoogleDiam(context)
                    if (akses == null) {
                        "perlu izin Google"
                    } else {
                        tercatat(ambilDanCatat(context, l, catatan, brankas, manual = false) {
                            ambilClassroom(akses, Instant.now())
                        })
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "gagal ${e.javaClass.simpleName}"
        }
        "${l.id} $teks"
    }
    return hasil.ifEmpty { listOf("belum ada layanan tersambung") }
}

private fun tercatat(t: HasilJob.Tertahan?): String = if (t == null) "run tercatat" else "ditahan ${t.sebab.kode}"
