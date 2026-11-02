package io.github.quazzong.gwado

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import io.github.quazzong.gwado.ui.theme.GwadoTheme
import java.security.MessageDigest
import java.util.UUID

private const val SITUS = "https://eling.ub.ac.id"

class MainActivity : ComponentActivity() {
    private var hasil by mutableStateOf("Belum ada hasil")
    private val prefs by lazy { getPreferences(MODE_PRIVATE) }

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
                        Text("Uji token Moodle (Eling)")
                        Button(onClick = { mulaiLogin("gwado") }) { Text("Uji A: minta gwado://") }
                        Button(onClick = { mulaiLogin("moodlemobile") }) { Text("Uji B: minta moodlemobile://") }
                        Text(hasil)
                    }
                }
            }
        }
        tangani(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tangani(intent)
    }

    private fun mulaiLogin(skema: String) {
        val passport = UUID.randomUUID().toString()
        prefs.edit().putString("passport", passport).apply()
        val url = Uri.parse("$SITUS/admin/tool/mobile/launch.php").buildUpon()
            .appendQueryParameter("service", "moodle_mobile_app")
            .appendQueryParameter("passport", passport)
            .appendQueryParameter("urlscheme", skema)
            .build()
        hasil = "Menunggu login (minta $skema://)…"
        startActivity(Intent(Intent.ACTION_VIEW, url))
    }

    private fun tangani(intent: Intent?) {
        val data = intent?.data ?: return
        val skema = data.scheme ?: return
        if (skema != "moodlemobile" && skema != "gwado") return
        val mentah = data.toString().substringAfter("token=", "")
        if (mentah.isEmpty()) {
            hasil = "Masuk lewat $skema://, tapi tanpa token"
            return
        }
        val bagian = try {
            String(Base64.decode(mentah, Base64.DEFAULT)).split(":::")
        } catch (e: IllegalArgumentException) {
            hasil = "Masuk lewat $skema://, tapi token tidak bisa dibaca"
            return
        }
        val passport = prefs.getString("passport", "").orEmpty()
        val cocok = passport.isNotEmpty() && bagian.getOrNull(0) == md5(SITUS + passport)
        val token = bagian.getOrNull(1).orEmpty()
        hasil = "Ditangkap lewat: $skema://\n" +
            "Tanda tangan cocok: ${if (cocok) "YA" else "TIDAK"}\n" +
            "Panjang token: ${token.length} karakter\n" +
            "Ada private token: ${if (bagian.size >= 3) "ya" else "tidak"}"
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
