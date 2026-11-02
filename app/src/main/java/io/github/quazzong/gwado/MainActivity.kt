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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import io.github.quazzong.gwado.fetch.CatatanSukses
import io.github.quazzong.gwado.fetch.FetchError
import io.github.quazzong.gwado.fetch.HasilJob
import io.github.quazzong.gwado.fetch.JENDELA_HINDARI
import io.github.quazzong.gwado.fetch.SebabTertahan
import io.github.quazzong.gwado.fetch.ambilClassroom
import io.github.quazzong.gwado.fetch.ambilMoodle
import io.github.quazzong.gwado.fetch.job
import io.github.quazzong.gwado.fetch.tertahan
import io.github.quazzong.gwado.masuk.BrankasKeystore
import io.github.quazzong.gwado.masuk.HasilLogin
import io.github.quazzong.gwado.masuk.alamatLogin
import io.github.quazzong.gwado.masuk.bacaRedirect
import io.github.quazzong.gwado.sumber.HasilSumber
import io.github.quazzong.gwado.ui.theme.GwadoTheme
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// GW5: tiga sumber, token Moodle tersimpan di Keystore, penahan untuk semua. Tampilan polos sampai GW8.

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

private class Baris(val judul: String, val keterangan: String, val deadline: String?)

class MainActivity : ComponentActivity() {
    private val status = mutableStateMapOf<String, String>()
    private val hasil = mutableStateMapOf<String, List<Baris>>()
    private var sibuk by mutableStateOf(false)
    private var umum by mutableStateOf("")
    private var alamatTempel by mutableStateOf("")
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
            GwadoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Gwado · uji tiga sumber (GW5)")
                        for (s in DAFTAR_SUMBER) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(s.label, modifier = Modifier.weight(1f))
                                Button(onClick = { mulai(s) }, enabled = !sibuk) { Text("Ambil") }
                                if (s.situs != null) {
                                    TextButton(onClick = { lupakan(s) }, enabled = !sibuk) { Text("Lupakan") }
                                }
                            }
                            Text(status[s.id] ?: "belum diambil", style = MaterialTheme.typography.bodySmall)
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
                        val semua = hasil.values.flatten().sortedWith(compareBy(nullsLast<String>()) { it.deadline })
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(semua) { b ->
                                Column {
                                    Text(b.judul)
                                    Text(b.keterangan, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        // Hanya saat pertama dibuat: rotasi layar mengirim intent yang sama lagi (passport sudah terpakai).
        if (savedInstanceState == null) intentMasuk(intent)
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

    private fun lupakan(s: Sumber) {
        brankas.hapus(s.id)
        hasil.remove(s.id)
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
                when (val j = withContext(Dispatchers.IO) { job(s.id, catatan, manual = true, jalankan = jalankan) }) {
                    is HasilJob.Tertahan -> pesanTertahan(s, j)
                    is HasilJob.Selesai -> {
                        hasil[s.id] = keBaris(s, j.hasil)
                        "${j.hasil.kelas.size} kelas, ${j.hasil.tugas.size} tugas (belum disaring semester)"
                    }
                }
            } catch (e: CancellationException) {
                throw e                                              // layar ditutup: biarkan batal
            } catch (e: FetchError) {
                if (e.jenis == "token_ditolak" && s.situs != null) brankas.hapus(s.id)
                "Gagal (${e.jenis}): ${e.pesan}" +
                    if (e.jenis == "token_ditolak") ". Tekan Ambil untuk login ulang." else ""
            } catch (e: Exception) {
                "Gagal tak terduga: ${e.javaClass.simpleName}"       // tanpa pesan: bisa memuat data
            }
            sibuk = false
        }
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

    private fun keBaris(s: Sumber, h: HasilSumber): List<Baris> {
        val namaKelas = h.kelas.associate { "${it.externalId}" to it.nama }
        return h.tugas.map { t ->
            Baris(
                t.judul ?: "(tanpa judul)",
                listOfNotNull(
                    s.label,
                    namaKelas["${t.courseExternalId}"],
                    t.jenis,
                    t.deadline ?: "tanpa deadline",
                    t.statusKumpulSitus?.let { "situs: $it" },
                ).joinToString(" · "),
                t.deadline,
            )
        }
    }
}
