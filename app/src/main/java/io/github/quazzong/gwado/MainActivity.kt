package io.github.quazzong.gwado

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.github.quazzong.gwado.fetch.FetchError
import io.github.quazzong.gwado.fetch.ambilMoodle
import io.github.quazzong.gwado.masuk.HasilLogin
import io.github.quazzong.gwado.masuk.alamatLogin
import io.github.quazzong.gwado.masuk.bacaRedirect
import io.github.quazzong.gwado.sumber.HasilSumber
import io.github.quazzong.gwado.ui.theme.GwadoTheme
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// GW4: alur utuh pertama (login Brone → ambil → daftar polos). Token hanya di memori sampai GW5.
private const val SITUS = "https://brone.ub.ac.id"

/** Satu baris daftar tugas polos; tampilan sungguhan menyusul di GW8. */
private class Baris(val judul: String, val keterangan: String)

class MainActivity : ComponentActivity() {
    private var status by mutableStateOf("Belum login")
    private var sibuk by mutableStateOf(false)
    private var daftar by mutableStateOf(emptyList<Baris>())
    private var alamatTempel by mutableStateOf("")
    private val prefs by lazy { getPreferences(MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GwadoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Gwado · uji alur Brone (GW4)")
                        Button(onClick = { mulaiLogin() }, enabled = !sibuk) {
                            Text("Login Brone + ambil tugas")
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
                        Text(status)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(daftar) { b ->
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

    private fun mulaiLogin() {
        val passport = UUID.randomUUID().toString()
        prefs.edit().putString("passport", passport).apply()    // aplikasi bisa dimatikan selama login
        status = "Menunggu login Brone di browser… (kalau ditanya \"Buka dengan\", pilih Gwado)"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(alamatLogin(SITUS, passport))))
    }

    private fun tangani(alamat: String) {
        val passport = prefs.getString("passport", "").orEmpty()
        when (val h = bacaRedirect(alamat, SITUS, passport)) {
            is HasilLogin.Gagal -> status = "Login gagal: ${h.alasan}"
            is HasilLogin.Berhasil -> {
                prefs.edit().remove("passport").apply()            // passport sekali pakai
                ambil(h.token)
            }
        }
    }

    private fun ambil(token: String) {
        sibuk = true
        status = "Login berhasil, mengambil tugas dari Brone…"
        lifecycleScope.launch {
            status = try {
                val h = withContext(Dispatchers.IO) { ambilMoodle(SITUS, token, Instant.now()) }
                daftar = keBaris(h)
                "${h.kelas.size} kelas, ${h.tugas.size} tugas (belum disaring semester)"
            } catch (e: CancellationException) {
                throw e                                              // layar ditutup: biarkan batal
            } catch (e: FetchError) {
                "Gagal (${e.jenis}): ${e.pesan}"
            } catch (e: Exception) {
                "Gagal tak terduga: ${e.javaClass.simpleName}"       // tanpa pesan: bisa memuat data
            }
            sibuk = false
        }
    }

    private fun keBaris(h: HasilSumber): List<Baris> {
        val namaKelas = h.kelas.associate { "${it.externalId}" to it.nama }
        return h.tugas.sortedWith(compareBy(nullsLast<String>()) { it.deadline }).map { t ->
            Baris(
                t.judul ?: "(tanpa judul)",
                listOfNotNull(
                    namaKelas["${t.courseExternalId}"],
                    t.jenis,
                    t.deadline ?: "tanpa deadline",
                    t.statusKumpulSitus?.let { "situs: $it" },
                ).joinToString(" · "),
            )
        }
    }
}
