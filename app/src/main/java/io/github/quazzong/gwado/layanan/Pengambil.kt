package io.github.quazzong.gwado.layanan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.fetch.CatatanSukses
import io.github.quazzong.gwado.fetch.FetchError
import io.github.quazzong.gwado.fetch.HasilJob
import io.github.quazzong.gwado.fetch.JENDELA_HINDARI
import io.github.quazzong.gwado.fetch.SebabTertahan
import io.github.quazzong.gwado.fetch.akhirJendela
import io.github.quazzong.gwado.fetch.ambilClassroom
import io.github.quazzong.gwado.fetch.ambilMoodle
import io.github.quazzong.gwado.fetch.job
import io.github.quazzong.gwado.fetch.tertahan
import io.github.quazzong.gwado.ingest.isiGagal
import io.github.quazzong.gwado.ingest.isiSukses
import io.github.quazzong.gwado.ingest.proses
import io.github.quazzong.gwado.masuk.BrankasKeystore
import io.github.quazzong.gwado.masuk.HasilLogin
import io.github.quazzong.gwado.masuk.alamatLogin
import io.github.quazzong.gwado.masuk.bacaRedirect
import io.github.quazzong.gwado.segar.SumberTerakhir
import io.github.quazzong.gwado.segar.sumberTerakhir
import io.github.quazzong.gwado.semester.semesterTersimpan
import io.github.quazzong.gwado.sumber.HasilSumber
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Layanan yang didukung. situs null = Classroom (token dari layar izin Google, bukan login SSO).
 * nama = nama lengkap di tabel sources; keterangan = satu baris di lembar Tambah layanan.
 */
class Layanan(
    val id: String,
    val label: String,
    val keterangan: String,
    val situs: String? = null,
    val nama: String = label,
)

val DAFTAR_LAYANAN = listOf(
    Layanan("brone", "Brone", "Tugas dan kuis kuliah dari Moodle UB", "https://brone.ub.ac.id"),
    Layanan("eling", "Eling", "Tugas praktikum dari Moodle UB", "https://eling.ub.ac.id"),
    Layanan("classroom", "Classroom", "Kelas yang dibuat dosen di Classroom", nama = "Google Classroom"),
)

private val SCOPES = listOf(
    Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
    Scope("https://www.googleapis.com/auth/classroom.coursework.me.readonly"),
)

/** Kunci SharedPreferences: izin Classroom pernah diberikan di HP ini. */
private const val KUNCI_IZIN_GOOGLE = "izin_classroom"

/** Status layanan yang sudah tersambung tapi belum boleh diambil. */
private const val TUNGGU_SEMESTER = "Tersambung. Tugas diambil setelah semester dikonfirmasi."

/** Gagal mendapat access token Google; pesannya aman ditampilkan. */
private class GagalIzin(pesan: String) : Exception(pesan)

/** Satu layanan tersambung: run terakhirnya dan apakah aksesnya tersimpan (token Moodle, atau izin Classroom). */
data class InfoLayanan(val layanan: Layanan, val baris: SumberTerakhir, val adaToken: Boolean)

/** Layanan yang tidak dihubungi karena penahan; [sampai] = kapan boleh dicoba lagi. */
data class Tahan(val sebab: SebabTertahan, val sampai: Instant)

/** Hasil satu Ambil semua: pesan yang tidak tercatat di database. nomor naik setiap selesai. */
data class HasilAmbil(val nomor: Int, val pesan: List<String>)

/**
 * Alur Ambil untuk layar: login Moodle lewat browser, izin Google, fetch, lalu ingest.
 * Dibuat sebagai properti Activity (sebelum onCreate), karena peluncur layar izin Google
 * harus didaftarkan sebelum Activity mulai. [onBerubah] dipanggil setiap kali database berubah.
 */
class Pengambil(private val activity: ComponentActivity, private val onBerubah: () -> Unit) {
    /** Teks status terakhir per layanan. */
    val status = mutableStateMapOf<String, String>()

    /** Pesan yang tidak terikat satu layanan. */
    var pesan by mutableStateOf("")
        private set

    var sibuk by mutableStateOf(false)
        private set

    var hasilTerakhir by mutableStateOf<HasilAmbil?>(null)
        private set

    /** Layanan yang terakhir kali ditahan (jendela hindari atau jeda), per id layanan. */
    val ditahan = mutableStateMapOf<String, Tahan>()

