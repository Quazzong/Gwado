package io.github.quazzong.gwado.layanan

import io.github.quazzong.gwado.db.Db

/**
 * Nyalakan atau matikan satu sumber. Sumber mati tidak diambil dan tidak muncul di status
 * sumber; tugas yang sudah tersimpan tidak disentuh.
 */
fun Db.aturAktif(sumber: String, aktif: Boolean) {
    exec("UPDATE sources SET aktif = ? WHERE id = ?", aktif, sumber)
}
