package io.github.quazzong.gwado.fetch

import java.net.URLEncoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
