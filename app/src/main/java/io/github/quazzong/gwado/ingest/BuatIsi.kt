package io.github.quazzong.gwado.ingest

import io.github.quazzong.gwado.fetch.FetchError
import io.github.quazzong.gwado.fetch.iso
import io.github.quazzong.gwado.sumber.HasilSumber
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FORMAT_ID = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
private val acak = SecureRandom()

/** 'brone-20261020T050002Z-3f9a1c': sumber, waktu mulai UTC, 6 hex acak (inbox.buat_fetch_id). */
fun buatFetchId(sumber: String, mulai: Instant): String {
    val b = ByteArray(3).also { acak.nextBytes(it) }
    return "$sumber-${FORMAT_ID.format(mulai)}-" + b.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

fun isiSukses(sumber: String, mulai: Instant, selesai: Instant, hasil: HasilSumber) =
    Isi(buatFetchId(sumber, mulai), sumber, iso(mulai), iso(selesai), hasil = hasil)

/** Run gagal tetap dicatat (source_runs 'failed'). Pesan dipotong 300 karakter seperti inbox.bungkus. */
fun isiGagal(sumber: String, mulai: Instant, selesai: Instant, e: FetchError) =
    Isi(buatFetchId(sumber, mulai), sumber, iso(mulai), iso(selesai), error = ErrorFetch(e.jenis, e.pesan.take(300)))
