package io.github.quazzong.gwado.ui.komponen

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.quazzong.gwado.beranda.beresDiSitus
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak
import io.github.quazzong.gwado.waktu.Makna
import io.github.quazzong.gwado.waktu.dariDb
import io.github.quazzong.gwado.waktu.sisaChip
import java.time.Instant

/**
 * Kartu satu tugas: judul, layanan + mata kuliah, dan chip sisa waktu.
 * Latar dan chip mengikuti makna sisa waktu terhadap `sekarang`. Tugas yang beres (dicentang
 * atau beres di situs) datar dan bertanda kue bercentang. Yang beres di situs paling redup
 * (judul redup + chip TERKUMPUL atau SELESAI DI SITUS redup); yang hanya dicentang judulnya
 * tetap jelas.
 */
@Composable
fun KartuTugas(
    baris: BarisTugas,
    sekarang: Instant,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    polos: Boolean = false,
    sembunyikanJudul: Boolean = false,
) {
    val w = GwadoTema.warna
    val selesai = baris.status == "done"
    val deadline = baris.deadline?.let(::dariDb)
    val diSitus = beresDiSitus(baris.statusKumpulSitus)
    val beres = selesai || diSitus
    val chip = if (beres) null else deadline?.let { sisaChip(it, sekarang) }
    val tanpaDeadline = !beres && deadline == null
    val latar = when {
        beres || chip == null -> w.kartu
        chip.makna == Makna.LEWAT -> w.latarLewat
        chip.makna == Makna.MEPET -> w.latarMepet
        else -> w.kartu
    }
    val meta = listOfNotNull(baris.sumber ?: "Manual", baris.matkul).joinToString(" · ")

    val bayangan = if (beres || tanpaDeadline) 0.dp else Jarak.bayangan
    Kotak(
        // Posisi diam kartu dicatat untuk kartu terbang (sebelum geseran tekan di dalam Kotak).
        // polos = hanya isi (dipakai kartu terbang, yang menggambar bingkainya sendiri).
        modifier = (if (polos) modifier else modifier.asalKartu(baris, latar, bayangan)).fillMaxWidth(),
        latar = SolidColor(if (polos) Color.Transparent else latar),
        garis = if (polos) Color.Transparent else w.garis,
        putus = tanpaDeadline,
        kilau = if (tanpaDeadline || polos) null else Kilau.Cincin(w.kilau),
        bayangan = bayangan,
        warnaBayangan = if (polos) Color.Transparent else w.bayangan,
        onClick = onClick,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Jarak.s8),
        ) {
            if (beres) IkonSelesai(Modifier.align(Alignment.CenterVertically))
            Column(Modifier.weight(1f)) {
                Text(
                    baris.judul,
                    modifier = (if (polos) Modifier else Modifier.judulKartu(baris.id))
                        .graphicsLayer { alpha = if (sembunyikanJudul) 0f else 1f },
                    style = Huruf.judulKartu,
                    color = if (diSitus) w.tintaRedup else w.tinta,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IkonLayanan(baris.sumber)
                    Text(
                        meta,
                        style = Huruf.meta,
                        color = w.tintaRedup,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            when {
                baris.statusKumpulSitus == "terkumpul" -> Chip("terkumpul", jenis = JenisChip.REDUP)
                diSitus -> Chip("selesai di situs", jenis = JenisChip.REDUP)
                selesai -> Unit
                chip == null -> Chip("tanpa deadline", jenis = JenisChip.PUTUS)
                else -> Chip(
                    chip.label,
                    jenis = when (chip.makna) {
                        Makna.LEWAT -> JenisChip.LEWAT
                        Makna.MEPET -> JenisChip.MEPET
                        Makna.NORMAL -> JenisChip.NETRAL
                    },
                )
            }
        }
    }
}

private fun contoh(id: String, judul: String, deadline: String?, sumber: String?, matkul: String?, status: String = "todo") =
    BarisTugas(
        id = id,
        jenis = if (sumber == null) "manual" else "assign",
        judul = judul,
        deadline = deadline,
        asal = if (sumber == null) "manual" else "sumber",
        statusSitus = "aktif",
        statusKumpulSitus = null,
        matkul = matkul,
        sumber = sumber,
        status = status,
    )

/** Pratinjau semua keadaan kartu pada Selasa 3 Nov 12.00 WIB. */
@Preview(name = "Terang", showBackground = true, widthDp = 360)
@Preview(name = "Gelap", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContohKartu() = GwadoTema {
    val w = GwadoTema.warna
    val sekarang = Instant.parse("2026-11-03T05:00:00Z")
    val daftar = listOf(
        contoh("1", "Laporan akhir praktikum jaringan", "2026-11-01T16:59:00Z", "Brone", "Jaringan Komputer"),
        contoh("2", "Kuis bab 3", "2026-11-03T05:40:00Z", "Eling", "Praktikum Basis Data"),
        contoh("3", "Resume video kuliah tamu", "2026-11-03T16:59:00Z", "Google Classroom", "Kewarganegaraan"),
        contoh("4", "Esai refleksi minggu ke-9 yang judulnya sengaja dibuat sangat panjang", "2026-11-08T16:59:00Z", "Brone", "Bahasa Indonesia"),
        contoh("5", "Beli kertas folio untuk ujian", null, null, null),
        contoh("6", "Tugas 2 metode numerik", "2026-11-02T16:59:00Z", "Brone", "Metode Numerik", status = "done"),
    )
    Column(
        Modifier.background(w.latar).padding(Jarak.tepiLayar),
        verticalArrangement = Arrangement.spacedBy(Jarak.s8),
    ) {
        daftar.forEach { KartuTugas(it, sekarang, onClick = {}) }
    }
}
