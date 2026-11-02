package io.github.quazzong.gwado.masuk

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Penyimpanan token per sumber. Isinya tidak pernah dicetak ke log atau pesan. */
interface BrankasToken {
    fun simpan(sumber: String, token: String)
    fun baca(sumber: String): String?
    fun hapus(sumber: String)
}

/**
 * Token dienkripsi AES-GCM dengan kunci di Android Keystore (kunci tidak bisa dibaca aplikasi),
 * sandinya disimpan di SharedPreferences. Nama sumber jadi AAD: sandi Brone tidak bisa dibaca
 * sebagai Eling. Gagal membuka (misalnya kunci hilang setelah restore) → token dihapus, login ulang.
 */
class BrankasKeystore(context: Context) : BrankasToken {
    private val prefs = context.getSharedPreferences("brankas", Context.MODE_PRIVATE)

    private fun kunci(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    override fun simpan(sumber: String, token: String) {
        val c = Cipher.getInstance(TRANSFORMASI)
        c.init(Cipher.ENCRYPT_MODE, kunci())          // IV acak dibuat Keystore
        c.updateAAD(sumber.toByteArray())
        val sandi = c.doFinal(token.toByteArray())
        val b64 = Base64.getEncoder()
        prefs.edit().putString(sumber, b64.encodeToString(c.iv) + ":" + b64.encodeToString(sandi)).apply()
    }

    override fun baca(sumber: String): String? {
        val isi = prefs.getString(sumber, null) ?: return null
        return try {
            val (iv, sandi) = isi.split(":", limit = 2).map { Base64.getDecoder().decode(it) }
            val c = Cipher.getInstance(TRANSFORMASI)
            c.init(Cipher.DECRYPT_MODE, kunci(), GCMParameterSpec(128, iv))
            c.updateAAD(sumber.toByteArray())
            String(c.doFinal(sandi))
        } catch (e: Exception) {
            hapus(sumber)                               // tidak bisa dibuka: lebih aman minta login ulang
            null
        }
    }

    override fun hapus(sumber: String) {
        prefs.edit().remove(sumber).apply()
    }

    private companion object {
        const val ALIAS = "gwado-token"
        const val TRANSFORMASI = "AES/GCM/NoPadding"
    }
}
