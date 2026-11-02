package io.github.quazzong.gwado.masuk

import java.security.MessageDigest
import java.util.Base64

/** Hasil membaca redirect login SSO Moodle. */
sealed interface HasilLogin {
    /** Bukan data class: toString bawaannya akan mencetak token. */
    class Berhasil(val token: String, val privateToken: String?) : HasilLogin {
        override fun toString() = "Berhasil(token ${token.length} karakter)"
    }

    data class Gagal(val alasan: String) : HasilLogin
}

/** Alamat login SSO; hasilnya redirect moodlemobile://token=… (Brone dan Eling memaksa skema ini). */
fun alamatLogin(situs: String, passport: String): String =
    "${situs.trimEnd('/')}/admin/tool/mobile/launch.php" +
        "?service=moodle_mobile_app&passport=$passport&urlscheme=moodlemobile"

/**
 * Redirect moodlemobile://token=<base64>; isi base64: md5(situs + passport):::token[:::private token].
 * situs tanpa garis miring di akhir, sama dengan wwwroot Moodle.
 */
fun bacaRedirect(alamat: String, situs: String, passport: String): HasilLogin {
    val mentah = alamat.trim().substringAfter("token=", "")
    if (mentah.isEmpty()) return HasilLogin.Gagal("alamat tidak berisi token")
    if (passport.isEmpty()) return HasilLogin.Gagal("tidak ada login yang sedang berjalan, mulai dari tombol login")
    val bagian = try {
        String(Base64.getMimeDecoder().decode(mentah), Charsets.UTF_8).split(":::")
    } catch (e: IllegalArgumentException) {
        return HasilLogin.Gagal("token tidak bisa dibaca")
    }
    if (bagian[0] != md5(situs.trimEnd('/') + passport)) {
        return HasilLogin.Gagal("tanda tangan tidak cocok (login dari percobaan atau situs lain)")
    }
    val token = bagian.getOrNull(1).orEmpty()
    if (token.isEmpty()) return HasilLogin.Gagal("token kosong")
    return HasilLogin.Berhasil(token, bagian.getOrNull(2)?.ifEmpty { null })
}

internal fun md5(s: String): String =
    MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