    /** Login Moodle terakhir yang berhasil: (id layanan, nomor urut). Layar login menutup diri saat nilainya berganti. */
    var loginBerhasil by mutableStateOf<Pair<String, Int>?>(null)
        private set

    private var nomorLogin = 0

    private var nomorAmbil = 0

    private val prefs by lazy { activity.getPreferences(Context.MODE_PRIVATE) }
    private val brankas by lazy { BrankasKeystore(activity) }
    private val catatan = object : CatatanSukses {
        override fun baca(sumber: String) = prefs.getString("sukses_$sumber", null)
        override fun tulis(sumber: String, waktuTeks: String) {
            prefs.edit().putString("sukses_$sumber", waktuTeks).apply()
        }
    }

    private var tungguIzin: CompletableDeferred<ActivityResult>? = null
    private val izinGoogle = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        tungguIzin?.complete(it)
    }

    /** Layanan tersambung (sumber aktif yang punya token tersimpan atau sudah pernah diambil), urut daftar layanan. */
    suspend fun daftarTersambung(): List<InfoLayanan> {
        val baris = Gudang.pakai(activity) { it.sumberTerakhir() }.associateBy { it.id }
        return DAFTAR_LAYANAN.mapNotNull { l ->
            baris[l.id]?.let { b -> InfoLayanan(l, b, adaAkses(l)) }
        }.filter { it.baris.status != null || it.adaToken }
    }

    suspend fun tersambung(): Set<String> = daftarTersambung().map { it.layanan.id }.toSet()

    private fun adaAkses(l: Layanan): Boolean =
        if (l.situs != null) brankas.baca(l.id) != null else prefs.getBoolean(KUNCI_IZIN_GOOGLE, false)

    /** Layanan yang didukung tapi belum tersambung (termasuk yang pernah dimatikan), urut daftar layanan. */
    suspend fun belumTersambung(): List<Layanan> {
        val ada = tersambung()
        return DAFTAR_LAYANAN.filter { it.id !in ada }
    }

    /** Sambungkan layanan: nyalakan lagi kalau pernah dimatikan, lalu Ambil. Moodle tanpa token membuka login. */
    fun tambah(l: Layanan) = luncurkan {
        status[l.id] = aman {
            Gudang.pakai(activity) { it.aturAktif(l.id, true) }
            satu(l, bolehLogin = true)
        }
        onBerubah()
    }

    /** Ambil satu layanan; Moodle tanpa token membuka login di browser. */
    fun ambil(l: Layanan) = luncurkan { status[l.id] = aman { satu(l, bolehLogin = true) } }

    /** Ambil semua layanan tersambung secara berurutan. Moodle tanpa token dilewati, browser tidak dibuka. */
    fun ambilSemua() = luncurkan {
        val siap = try {
            tersambung()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            pesan = "Database tidak bisa dibuka: ${e.javaClass.simpleName}"
            hasilTerakhir = HasilAmbil(++nomorAmbil, listOf(pesan))
            return@luncurkan
        }
        pesan = if (siap.isEmpty()) "Belum ada layanan yang tersambung" else ""
        val catatanAmbil = mutableListOf<String>()
        if (pesan.isNotEmpty()) catatanAmbil += pesan
        for (l in DAFTAR_LAYANAN) {
            if (l.id !in siap) continue
            val t = aman { satu(l, bolehLogin = false) }
            status[l.id] = t
            if (t.isNotBlank() && l.id !in ditahan) catatanAmbil += t      // yang ditahan tampil di kotaknya sendiri
        }
        hasilTerakhir = HasilAmbil(++nomorAmbil, catatanAmbil.distinct())
    }

    /** Hanya menghapus token Moodle; tugas yang sudah tersimpan di database tetap ada. */
    fun lupakan(l: Layanan) {
        brankas.hapus(l.id)
        status[l.id] = "Token ${l.label} dihapus dari HP"
        onBerubah()
    }

    /**
     * Matikan layanan: token Moodle dihapus atau izin Google dicabut, lalu sumbernya tidak
     * diambil lagi. Tugas yang sudah tersimpan tetap ada.
     */
    fun matikan(l: Layanan) = luncurkan {
        val catatanCabut = if (l.situs == null) {
            prefs.edit().remove(KUNCI_IZIN_GOOGLE).apply()
            cabutGoogle()
        } else {
            brankas.hapus(l.id)
            null
        }
        pesan = aman {
            Gudang.pakai(activity) { it.aturAktif(l.id, false) }
            catatanCabut ?: "${l.label} dimatikan. Tugas yang sudah diambil tetap ada."
        }
        status.remove(l.id)
        onBerubah()
    }

    /**
     * Alamat moodlemobile:// dari browser (atau ditempel): simpan token, lalu langsung ambil.
     * untuk = layanan di layar tempel, tempat pesan ditampilkan kalau belum ada login yang berjalan.
     */
    fun tangani(alamat: String, untuk: String? = null) {
        val l = DAFTAR_LAYANAN.find { it.id == prefs.getString("login_sumber", null) }
        val situs = l?.situs
        if (l == null || situs == null) {
            val teks = "Tidak ada login Moodle yang sedang berjalan. Tekan Buka halaman login dulu."
            if (untuk != null) status[untuk] = teks else pesan = teks
            return
        }
        when (val h = bacaRedirect(alamat, situs, prefs.getString("passport", "").orEmpty())) {
            is HasilLogin.Gagal -> status[l.id] = "Login gagal: ${h.alasan}"
            is HasilLogin.Berhasil -> {
                prefs.edit().remove("passport").remove("login_sumber").apply()   // passport sekali pakai
                pesan = try {
                    brankas.simpan(l.id, h.token)
                    ""
                } catch (e: Exception) {
                    "Token ${l.label} tidak bisa disimpan (${e.javaClass.simpleName}); dipakai sekali saja."
                }
                loginBerhasil = l.id to ++nomorLogin
                luncurkan {
                    status[l.id] = aman {
                        if (Gudang.pakai(activity) { it.semesterTersimpan() } == null) {
                            TUNGGU_SEMESTER
                        } else {
                            ambilDanSimpan(l) { ambilMoodle(situs, h.token, Instant.now()) }
                        }
                    }
                    onBerubah()
                }
            }
        }
    }

    /** Satu pekerjaan Ambil dalam satu waktu; ketukan saat sibuk diabaikan. */
    private fun luncurkan(kerja: suspend () -> Unit) {
        if (sibuk) return
        sibuk = true
        activity.lifecycleScope.launch {
            try {
                kerja()
            } finally {
                sibuk = false
            }
        }
    }

    /** Jalankan kerja dan ubah galat jadi teks status. Galat tak terduga tanpa pesannya (bisa memuat data). */
    private suspend fun aman(kerja: suspend () -> String): String = try {
        kerja()
    } catch (e: CancellationException) {
        throw e
    } catch (e: GagalIzin) {
        e.message.orEmpty()
    } catch (e: Exception) {
        "Gagal tak terduga: ${e.javaClass.simpleName}"
    }

    /**
     * Login atau izin dulu kalau belum ada, lalu ambil. Menyambungkan tidak butuh semester;
     * mengambil butuh semester yang sudah dikonfirmasi.
     */
    private suspend fun satu(l: Layanan, bolehLogin: Boolean): String {
        ditahan.remove(l.id)
        tertahan(l.id, catatan.baca(l.id), Instant.now(), manual = true)?.let { return pesanTertahan(l, it) }
        val adaSemester = Gudang.pakai(activity) { it.semesterTersimpan() } != null
        val situs = l.situs
        if (situs == null) {
            status[l.id] = "Meminta izin Google…"
            val akses = tokenGoogle()
            prefs.edit().putBoolean(KUNCI_IZIN_GOOGLE, true).apply()
            if (!adaSemester) return TUNGGU_SEMESTER
            return ambilDanSimpan(l) { ambilClassroom(akses, Instant.now()) }
        }
        val token = brankas.baca(l.id)
        return when {
            token != null && adaSemester -> ambilDanSimpan(l) { ambilMoodle(situs, token, Instant.now()) }
            token != null -> TUNGGU_SEMESTER
            bolehLogin -> mulaiLogin(l, situs)
            else -> "Perlu login: tekan Login di kartu ${l.label}"
        }
    }

    private fun mulaiLogin(l: Layanan, situs: String): String {
        val passport = UUID.randomUUID().toString()
        prefs.edit().putString("passport", passport).putString("login_sumber", l.id).apply()
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(alamatLogin(situs, passport))))
        return "Menunggu login ${l.label} di browser… (kalau ditanya \"Buka dengan\", pilih Gwado)"
    }

    /** Access token Google; layar izin hanya muncul kalau Google memintanya. */
    private suspend fun tokenGoogle(): String {
        val klien = Identity.getAuthorizationClient(activity)
        val permintaan = AuthorizationRequest.builder().setRequestedScopes(SCOPES).build()
        val r = try {
            tunggu(klien.authorize(permintaan))
        } catch (e: ApiException) {
            throw GagalIzin("Gagal meminta izin Google (kode ${e.statusCode})")
        }
        val pi = r.pendingIntent
        val hasil = if (r.hasResolution() && pi != null) {
            val tunggu = CompletableDeferred<ActivityResult>()
            tungguIzin = tunggu
            izinGoogle.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            val data = tunggu.await().data ?: throw GagalIzin("Layar izin Google ditutup tanpa jawaban")
            try {
                klien.getAuthorizationResultFromIntent(data)
            } catch (e: ApiException) {
                throw GagalIzin("Izin Google gagal (kode ${e.statusCode})")
            }
        } else {
            r
        }
        return hasil.accessToken?.takeIf { it.isNotEmpty() } ?: throw GagalIzin("Tidak dapat access token Google")
    }

    /**
     * Cabut izin Classroom tanpa layar. null kalau berhasil atau memang belum ada izin;
     * selain itu kalimat petunjuk untuk pengguna.
     */
    private suspend fun cabutGoogle(): String? {
        val manual = "Classroom dimatikan, tapi izin Google belum bisa dicabut dari sini. " +
            "Cabut lewat myaccount.google.com/permissions kalau perlu."
        val klien = Identity.getAuthorizationClient(activity)
        return try {
            val r = tunggu(klien.authorize(AuthorizationRequest.builder().setRequestedScopes(SCOPES).build()))
            if (r.hasResolution()) return null                 // belum ada izin: tidak ada yang dicabut
            val akun = r.toGoogleSignInAccount()?.account ?: return manual
            tunggu(klien.revokeAccess(RevokeAccessRequest.builder().setAccount(akun).setScopes(SCOPES).build()))
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            manual
        }
    }

    /** Fetch, Isi, ingest. Teks kosong kalau run tercatat; selain itu alasan situs tidak dihubungi. */
    private suspend fun ambilDanSimpan(l: Layanan, jalankan: () -> HasilSumber): String {
        status[l.id] = "Mengambil dari ${l.label}…"
        val mulai = Instant.now()
        val isi = try {
            when (val j = withContext(Dispatchers.IO) { job(l.id, catatan, manual = true, jalankan = jalankan) }) {
                is HasilJob.Tertahan -> return pesanTertahan(l, j)      // situs tidak dihubungi: tidak ada run
                is HasilJob.Selesai -> isiSukses(l.id, mulai, Instant.now(), j.hasil)
            }
        } catch (e: FetchError) {
            if (e.jenis == "token_ditolak" && l.situs != null) brankas.hapus(l.id)
            isiGagal(l.id, mulai, Instant.now(), e)                    // run gagal tetap dicatat
        }
        Gudang.pakai(activity) { it.proses(isi) }
        onBerubah()
        return ""   // hasil run tercatat di database dan tampil lewat status sumber
    }

    private fun pesanTertahan(l: Layanan, t: HasilJob.Tertahan): String {
        val sekarang = Instant.now()
        val sampai = if (t.sebab == SebabTertahan.JENDELA_HINDARI) akhirJendela(l.id, sekarang) else t.bolehLagi
        ditahan[l.id] = Tahan(t.sebab, sampai ?: sekarang)
        return teksTertahan(l, t)
    }

    private fun teksTertahan(l: Layanan, t: HasilJob.Tertahan): String = when (t.sebab) {
        SebabTertahan.JENDELA_HINDARI -> {
            val (m, sl) = JENDELA_HINDARI.getValue(l.id)
            "${l.label} sedang backup (%02d.00–%02d.00 WIB), situs tidak dihubungi. Coba lagi setelah %02d.00 WIB."
                .format(m, sl, sl)
        }
        SebabTertahan.COOLDOWN -> {
            val detik = Duration.between(Instant.now(), t.bolehLagi ?: Instant.now()).seconds.coerceAtLeast(1)
            val sisa = if (detik < 60) "$detik detik" else "${detik / 60 + 1} menit"
            "${l.label} baru saja diambil, situs tidak dihubungi. Coba lagi dalam $sisa."
        }
    }
}

/** Tunggu hasil Task Play services di dalam coroutine. */
private suspend fun <T> tunggu(t: Task<T>): T = suspendCancellableCoroutine { k ->
    t.addOnSuccessListener { k.resume(it) }
        .addOnFailureListener { k.resumeWithException(it) }
}
