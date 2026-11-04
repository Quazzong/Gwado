package io.github.quazzong.gwado.siapkan

import io.github.quazzong.gwado.db.Db

/** Kunci preferences: alur Siapkan Gwado sudah selesai di HP ini. */
const val KUNCI_SIAP = "siap_selesai"

fun Db.siapSelesai(): Boolean = satu("SELECT 1 AS ada FROM preferences WHERE kunci = ?", KUNCI_SIAP) != null

fun Db.tandaiSiap() {
    exec(
        "INSERT INTO preferences (kunci, nilai) VALUES (?, '1') ON CONFLICT (kunci) DO NOTHING",
        KUNCI_SIAP,
    )
}

/**
 * Siapkan Gwado tampil sampai pengguna menekan Mulai. Instalasi yang sudah punya semester
 * dan minimal satu layanan dianggap sudah siap, jadi tidak ditanya lagi.
 */
fun perluSiapkan(selesai: Boolean, adaSemester: Boolean, adaLayanan: Boolean): Boolean =
    !selesai && !(adaSemester && adaLayanan)

/** Kalimat langkah yang belum beres saat Mulai ditekan, atau null kalau boleh mulai. */
fun kurangSiap(adaLayanan: Boolean, adaSemester: Boolean): String? = when {
    !adaLayanan -> "Tambah minimal satu layanan dulu."
    !adaSemester -> "Konfirmasi semester dulu."
    else -> null
}
