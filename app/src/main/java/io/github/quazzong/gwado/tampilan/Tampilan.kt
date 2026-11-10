package io.github.quazzong.gwado.tampilan

import android.content.SharedPreferences
import io.github.quazzong.gwado.db.Db

/** Tekstur latar layar. kode = nilai di preferences `tekstur`. */
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

/** Tema tampilan. kode = nilai di SharedPreferences `tema`. */
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

private const val KUNCI_WARNA_MODE = "warna_mode"
private const val KUNCI_WARNA_CONTOH = "warna_contoh"
private const val KUNCI_WARNA_BEBAS = "warna_bebas"
private const val KUNCI_WARNA_TERANG = "warna_lanjut_terang"
private const val KUNCI_WARNA_GELAP = "warna_lanjut_gelap"

/** Pilihan warna, di SharedPreferences seperti tema; nilai rusak → bawaan. */
fun SharedPreferences.pilihanWarna(): PilihanWarna {
    val bawaan = PilihanWarna()
    val contoh = getString(KUNCI_WARNA_CONTOH, null)
        ?.takeIf { k -> k == PilihanWarna.KODE_BEBAS || CONTOH_WARNA.any { it.kode == k } } ?: bawaan.contoh
    return PilihanWarna(
        mode = ModeWarna.entries.firstOrNull { it.kode == getString(KUNCI_WARNA_MODE, null) } ?: bawaan.mode,
        contoh = contoh,
        bebas = dariKodeLch(getString(KUNCI_WARNA_BEBAS, null)) ?: bawaan.bebas,
        lanjutTerang = dariKodeKelompok(getString(KUNCI_WARNA_TERANG, null)),
        lanjutGelap = dariKodeKelompok(getString(KUNCI_WARNA_GELAP, null)),
    )
}

fun SharedPreferences.simpanPilihanWarna(p: PilihanWarna) {
    edit()
        .putString(KUNCI_WARNA_MODE, p.mode.kode)
        .putString(KUNCI_WARNA_CONTOH, p.contoh)
        .putString(KUNCI_WARNA_BEBAS, kodeLch(p.bebas))
        .putString(KUNCI_WARNA_TERANG, kodeKelompok(p.lanjutTerang))
        .putString(KUNCI_WARNA_GELAP, kodeKelompok(p.lanjutGelap))
        .apply()
}
