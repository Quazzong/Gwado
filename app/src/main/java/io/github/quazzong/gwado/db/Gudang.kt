package io.github.quazzong.gwado.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Satu-satunya pintu ke database aplikasi (gwado.db). Semua akses lewat satu jalur
 * (limitedParallelism(1)), jadi koneksi tidak pernah dipakai dua blok bersamaan.
 * Satu objek per proses, supaya layar dan penjadwal memakai koneksi yang sama.
 */
object Gudang {
    private val jalur = Dispatchers.IO.limitedParallelism(1)
    private var db: Db? = null

    suspend fun <T> pakai(context: Context, blok: (Db) -> T): T = withContext(jalur) {
        val d = db ?: buka(context.applicationContext).also { db = it }
        blok(d)
    }

    private fun buka(context: Context): Db {
        val file = context.getDatabasePath("gwado.db")
        file.parentFile?.mkdirs()          // folder databases/ belum ada di instalasi baru
        return Db.buka(file.path).also { it.migrasi() }
    }
}
