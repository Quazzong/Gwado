package io.github.quazzong.gwado.db

import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/** Satu baris hasil query: nama kolom → Long, Double, String, atau null (seperti sqlite3.Row). */
typealias Baris = Map<String, Any?>

/**
 * Pembungkus tipis SQLiteConnection supaya terasa seperti sqlite3 Python.
 * Satu Db = satu koneksi; jangan dipakai dari dua thread sekaligus.
 */
class Db(private val conn: SQLiteConnection) : AutoCloseable {

    /** Jalankan satu pernyataan; baris hasilnya (kalau ada) dibuang. '?' diisi berurutan dari [arg]. */
    fun exec(sql: String, vararg arg: Any?) {
        pakai(sql) { st ->
            ikat(st, arg)
            while (st.step()) { /* buang baris */ }
        }
    }

    /** Semua baris hasil, seperti cursor.fetchall(). */
    fun semua(sql: String, vararg arg: Any?): List<Baris> =
        pakai(sql) { st ->
            ikat(st, arg)
            val kolom = List(st.getColumnCount()) { st.getColumnName(it) }
            buildList {
                while (st.step()) add(kolom.indices.associate { kolom[it] to nilai(st, it) })
            }
        }

    /** Baris pertama atau null, seperti cursor.fetchone(). */
    fun satu(sql: String, vararg arg: Any?): Baris? = semua(sql, *arg).firstOrNull()

    /** Blok yang berhasil semua atau batal semua. */
    fun <T> transaksi(blok: () -> T): T {
        // IMMEDIATE: ambil kunci tulis di awal, supaya tidak bentrok di tengah jalan
        exec("BEGIN IMMEDIATE")
        val hasil = try {
            blok()
        } catch (e: Throwable) {
            exec("ROLLBACK")
            throw e
        }
        exec("COMMIT")
        return hasil
    }

    /**
     * prepare + use, dengan SQL-nya ditempel di pesan error. Di test JVM pesan asli SQLite hilang
     * (android.database.SQLException di android.jar test hanya kerangka), jadi tanpa ini kita
     * tidak tahu pernyataan mana yang gagal. Aman: nilai selalu lewat '?', bukan teks SQL.
     */
    private inline fun <T> pakai(sql: String, blok: (SQLiteStatement) -> T): T =
        try {
            conn.prepare(sql).use(blok)
        } catch (e: Exception) {
            throw SqlGagal(sql, e)
        }

    override fun close() = conn.close()

    private fun ikat(st: SQLiteStatement, arg: Array<out Any?>) {
        arg.forEachIndexed { i, a ->
            val k = i + 1   // bind mulai dari 1
            when (a) {
                null -> st.bindNull(k)
                is String -> st.bindText(k, a)
                is Long -> st.bindLong(k, a)
                is Int -> st.bindLong(k, a.toLong())
                is Boolean -> st.bindLong(k, if (a) 1 else 0)
                is Double -> st.bindDouble(k, a)
                else -> throw IllegalArgumentException("tipe parameter tidak didukung: ${a::class}")
            }
        }
    }

    private fun nilai(st: SQLiteStatement, i: Int): Any? = when (st.getColumnType(i)) {
        SQLITE_DATA_NULL -> null
        SQLITE_DATA_INTEGER -> st.getLong(i)   // kolom mulai dari 0
        SQLITE_DATA_FLOAT -> st.getDouble(i)
        SQLITE_DATA_TEXT -> st.getText(i)
        else -> throw IllegalStateException("kolom ${st.getColumnName(i)}: BLOB tidak dipakai Gwado")
    }

    companion object {
        /** Buka database (path file atau ":memory:") dan nyalakan foreign key di koneksi ini. */
        fun buka(path: String): Db {
            val db = Db(BundledSQLiteDriver().open(path))
            db.exec("PRAGMA foreign_keys = ON")   // wajib per koneksi; bawaan SQLite mematikannya
            return db
        }
    }
}

/** Pernyataan SQL gagal; pesan memuat SQL-nya (tanpa nilai parameter). */
class SqlGagal(sql: String, sebab: Exception) : RuntimeException(
    "SQL gagal (${sebab.message ?: sebab::class.simpleName}): ${sql.replace(Regex("\\s+"), " ").take(300)}",
    sebab,
)
