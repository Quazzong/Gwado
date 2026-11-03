package io.github.quazzong.gwado.db

/** File migrasi di src/main/resources/migrasi, nomor 1, 2, 3, … tanpa lompat. */
val MIGRASI = listOf("0001_awal.sql", "0002_pengecualian_matkul.sql")

private val NAMA_MIGRASI = Regex("""^(\d{4})_[a-z0-9_]+\.sql$""")

fun bacaMigrasi(nama: String): String =
    Db::class.java.getResourceAsStream("/migrasi/$nama")
        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: error("file migrasi tidak ditemukan: $nama")

/** Terapkan migrasi yang belum diterapkan; kembalikan nama file yang dijalankan. */
fun Db.migrasi(): List<String> {
    val nomor = MIGRASI.map { n ->
        NAMA_MIGRASI.matchEntire(n)?.groupValues?.get(1)?.toInt() ?: error("nama migrasi tidak valid: $n")
    }
    check(nomor == (1..nomor.size).toList()) { "nomor migrasi harus berurutan mulai dari 1: $nomor" }

    exec("PRAGMA journal_mode = WAL")   // tersimpan permanen di file database; aman diulang
    val versi = (satu("PRAGMA user_version")!!["user_version"] as Long).toInt()
    check(versi <= nomor.size) { "database versi $versi, tapi kode hanya kenal sampai ${nomor.size}" }

    val dijalankan = mutableListOf<String>()
    for ((i, nama) in MIGRASI.withIndex()) {
        val n = i + 1
        if (n <= versi) continue
        // isi migrasi + nomor versi baru dalam SATU transaksi
        transaksi {
            pisahPernyataan(bacaMigrasi(nama)).forEach { exec(it) }
            exec("PRAGMA user_version = $n")
        }
        dijalankan += nama
    }
    return dijalankan
}

/**
 * Pecah skrip SQL per ';', melewati komentar (-- dan /* */) dan teks '...'. Komentar dibuang.
 * Perlu karena prepare() hanya menjalankan pernyataan pertama (beda dengan executescript Python).
 */
fun pisahPernyataan(sql: String): List<String> {
    val hasil = mutableListOf<String>()
    val kini = StringBuilder()
    fun simpan() {
        kini.toString().trim().takeIf { it.isNotEmpty() }?.let { hasil += it }
        kini.clear()
    }
    var dalamTeks = false
    var i = 0
    while (i < sql.length) {
        val c = sql[i]
        val d = sql.getOrNull(i + 1)
        when {
            dalamTeks -> {
                kini.append(c)
                if (c == '\'') {
                    if (d == '\'') { kini.append(d); i++ } else dalamTeks = false   // '' = kutip di dalam teks
                }
            }
            c == '\'' -> { kini.append(c); dalamTeks = true }
            c == '-' && d == '-' -> {
                while (i < sql.length && sql[i] != '\n') i++
                continue
            }
            c == '/' && d == '*' -> {
                val tutup = sql.indexOf("*/", i + 2)
                i = if (tutup < 0) sql.length else tutup + 2
                continue
            }
            c == ';' -> simpan()
            else -> kini.append(c)
        }
        i++
    }
    check(!dalamTeks) { "teks '...' tidak ditutup" }
    simpan()
    return hasil
}
