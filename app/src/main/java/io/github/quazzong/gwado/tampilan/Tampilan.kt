package io.github.quazzong.gwado.tampilan

import android.content.SharedPreferences
import io.github.quazzong.gwado.db.Db

/** Tekstur latar layar (DESAIN §7). kode = nilai di preferences `tekstur`. */
enum class Tekstur(val kode: String, val label: String) {
    POLOS("polos", "Polos"),
    TITIK("titik", "Titik"),
    KOTAK("kotak", "Kotak");

    companion object {
        val BAWAAN = TITIK

        /**
         * Kode tak dikenal atau kosong → bawaan, supaya preferences yang rusak (atau pilihan yang
         * sudah tidak ada, misalnya "bentuk") tidak membuat layar galat.
         */
        fun dariKode(kode: String?): Tekstur = entries.firstOrNull { it.kode == kode } ?: BAWAAN
    }
}

private const val KUNCI_TEKSTUR = "tekstur"

fun Db.tekstur(): Tekstur =
    Tekstur.dariKode(satu("SELECT nilai FROM preferences WHERE kunci = ?", KUNCI_TEKSTUR)?.get("nilai") as String?)

fun Db.simpanTekstur(t: Tekstur) {
    exec(
        "INSERT INTO preferences (kunci, nilai) VALUES (?, ?) ON CONFLICT (kunci) DO UPDATE SET nilai = excluded.nilai",
        KUNCI_TEKSTUR, t.kode,
    )
}

/** Tema tampilan (DESAIN §9.10). kode = nilai di SharedPreferences `tema`. */
enum class Tema(val kode: String, val label: String) {
    SISTEM("sistem", "Ikut sistem"),
    TERANG("terang", "Terang"),
    GELAP("gelap", "Gelap");

    /** Apakah tampilan gelap, kalau tampilan sistem sedang [sistemGelap]. */
    fun gelap(sistemGelap: Boolean): Boolean = when (this) {
        SISTEM -> sistemGelap
        TERANG -> false
        GELAP -> true
    }

    companion object {
        val BAWAAN = SISTEM

        /** Kode tak dikenal atau kosong → bawaan. */
        fun dariKode(kode: String?): Tema = entries.firstOrNull { it.kode == kode } ?: BAWAAN
    }
}

private const val KUNCI_TEMA = "tema"

/**
 * Tema disimpan di SharedPreferences, bukan database, supaya bisa dibaca langsung saat
 * aplikasi dibuka (tanpa kedip tema yang salah selama database dimuat).
 */
fun SharedPreferences.tema(): Tema = Tema.dariKode(getString(KUNCI_TEMA, null))

fun SharedPreferences.simpanTema(t: Tema) {
    edit().putString(KUNCI_TEMA, t.kode).apply()
}
