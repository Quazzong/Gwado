package io.github.quazzong.gwado.fetch

import io.github.quazzong.gwado.sumber.Classroom
import io.github.quazzong.gwado.sumber.HasilSumber
import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val API_CLASSROOM = "https://classroom.googleapis.com/v1/"

/**
 * Kelas `Google` di fetch.py tanpa akses(): Gwado mendapat access token dari AuthorizationClient
 * (Play services memegang refresh token). Token hanya di header, tidak pernah di URL.
 */
class KlienGoogle(private val akses: String, private val minta: Minta) {
    /** GET berhalaman: gabungkan isi `kunci` dari semua halaman sampai nextPageToken kosong. */
    fun daftar(path: String, kunci: String, vararg param: Pair<String, String>): List<JsonElement> {
        val hasil = mutableListOf<JsonElement>()
        var halaman: String? = null
        while (true) {
            val p = param.toList() + ("pageSize" to "100") + listOfNotNull(halaman?.let { "pageToken" to it })
            val r = try {
                minta(API_CLASSROOM + path + "?" + kodekanForm(p), null, mapOf("Authorization" to "Bearer $akses"))
            } catch (e: HttpGagal) {
                if (e.kode == 401) throw FetchError("token_ditolak", "akses Google ditolak (HTTP 401)")
                throw umum(e)
            }
            val o = r.jsonObject
            hasil += (o[kunci] as? JsonArray).orEmpty()
            halaman = (o["nextPageToken"] as? JsonPrimitive)?.contentOrNull?.ifEmpty { null } ?: return hasil
        }
    }
}

/**
 * ambil_classroom (tanpa lampiran). Satu kelas gagal = seluruh run gagal: tugas yang tidak
 * terambil tidak boleh terhitung absen (lalu salah ditandai hilang). Blokir: Dispatchers.IO.
 */
fun ambilClassroom(akses: String, sekarang: Instant, minta: Minta = mintaBawaan): HasilSumber {
    val g = KlienGoogle(akses, minta)
    val kelas = g.daftar("courses", "courses", "studentId" to "me")
    val cw = mutableMapOf<String, JsonObject>()
    val sub = mutableMapOf<String, JsonObject>()
    for (k in kelas) {
        val o = k.jsonObject
        if (!Classroom.perluDetail(o, sekarang)) continue
        val id = o.getValue("id").jsonPrimitive.content
        cw[id] = buildJsonObject {
            put("courseWork", JsonArray(g.daftar("courses/$id/courseWork", "courseWork")))
        }
        sub[id] = buildJsonObject {
            put(
                "studentSubmissions",
                JsonArray(g.daftar("courses/$id/courseWork/-/studentSubmissions", "studentSubmissions", "userId" to "me")),
            )
        }
    }
    return Classroom.normalisasi(buildJsonObject { put("courses", JsonArray(kelas)) }, cw, sub)
}
