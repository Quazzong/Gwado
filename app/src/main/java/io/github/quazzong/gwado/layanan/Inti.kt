package io.github.quazzong.gwado.layanan

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.fetch.CatatanSukses
import io.github.quazzong.gwado.fetch.FetchError
import io.github.quazzong.gwado.fetch.HasilJob
import io.github.quazzong.gwado.fetch.job
import io.github.quazzong.gwado.ingest.isiGagal
import io.github.quazzong.gwado.ingest.isiSukses
import io.github.quazzong.gwado.ingest.proses
import io.github.quazzong.gwado.masuk.BrankasToken
import io.github.quazzong.gwado.pengingat.perbaruiPengingat
import io.github.quazzong.gwado.segar.sumberTerakhir
import io.github.quazzong.gwado.sumber.HasilSumber
import io.github.quazzong.gwado.widget.perbaruiWidget
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

// Bagian alur Ambil yang tidak butuh layar: dipakai tombol Ambil dan pengambilan otomatis di latar.

internal val SCOPES = listOf(
    Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
    Scope("https://www.googleapis.com/auth/classroom.coursework.me.readonly"),
)

/** Kunci SharedPreferences: izin Classroom pernah diberikan di HP ini. */
internal const val KUNCI_IZIN_GOOGLE = "izin_classroom"

/**
 * SharedPreferences Gwado. Namanya sama dengan Activity.getPreferences di MainActivity,
 * jadi isi yang sudah tersimpan tetap terbaca dari Activity maupun dari latar.
 */
internal fun prefsGwado(context: Context): SharedPreferences =
    context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE)

/** Waktu sukses terakhir per sumber, untuk penahan. */
internal class CatatanPrefs(private val prefs: SharedPreferences) : CatatanSukses {
    override fun baca(sumber: String) = prefs.getString("sukses_$sumber", null)
    override fun tulis(sumber: String, waktuTeks: String) {
        prefs.edit().putString("sukses_$sumber", waktuTeks).apply()
    }
}

/** Akses tersimpan: token Moodle di brankas, atau izin Classroom yang pernah diberikan. */
internal fun adaAkses(l: Layanan, brankas: BrankasToken, prefs: SharedPreferences): Boolean =
    if (l.situs != null) brankas.baca(l.id) != null else prefs.getBoolean(KUNCI_IZIN_GOOGLE, false)

/** Sumber aktif yang punya akses tersimpan atau sudah pernah diambil, urut daftar layanan. */
internal suspend fun daftarTersambung(
    context: Context,
    brankas: BrankasToken,
    prefs: SharedPreferences,
): List<InfoLayanan> {
    val baris = Gudang.pakai(context) { it.sumberTerakhir() }.associateBy { it.id }
    return DAFTAR_LAYANAN.mapNotNull { l ->
        baris[l.id]?.let { b -> InfoLayanan(l, b, adaAkses(l, brankas, prefs)) }
    }.filter { it.baris.status != null || it.adaToken }
}

internal fun permintaanIzin(): AuthorizationRequest =
    AuthorizationRequest.builder().setRequestedScopes(SCOPES).build()

/** Access token Google tanpa layar; null kalau Google meminta persetujuan pengguna lagi. */
internal suspend fun tokenGoogleDiam(context: Context): String? {
    val r = tunggu(Identity.getAuthorizationClient(context).authorize(permintaanIzin()))
    if (r.hasResolution()) return null
    return r.accessToken?.takeIf { it.isNotEmpty() }
}

/**
 * Fetch lewat penahan, lalu Isi dan ingest. Hasilnya Tertahan kalau situs tidak dihubungi,
 * null kalau run tercatat (sukses maupun gagal). Token Moodle yang ditolak situs dihapus.
 */
internal suspend fun ambilDanCatat(
    context: Context,
    l: Layanan,
    catatan: CatatanSukses,
    brankas: BrankasToken,
    manual: Boolean,
    jalankan: () -> HasilSumber,
): HasilJob.Tertahan? {
    val mulai = Instant.now()
    val isi = try {
        when (val j = withContext(Dispatchers.IO) { job(l.id, catatan, manual = manual, jalankan = jalankan) }) {
            is HasilJob.Tertahan -> return j
            is HasilJob.Selesai -> isiSukses(l.id, mulai, Instant.now(), j.hasil)
        }
    } catch (e: FetchError) {
        if (e.jenis == "token_ditolak" && l.situs != null) brankas.hapus(l.id)
        isiGagal(l.id, mulai, Instant.now(), e)                    // run gagal tetap dicatat
    }
    Gudang.pakai(context) { it.proses(isi) }
    perbaruiWidget(context)
    perbaruiPengingat(context)
    return null
}

/** Tunggu hasil Task Play services di dalam coroutine. */
internal suspend fun <T> tunggu(t: Task<T>): T = suspendCancellableCoroutine { k ->
    t.addOnSuccessListener { k.resume(it) }
        .addOnFailureListener { k.resumeWithException(it) }
}
