package io.github.quazzong.gwado.sumber

/** Kelas dari satu sumber. Bentuknya sama dengan format inbox SERPER. */
data class Kelas(
    val externalId: String,
    val nama: String,
    val shortname: String?,
    val kodeMk: String?,
    val kategori: String?,
    val mulai: String?,
    val selesai: String?,
    val dibuat: String?,
)

/** Tugas dari satu sumber. Waktu selalu UTC, format "2026-10-20T05:00:00Z". */
data class Tugas(
    val externalId: String,
    val courseExternalId: String,
    val jenis: String,
    val linkId: String?,
    val judul: String,
    val deskripsi: String,
    val deadline: String?,
    val url: String?,
    val statusKumpulSitus: String?,
)

data class HasilSumber(val kelas: List<Kelas>, val tugas: List<Tugas>)
