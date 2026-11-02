package io.github.quazzong.gwado

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import io.github.quazzong.gwado.ui.theme.GwadoTheme
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val SCOPES = listOf(
    Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
    Scope("https://www.googleapis.com/auth/classroom.coursework.me.readonly"),
)

class MainActivity : ComponentActivity() {
    private var hasil by mutableStateOf("Belum ada hasil")

    private val izin = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val data = res.data
        if (data == null) {
            hasil = "Layar izin ditutup tanpa jawaban"
            return@registerForActivityResult
        }
        try {
            val r = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data)
            pakai(r.accessToken, "setelah layar izin")
        } catch (e: ApiException) {
            hasil = "Izin dibatalkan/gagal (kode ${e.statusCode})"
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Uji Google Classroom")
                        Button(onClick = { mulai() }) { Text("Minta izin + hitung kelas") }
                        Text(hasil)
                    }
                }
            }
        }
    }

    private fun mulai() {
        hasil = "Meminta izin…"
        val req = AuthorizationRequest.builder().setRequestedScopes(SCOPES).build()
        Identity.getAuthorizationClient(this).authorize(req)
            .addOnSuccessListener { r ->
                val pi = r.pendingIntent
                if (r.hasResolution() && pi != null) {
                    izin.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else {
                    pakai(r.accessToken, "tanpa layar (izin sudah ada)")
                }
            }
            .addOnFailureListener { e ->
                hasil = "Gagal meminta izin: " +
                    ((e as? ApiException)?.let { "kode ${it.statusCode}" } ?: e.javaClass.simpleName)
            }
    }

    private fun pakai(token: String?, jalur: String) {
        if (token.isNullOrEmpty()) {
            hasil = "Tidak dapat access token ($jalur)"
            return
        }
        hasil = "Token didapat ($jalur), menghubungi Classroom…"
        lifecycleScope.launch {
            val teks = withContext(Dispatchers.IO) { hitungKelas(token) }
            hasil = "Jalur: $jalur\n$teks"
        }
    }

    private fun hitungKelas(token: String): String {
        val conn = URL("https://classroom.googleapis.com/v1/courses?studentId=me&courseStates=ACTIVE&pageSize=100")
            .openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        return try {
            val kode = conn.responseCode
            if (kode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                "Berhasil: ${json.optJSONArray("courses")?.length() ?: 0} kelas aktif"
            } else {
                val pesan = conn.errorStream?.bufferedReader()?.readText()
                    ?.let { JSONObject(it).optJSONObject("error")?.optString("message") }
                "Classroom menjawab HTTP $kode: ${pesan ?: "-"}"
            }
        } catch (e: Exception) {
            "Gagal menghubungi Classroom: ${e.javaClass.simpleName}"
        } finally {
            conn.disconnect()
        }
    }
}
