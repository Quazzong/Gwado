package io.github.quazzong.gwado.tampilan

import io.github.quazzong.gwado.db.Db

/** Tekstur latar layar (DESAIN §7). kode = nilai di preferences `tekstur`. */
enum class Tekstur(val kode: String, val label: String) {
    POLOS("polos", "Polos"),
    TITIK("titik", "Titik"),
    KOTAK("kotak", "Kotak"),
    BENTUK("bentuk", "Bentuk acak");

    companion object {
        val BAWAAN = TITIK

        /** Kode tak dikenal atau kosong → bawaan, supaya preferences yang rusak tidak membuat layar galat. */
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
