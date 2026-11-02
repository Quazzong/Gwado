package io.github.quazzong.gwado.fetch

import kotlinx.serialization.json.JsonElement

// Terjemahan bagian umum serper/fetch.py. Token tidak pernah masuk URL, log, atau pesan error.

const val TIMEOUT = 30                 // detik
val JEDA_ULANG = listOf(10, 20)        // detik; jeda sebelum percobaan ke-2 dan ke-3

/** HTTP → JSON. data != null berarti POST. Test memakai versi palsu; versi jaringan menyusul. */
typealias Minta = (url: String, data: ByteArray?, headers: Map<String, String>) -> JsonElement

/** Gagal yang sudah diklasifikasikan. jenis: token_ditolak | jaringan | lain. */
class FetchError(val jenis: String, val pesan: String) : Exception("$jenis: $pesan")

/** Jawaban HTTP 4xx/5xx; isi = badan JSON kalau ada (untuk membaca kode error). */
class HttpGagal(val kode: Int, val isi: JsonElement?) : Exception("HTTP $kode")

internal fun umum(e: HttpGagal) =
    FetchError(if (e.kode >= 500) "jaringan" else "lain", "HTTP ${e.kode}")
