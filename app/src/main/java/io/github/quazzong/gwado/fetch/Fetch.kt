package io.github.quazzong.gwado.fetch

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// Bagian umum pengambilan data lewat HTTP. Token tidak pernah masuk URL, log, atau pesan error.
const val TIMEOUT = 30                 // detik
val JEDA_ULANG = listOf(10, 20)        // detik; jeda sebelum percobaan ke-2 dan ke-3

/** HTTP → JSON. data != null berarti POST. */
typealias Minta = (url: String, data: ByteArray?, headers: Map<String, String>) -> JsonElement

/** Gagal yang sudah diklasifikasikan. jenis: token_ditolak | jaringan | lain. */
class FetchError(val jenis: String, val pesan: String) : Exception("$jenis: $pesan")

/** Jawaban HTTP 4xx/5xx; isi = badan JSON kalau ada (untuk membaca kode error). */
class HttpGagal(val kode: Int, val isi: JsonElement?) : Exception("HTTP $kode")

internal fun umum(e: HttpGagal) =
    FetchError(if (e.kode >= 500) "jaringan" else "lain", "HTTP ${e.kode}")

/** Jawaban HTTP mentah dari satu percobaan, sebelum dibaca sebagai JSON. */
class JawabanHttp(val kode: Int, val badan: String)

/** Satu percobaan HTTP. Gangguan jaringan dilempar sebagai IOException. */
typealias Buka = (url: String, data: ByteArray?, headers: Map<String, String>) -> JawabanHttp

/**
 * Buka asli lewat HttpURLConnection. Blokir: panggil dari Dispatchers.IO.
 * Tidak memanggil disconnect(): aliran ditutup penuh, jadi koneksi bisa dipakai ulang
 * (keep-alive), lebih ringan untuk situs kampus.
 */
fun bukaHttp(url: String, data: ByteArray?, headers: Map<String, String>): JawabanHttp {
    val k = URL(url).openConnection() as HttpURLConnection
    k.connectTimeout = TIMEOUT * 1000
    k.readTimeout = TIMEOUT * 1000
    headers.forEach { (nama, nilai) -> k.setRequestProperty(nama, nilai) }
    if (data != null) {
        k.requestMethod = "POST"
        k.doOutput = true
        k.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        k.setFixedLengthStreamingMode(data.size)
        k.outputStream.use { it.write(data) }
    }
    val kode = k.responseCode
    val aliran = if (kode in 200..299) k.inputStream else k.errorStream
    val badan = aliran?.use { bacaTerbatas(it) } ?: ""
    return JawabanHttp(kode, badan)
}

/** Batas isi satu jawaban; yang lebih besar ditolak supaya memori HP tidak habis. */
const val MAKS_JAWABAN = 20 * 1024 * 1024

/** Baca aliran sebagai teks UTF-8; lebih dari [maks] byte → FetchError "lain" (tidak diulang). */
internal fun bacaTerbatas(aliran: InputStream, maks: Int = MAKS_JAWABAN): String {
    val keluar = ByteArrayOutputStream()
    val penampung = ByteArray(64 * 1024)
    while (true) {
        val n = aliran.read(penampung)
        if (n < 0) break
        if (keluar.size() + n > maks) throw FetchError("lain", "jawaban lebih dari ${maks / (1024 * 1024)} MB")
        keluar.write(penampung, 0, n)
    }
    return keluar.toByteArray().decodeToString()
}

/** Hanya objek atau daftar: parser kotlinx menerima teks tanpa kutip yang ditolak json.loads. */
private fun bacaJson(teks: String): JsonElement? =
    try {
        Json.parseToJsonElement(teks).takeIf { it is JsonObject || it is JsonArray }
    } catch (e: SerializationException) {
        null
    }

/** HTTP → JSON. Gangguan jaringan dan 5xx diulang (jeda 10 s lalu 20 s); 4xx langsung HttpGagal. */
fun minta(
    url: String,
    data: ByteArray? = null,
    headers: Map<String, String> = emptyMap(),
    buka: Buka = ::bukaHttp,
    tidur: (Int) -> Unit = { Thread.sleep(it * 1000L) },
    catat: (String) -> Unit = { Log.i("GwadoFetch", it) },    // tanpa URL maupun token
): JsonElement {
    val urutanJeda: List<Int?> = JEDA_ULANG + listOf(null)
    for ((i, jeda) in urutanJeda.withIndex()) {
        val ke = i + 1
        val r = try {
            buka(url, data, headers)
        } catch (e: IOException) {
            val nama = e.javaClass.simpleName
            if (jeda == null) {
                throw FetchError("jaringan", "tidak tersambung setelah $ke percobaan ($nama)")
            }
            catat("percobaan $ke gagal ($nama), ulang dalam $jeda s")
            tidur(jeda)
            continue
        }
        if (r.kode !in 200..299) {
            if (r.kode >= 500 && jeda != null) {
                catat("percobaan $ke gagal (HTTP ${r.kode}), ulang dalam $jeda s")
                tidur(jeda)
                continue
            }
            throw HttpGagal(r.kode, bacaJson(r.badan))
        }
        return bacaJson(r.badan) ?: throw FetchError("lain", "jawaban bukan JSON")
    }
    error("tidak tercapai")
}

/** minta asli sebagai nilai Minta (untuk parameter bawaan yang namanya juga "minta"). */
val mintaBawaan: Minta = { url, data, headers -> minta(url, data, headers) }
