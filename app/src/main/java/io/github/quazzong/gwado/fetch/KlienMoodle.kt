package io.github.quazzong.gwado.fetch

import io.github.quazzong.gwado.sumber.HasilSumber
import io.github.quazzong.gwado.sumber.Moodle
import io.github.quazzong.gwado.sumber.angka
import io.github.quazzong.gwado.sumber.daftar
import java.net.URLEncoder
import java.time.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.putJsonArray

/** _param_moodle: daftar dipecah jadi k[0], k[1], …; nilai lain jadi teks. */
internal fun paramMoodle(param: List<Pair<String, Any>>): List<Pair<String, String>> =
    param.flatMap { (k, v) ->
        if (v is List<*>) v.mapIndexed { i, x -> "$k[$i]" to x.toString() }
        else listOf(k to v.toString())
    }

/** Seperti urlencode Python (quote_plus). URLEncoder Java beda di `*` dan `~`. */
internal fun kodekanForm(pasangan: List<Pair<String, String>>): String =
    pasangan.joinToString("&") { (k, v) -> "${kodekan(k)}=${kodekan(v)}" }

private fun kodekan(s: String): String =
    URLEncoder.encode(s, "UTF-8").replace("*", "%2A").replace("%7E", "~")

/** Kelas `Moodle` di fetch.py; namanya beda supaya tidak bentrok dengan parser sumber.Moodle. */
class KlienMoodle(situs: String, private val token: String, private val minta: Minta) {
    val url = situs.trimEnd('/') + "/webservice/rest/server.php"

    fun panggil(fungsi: String, vararg param: Pair<String, Any>): JsonElement {
        val data = listOf("wstoken" to token, "wsfunction" to fungsi, "moodlewsrestformat" to "json")
        val badan = kodekanForm(data + paramMoodle(param.toList())).toByteArray()
        val r = try {
            minta(url, badan, emptyMap())
        } catch (e: HttpGagal) {
            throw umum(e)
        }
        if (r is JsonObject && "exception" in r) {
            val kode = (r["errorcode"] as? JsonPrimitive)?.contentOrNull?.ifEmpty { null } ?: "tanpa kode"
            if (kode == "invalidtoken") {
                throw FetchError("token_ditolak", "invalidtoken (token kedaluwarsa, ambil ulang)")
            }
            throw FetchError("lain", "$fungsi: $kode")
        }
        return r
    }
}

/**
 * Tanda selesai di situs untuk kelas yang sedang berjalan: {id kelas: respons completion}.
 * Butuh userid dari info situs. Gagal biasa (fungsi ditutup situs, kelas tanpa izin) hanya
 * melewatkan bagian itu; gagal jaringan dan token tetap menggagalkan pengambilan.
 */
internal fun ambilCompletion(m: KlienMoodle): Map<Long, JsonObject> {
    fun <T> lewatiBiasa(blok: () -> T): T? = try {
        blok()
    } catch (e: FetchError) {
        if (e.jenis != "lain") throw e
        null
    }
    val userid = lewatiBiasa { m.panggil("core_webservice_get_site_info").jsonObject.angka("userid") }
        ?: return emptyMap()
    val berjalan = lewatiBiasa {
        m.panggil(
            "core_course_get_enrolled_courses_by_timeline_classification",
            "classification" to "inprogress", "limit" to 0,
        ).jsonObject
    } ?: return emptyMap()
    val hasil = mutableMapOf<Long, JsonObject>()
    for (c in berjalan.daftar("courses")) {
        val id = c.angka("id") ?: continue
        lewatiBiasa {
            m.panggil("core_completion_get_activities_completion_status", "courseid" to id, "userid" to userid)
                .jsonObject
        }?.let { hasil[id] = it }
    }
    return hasil
}

/**
 * ambil_moodle (tanpa lampiran): semua kelas → assign + kuis → status assign dalam jendela →
 * tanda selesai kelas yang berjalan → normalisasi. Blokir: panggil dari Dispatchers.IO.
 */
fun ambilMoodle(situs: String, token: String, sekarang: Instant, minta: Minta = mintaBawaan): HasilSumber {
    val m = KlienMoodle(situs, token, minta)
    val coursesAll = m.panggil(
        "core_course_get_enrolled_courses_by_timeline_classification",
        "classification" to "all", "limit" to 0,
    ).jsonObject
    val ids = coursesAll.daftar("courses").map { it.getValue("id").jsonPrimitive.content }
    val (assignments, quizzes) = if (ids.isNotEmpty()) {
        Pair(
            m.panggil("mod_assign_get_assignments", "courseids" to ids).jsonObject,
            m.panggil("mod_quiz_get_quizzes_by_courses", "courseids" to ids).jsonObject,
        )
    } else {
        Pair(buildJsonObject { putJsonArray("courses") {} }, buildJsonObject { putJsonArray("quizzes") {} })
    }
    val status = mutableMapOf<Long, JsonObject>()
    for (c in assignments.daftar("courses")) {
        for (a in c.daftar("assignments")) {
            if (!Moodle.perluDetail(a.angka("duedate"), sekarang)) continue
            val id = a.getValue("id").jsonPrimitive.long
            try {
                status[id] = m.panggil("mod_assign_get_submission_status", "assignid" to id).jsonObject
            } catch (e: FetchError) {
                if (e.jenis != "lain") throw e
                // status satu assign gagal: tugasnya tetap ada, hanya statusnya "tidak diketahui"
            }
        }
    }
    val completion = if (ids.isNotEmpty()) ambilCompletion(m) else emptyMap()
    return Moodle.normalisasi(situs, coursesAll, assignments, quizzes, status, completion)
}
