package io.github.quazzong.gwado

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import io.github.quazzong.gwado.beranda.kuliahMulai
import io.github.quazzong.gwado.beranda.simpanKuliahMulai
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.fetch.CatatanSukses
import io.github.quazzong.gwado.fetch.FetchError
import io.github.quazzong.gwado.fetch.HasilJob
import io.github.quazzong.gwado.fetch.JENDELA_HINDARI
import io.github.quazzong.gwado.fetch.SebabTertahan
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
import io.github.quazzong.gwado.semester.LABEL_SEMESTER
import io.github.quazzong.gwado.semester.Matkul
import io.github.quazzong.gwado.semester.aturPengecualian
import io.github.quazzong.gwado.semester.daftarMatkul
import io.github.quazzong.gwado.semester.periksaSemester
import io.github.quazzong.gwado.semester.ringkasanMatkul
import io.github.quazzong.gwado.semester.semesterTersimpan
import io.github.quazzong.gwado.semester.simpanSemester
import io.github.quazzong.gwado.semester.tanggalWib
import io.github.quazzong.gwado.semester.tebakSemester
import io.github.quazzong.gwado.sumber.HasilSumber
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.ui.Akar
import io.github.quazzong.gwado.ui.tema.GwadoTema
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** situs null = Classroom (token dari AuthorizationClient, bukan login SSO). */
private class Sumber(val id: String, val label: String, val situs: String? = null)

private val DAFTAR_SUMBER = listOf(
    Sumber("brone", "Brone", "https://brone.ub.ac.id"),
    Sumber("eling", "Eling", "https://eling.ub.ac.id"),
    Sumber("classroom", "Classroom"),
)

private val SCOPES = listOf(
    Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
    Scope("https://www.googleapis.com/auth/classroom.coursework.me.readonly"),
)

private val PILIHAN_PENGECUALIAN: List<Pair<String?, String>> = listOf(
    null to "Otomatis",
    "ikuti" to "Ikuti",
    "abaikan" to "Abaikan",
)

class MainActivity : ComponentActivity() {
    private val status = mutableStateMapOf<String, String>()
    private var daftar by mutableStateOf<List<BarisTugas>>(emptyList())
    private var sibuk by mutableStateOf(false)
    private var umum by mutableStateOf("")
    private var alamatTempel by mutableStateOf("")
    private var isianKuliah by mutableStateOf("")
    private var versiData by mutableIntStateOf(0)
    private var semester by mutableStateOf<Map<String, String>?>(null)   // null = belum dikonfirmasi
    private val isianSemester = mutableStateMapOf<String, String>()
    private var lihatMatkul by mutableStateOf<String?>(null)              // sumber yang daftar matkulnya dibuka
    private var matkulSumber by mutableStateOf<List<Matkul>>(emptyList())
    private val matkul = mutableStateMapOf<String, String>()               // sumber → "N diikuti · M dilewati"
    private val prefs by lazy { getPreferences(MODE_PRIVATE) }
    private val brankas by lazy { BrankasKeystore(this) }
    private val catatan = object : CatatanSukses {
        override fun baca(sumber: String) = prefs.getString("sukses_$sumber", null)
        override fun tulis(sumber: String, waktuTeks: String) {
            prefs.edit().putString("sukses_$sumber", waktuTeks).apply()
        }
    }

