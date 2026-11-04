package io.github.quazzong.gwado.tugas

import io.github.quazzong.gwado.db.Db
import java.time.Instant

/**
 * Centang banyak tugas sekaligus, satu per satu lewat [tandai] (riwayat tiap tugas tetap tercatat).
 * Kembalikan jumlah tugas yang benar-benar berubah; yang sudah selesai dilewati.
 */
fun Db.tandaiSemua(id: List<String>, sekarang: Instant): Int =
    id.count { tandai(it, true, sekarang) }