    private val izinGoogle = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val data = res.data
        if (data == null) {
            selesai("classroom", "Layar izin Google ditutup tanpa jawaban")
            return@registerForActivityResult
        }
        try {
            ambilGoogle(Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data).accessToken)
        } catch (e: ApiException) {
            selesai("classroom", "Izin Google gagal (kode ${e.statusCode})")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GwadoTema {
                Akar(daftar = daftar, versi = versiData, muatUlang = ::muatUlangDaftar) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val sm = semester
                        if (sm == null) {
                            Text("Konfirmasi semester aktif (ditebak dari tanggal hari ini, betulkan kalau salah):")
                            for ((kunci, label) in LABEL_SEMESTER) {
                                OutlinedTextField(
                                    value = isianSemester[kunci].orEmpty(),
                                    onValueChange = { isianSemester[kunci] = it },
                                    label = { Text(label) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Button(onClick = { simpanSemesterDariIsian() }) { Text("Simpan semester") }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Semester ${sm["semester_label"]} · kode ${sm["semester_kode"]} · " +
                                        "Classroom sejak ${sm["semester_mulai"]}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { ubahSemester(sm) }, enabled = !sibuk) { Text("Ubah") }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = isianKuliah,
                                onValueChange = { isianKuliah = it },
                                label = { Text("Kuliah pertama (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { simpanKuliah() }, enabled = !sibuk) { Text("Simpan") }
                        }
                        for (s in DAFTAR_SUMBER) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(s.label, modifier = Modifier.weight(1f))
                                Button(onClick = { mulai(s) }, enabled = !sibuk && semester != null) { Text("Ambil") }
                                if (s.situs != null) {
                                    TextButton(onClick = { lupakan(s) }, enabled = !sibuk) { Text("Lupakan") }
                                }
                                TextButton(onClick = { bukaMatkul(s.id) }, enabled = !sibuk) { Text("Matkul") }
                            }
                            Text(status[s.id] ?: "belum diambil", style = MaterialTheme.typography.bodySmall)
                            matkul[s.id]?.let { Text("Mata kuliah: $it", style = MaterialTheme.typography.bodySmall) }
                        }
                        OutlinedTextField(
                            value = alamatTempel,
                            onValueChange = { alamatTempel = it },
                            label = { Text("Cadangan: tempel alamat moodlemobile://…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedButton(
                            onClick = { tangani(alamatTempel); alamatTempel = "" },
                            enabled = !sibuk && alamatTempel.isNotBlank(),
                        ) { Text("Pakai alamat") }
                        if (umum.isNotEmpty()) Text(umum)
                        val ms = lihatMatkul
                        if (ms == null) {
                            Text("${daftar.size} tugas di database", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Mata kuliah ${DAFTAR_SUMBER.first { it.id == ms }.label} · " +
                                        "pengecualian berlaku saat Ambil berikutnya",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { lihatMatkul = null }) { Text("Tutup") }
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(matkulSumber, key = { it.externalId }) { m ->
                                    Column {
                                        Text((if (m.diikuti) "✓ " else "– ") + m.nama)
                                        Text(m.alasan.orEmpty(), style = MaterialTheme.typography.bodySmall)
                                        Row {
                                            for ((aksi, label) in PILIHAN_PENGECUALIAN) {
                                                TextButton(
                                                    onClick = { atur(ms, m.externalId, aksi) },
                                                    enabled = m.aksi != aksi,
                                                ) { Text(label) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            umum = try {
                val (sm, d, r) = Gudang.pakai(this@MainActivity) { db ->
                    Triple(db.semesterTersimpan(), db.daftarTugas(), db.ringkasanMatkul())
                }
                semester = sm
                daftar = d
                versiData++
                isianKuliah = Gudang.pakai(this@MainActivity) { it.kuliahMulai() }?.toString().orEmpty()
                matkul.putAll(r)
                if (sm == null) isianSemester.putAll(tebakSemester(tanggalWib(Instant.now())))
                ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Database tidak bisa dibuka: ${e.javaClass.simpleName}"
            }
        }
        // Hanya saat pertama dibuat: rotasi layar mengirim intent yang sama lagi (passport sudah terpakai).
        if (savedInstanceState == null) intentMasuk(intent)
    }

    /** Simpan tanggal kuliah pertama (kosong = hapus), lalu muat ulang beranda. */
    private fun simpanKuliah() {
        val teks = isianKuliah.trim()
        val tanggal = if (teks.isEmpty()) null else runCatching { LocalDate.parse(teks) }.getOrNull()
        if (teks.isNotEmpty() && tanggal == null) {
            umum = "Tanggal kuliah pertama harus berbentuk YYYY-MM-DD, misalnya 2026-08-24"
            return
        }
        lifecycleScope.launch {
            Gudang.pakai(this@MainActivity) { it.simpanKuliahMulai(tanggal) }
            umum = if (tanggal == null) "Tanggal kuliah pertama dihapus" else "Tanggal kuliah pertama disimpan"
            muatUlangDaftar()
        }
    }

    /** Baca ulang daftar dari database, misalnya setelah centang di layar detail. */
    private fun muatUlangDaftar() {
        lifecycleScope.launch {
            daftar = Gudang.pakai(this@MainActivity) { it.daftarTugas() }
            versiData++
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentMasuk(intent)
    }

    private fun intentMasuk(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "moodlemobile") tangani(data.toString())
    }

    private fun mulai(s: Sumber) {
        tertahan(s.id, catatan.baca(s.id), Instant.now(), manual = true)?.let {
            status[s.id] = pesanTertahan(s, it)
            return
        }
        val situs = s.situs
        if (situs == null) {
            mintaIzinGoogle()
            return
        }
        val token = brankas.baca(s.id)
        if (token == null) mulaiLogin(s, situs) else ambil(s) { ambilMoodle(situs, token, Instant.now()) }
    }

    private fun mulaiLogin(s: Sumber, situs: String) {
        val passport = UUID.randomUUID().toString()
        prefs.edit().putString("passport", passport).putString("login_sumber", s.id).apply()
        status[s.id] = "Menunggu login ${s.label} di browser… (kalau ditanya \"Buka dengan\", pilih Gwado)"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(alamatLogin(situs, passport))))
    }

    private fun tangani(alamat: String) {
        val s = DAFTAR_SUMBER.find { it.id == prefs.getString("login_sumber", null) }
        val situs = s?.situs
        if (s == null || situs == null) {
            umum = "Tidak ada login Moodle yang sedang berjalan. Tekan Ambil di Brone/Eling dulu."
            return
        }
        when (val h = bacaRedirect(alamat, situs, prefs.getString("passport", "").orEmpty())) {
            is HasilLogin.Gagal -> status[s.id] = "Login gagal: ${h.alasan}"
            is HasilLogin.Berhasil -> {
                prefs.edit().remove("passport").remove("login_sumber").apply()   // passport sekali pakai
                umum = try {
                    brankas.simpan(s.id, h.token)
                    ""
                } catch (e: Exception) {
                    "Token ${s.label} tidak bisa disimpan (${e.javaClass.simpleName}); dipakai sekali saja."
                }
                ambil(s) { ambilMoodle(situs, h.token, Instant.now()) }
            }
        }
    }

    /** Hanya menghapus token; tugas yang sudah tersimpan di database tetap ada. */
    private fun lupakan(s: Sumber) {
        brankas.hapus(s.id)
        status[s.id] = "Token ${s.label} dihapus dari HP"
    }

    private fun mintaIzinGoogle() {
        sibuk = true
        status["classroom"] = "Meminta izin Google…"
        val req = AuthorizationRequest.builder().setRequestedScopes(SCOPES).build()
        Identity.getAuthorizationClient(this).authorize(req)
            .addOnSuccessListener { r ->
                val pi = r.pendingIntent
                if (r.hasResolution() && pi != null) {
                    izinGoogle.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else {
                    ambilGoogle(r.accessToken)
                }
            }
            .addOnFailureListener { e ->
                selesai(
                    "classroom",
                    "Gagal meminta izin: " + ((e as? ApiException)?.let { "kode ${it.statusCode}" } ?: e.javaClass.simpleName),
                )
            }
    }

    private fun ambilGoogle(akses: String?) {
        if (akses.isNullOrEmpty()) {
            selesai("classroom", "Tidak dapat access token Google")
            return
        }
        ambil(DAFTAR_SUMBER.first { it.id == "classroom" }) { ambilClassroom(akses, Instant.now()) }
    }

    private fun selesai(id: String, pesan: String) {
        status[id] = pesan
        sibuk = false
    }

    private fun ambil(s: Sumber, jalankan: () -> HasilSumber) {
        sibuk = true
        status[s.id] = "Mengambil dari ${s.label}…"
        lifecycleScope.launch {
            status[s.id] = try {
                ambilDanSimpan(s, jalankan)
            } catch (e: CancellationException) {
                throw e                                              // layar ditutup: biarkan batal
            } catch (e: Exception) {
                "Gagal tak terduga: ${e.javaClass.simpleName}"       // tanpa pesan: bisa memuat data
            }
            sibuk = false
        }
    }

    /** Fetch → Isi → ingest → muat ulang daftar. Kembalikan teks status untuk sumber ini. */
    private suspend fun ambilDanSimpan(s: Sumber, jalankan: () -> HasilSumber): String {
        val mulai = Instant.now()
        val isi = try {
            when (val j = withContext(Dispatchers.IO) { job(s.id, catatan, manual = true, jalankan = jalankan) }) {
                is HasilJob.Tertahan -> return pesanTertahan(s, j)      // situs tidak dihubungi: tidak ada run
                is HasilJob.Selesai -> isiSukses(s.id, mulai, Instant.now(), j.hasil)
            }
        } catch (e: FetchError) {
            if (e.jenis == "token_ditolak" && s.situs != null) brankas.hapus(s.id)
            isiGagal(s.id, mulai, Instant.now(), e)                    // run gagal tetap dicatat
        }
        val (h, baru, r) = Gudang.pakai(this) { db -> Triple(db.proses(isi), db.daftarTugas(), db.ringkasanMatkul()) }
        daftar = baru
        versiData++
        matkul.putAll(r)
        lihatMatkul?.let { ms -> matkulSumber = Gudang.pakai(this) { it.daftarMatkul(ms) } }
        val e = isi.error
        return when {
            e == null -> "${h.status}: ${h.pesan}"
            e.jenis == "token_ditolak" -> "Gagal (token_ditolak): ${e.pesan}. Tekan Ambil untuk login ulang."
            else -> "Gagal (${e.jenis}): ${e.pesan}"
        }
    }

    private fun bukaMatkul(sumber: String) {
        lifecycleScope.launch {
            matkulSumber = Gudang.pakai(this@MainActivity) { it.daftarMatkul(sumber) }
            lihatMatkul = sumber
        }
    }

    private fun atur(sumber: String, externalId: String, aksi: String?) {
        lifecycleScope.launch {
            matkulSumber = Gudang.pakai(this@MainActivity) { db ->
                db.aturPengecualian(sumber, externalId, aksi)
                db.daftarMatkul(sumber)
            }
        }
    }

    private fun simpanSemesterDariIsian() {
        val nilai = LABEL_SEMESTER.keys.associateWith { isianSemester[it].orEmpty().trim() }
        periksaSemester(nilai)?.let {
            umum = it
            return
        }
        lifecycleScope.launch {
            umum = try {
                Gudang.pakai(this@MainActivity) { it.simpanSemester(nilai) }
                semester = nilai
                "Semester disimpan; berlaku mulai Ambil berikutnya."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Semester tidak bisa disimpan: ${e.javaClass.simpleName}"
            }
        }
    }

    /** Buka lagi isian dengan nilai sekarang; Ambil mati sampai disimpan ulang. */
    private fun ubahSemester(sekarang: Map<String, String>) {
        isianSemester.clear()
        isianSemester.putAll(sekarang)
        semester = null
    }

    private fun pesanTertahan(s: Sumber, t: HasilJob.Tertahan): String = when (t.sebab) {
        SebabTertahan.JENDELA_HINDARI -> {
            val (m, sl) = JENDELA_HINDARI.getValue(s.id)
            "${s.label} sedang backup (%02d.00–%02d.00 WIB), situs tidak dihubungi. Coba lagi setelah %02d.00 WIB."
                .format(m, sl, sl)
        }
        SebabTertahan.COOLDOWN -> {
            val detik = Duration.between(Instant.now(), t.bolehLagi ?: Instant.now()).seconds.coerceAtLeast(1)
            val sisa = if (detik < 60) "$detik detik" else "${detik / 60 + 1} menit"
            "${s.label} baru saja diambil, situs tidak dihubungi. Coba lagi dalam $sisa."
        }
    }

}
