package io.github.quazzong.gwado.ui.komponen

import io.github.quazzong.gwado.ui.tema.gtween
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.ui.tema.Gerak
import io.github.quazzong.gwado.ui.tema.Bentuk
import io.github.quazzong.gwado.ui.tema.GwadoTema
import io.github.quazzong.gwado.ui.tema.Huruf
import io.github.quazzong.gwado.ui.tema.Jarak

/** Label di atas isian (13/800), dengan penanda "opsional". */
@Composable
fun LabelIsian(teks: String, modifier: Modifier = Modifier, opsional: Boolean = false) {
    val w = GwadoTema.warna
    Row(modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(teks, style = Huruf.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.W800), color = w.tinta)
        if (opsional) {
            Text("opsional", style = Huruf.meta.copy(fontSize = 11.sp), color = w.tintaRedup, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** Kalimat galat merah dengan ikon, di bawah isian yang salah. */
@Composable
fun PesanGalat(teks: String, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Row(modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ikon_galat), contentDescription = null, tint = w.galat, modifier = Modifier.size(15.dp))
        Text(teks, style = Huruf.meta.copy(fontWeight = FontWeight.W800), color = w.galat)
    }
}

/**
 * Kolom teks bergaris. Saat fokus muncul bayangan keras (ruangnya selalu disediakan supaya
 * tata letak tidak bergeser); salah = garis berwarna galat.
 */
@Composable
fun IsianTeks(
    nilai: String,
    onUbah: (String) -> Unit,
    modifier: Modifier = Modifier,
    gaya: TextStyle = Huruf.isi.copy(fontWeight = FontWeight.W700),
    petunjuk: String = "",
    salah: Boolean = false,
    minTinggi: Dp = 48.dp,
    satuBaris: Boolean = false,
) {
    val w = GwadoTema.warna
    var fokus by remember { mutableStateOf(false) }
    BasicTextField(
        value = nilai,
        onValueChange = onUbah,
        modifier = modifier.fillMaxWidth().onFocusChanged { fokus = it.isFocused },
        textStyle = gaya.copy(color = w.tinta),
        cursorBrush = SolidColor(w.garis),
        singleLine = satuBaris,
        decorationBox = { isi ->
            Kotak(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = minTinggi + Jarak.bayangan),
                garis = if (salah) w.galat else w.garis,
                warnaBayangan = if (fokus) w.bayangan else Color.Transparent,
                padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (nilai.isEmpty()) Text(petunjuk, style = gaya.copy(fontWeight = FontWeight.W600), color = w.tintaRedup)
                isi()
            }
        },
    )
}

/** Kotak pilihan yang membuka lembar atau dialog; teks kecil opsional di bawahnya. */
@Composable
fun Pemilih(
    teks: String,
    onClick: () -> Unit,
    @DrawableRes ikon: Int,
    modifier: Modifier = Modifier,
    kecil: String? = null,
    redup: Boolean = false,
) {
    val w = GwadoTema.warna
    Kotak(
        modifier.defaultMinSize(minHeight = 48.dp),
        bayangan = 0.dp,
        padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        rataIsi = Alignment.CenterStart,
        onClick = onClick,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    teks,
                    style = Huruf.isi.copy(fontWeight = FontWeight.W800),
                    color = if (redup) w.tintaRedup else w.tinta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (kecil != null) Text(kecil, style = Huruf.meta.copy(fontSize = 11.sp), color = w.tintaRedup)
            }
            Icon(painterResource(ikon), contentDescription = null, tint = w.tintaRedup, modifier = Modifier.size(18.dp))
        }
    }
}

/** Sakelar nyala/mati berlabel; ibu jari bergeser 120 ms. */
@Composable
fun Sakelar(nyala: Boolean, onUbah: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    val geser by animateDpAsState(if (nyala) 20.dp else 0.dp, gtween(Gerak.CEPAT), label = "sakelar")
    val bentuk = RoundedCornerShape(12.dp)
    Row(
        modifier.minimumInteractiveComponentSize().toggleable(value = nyala, role = Role.Switch, onValueChange = onUbah),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Huruf.meta.copy(fontWeight = FontWeight.W800), color = w.tintaRedup)
        Box(Modifier.size(44.dp, 24.dp).background(if (nyala) w.aksen else w.ubin, bentuk).border(Jarak.garis, w.garis, bentuk)) {
            Box(Modifier.offset(x = 4.dp + geser, y = 4.dp).size(16.dp).background(w.garis, CircleShape))
        }
    }
}

/** Kotak info bergaris putus-putus, satu kalimat redup. */
@Composable
fun KotakInfo(teks: String, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Kotak(
        modifier.fillMaxWidth(),
        putus = true,
        kilau = null,
        bayangan = 0.dp,
        padding = PaddingValues(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Text(teks, style = Huruf.isi.copy(fontSize = 13.sp), color = w.tintaRedup)
    }
}

/** Radio berbentuk kue: terisi gradasi aksen + centang saat dipilih. */
@Composable
fun RadioKue(pilih: Boolean, modifier: Modifier = Modifier, ukuran: Dp = 22.dp) {
    val w = GwadoTema.warna
    val isi = w.gradasiAksen()
    Canvas(modifier.size(ukuran)) {
        val s = size.minDimension / 22f
        val kue = pathGelombang(Offset(11 * s, 11 * s), 8.5f * s, 9, 0.1f)
        if (pilih) drawPath(kue, isi) else drawPath(kue, w.kartu)
        drawPath(kue, w.garis, style = Stroke(2 * s))
        if (pilih) {
            val centang = Path().apply {
                moveTo(7 * s, 11.5f * s)
                relativeLineTo(3 * s, 3 * s)
                relativeLineTo(5 * s, -6 * s)
            }
            drawPath(centang, Color(0xFF3A1D36), style = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Strip judul layar sendiri (berbayangan ganda, taburan) dengan slot kiri dan kanan. */
@Composable
fun StripSub(
    judul: String,
    modifier: Modifier = Modifier,
    benih: Int = 21,
    ukuran: TextUnit = 24.sp,
    kiri: @Composable () -> Unit = {},
    kanan: @Composable () -> Unit = {},
) {
    val w = GwadoTema.warna
    Kotak(
        modifier.fillMaxWidth(),
        bentuk = Bentuk.header,
        latar = gradasiCss(115f, w.strip),
        bayanganKedua = 6.dp,
        padding = PaddingValues(Jarak.garis * 2),
    ) {
        Taburan(benih, Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)), tinggi = 70f, mulai = 0.4f)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                kiri()
                TeksStiker(judul, ukuran = ukuran, garis = 2.dp)
            }
            kanan()
        }
    }
}

/** Tombol bergaris persegi berisi ikon (menu ⋯). */
@Composable
fun TombolGarisIkon(@DrawableRes ikon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val w = GwadoTema.warna
    Kotak(
        modifier.size(Jarak.sentuh + Jarak.bayanganKecil),
        bentuk = Bentuk.tombol,
        kilau = null,
        bayangan = Jarak.bayanganKecil,
        padding = PaddingValues(0.dp),
        rataIsi = Alignment.Center,
        onClick = onClick,
    ) {
        Icon(painterResource(ikon), contentDescription = label, tint = w.tinta, modifier = Modifier.size(20.dp))
    }
}

/**
 * Lembar bawah bergaris dengan pegangan; tirai gelap di belakangnya. Latar, garis, dan
 * pegangan digambar di dalam isi lembar supaya ikut bergerak saat lembar naik dan turun.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarBawah(onTutup: () -> Unit, isi: @Composable ColumnScope.() -> Unit) {
    val w = GwadoTema.warna
    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = Bentuk.lembarBawah,
        containerColor = Color.Transparent,
        contentColor = w.tinta,
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF1C1220).copy(alpha = 0.42f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(w.kartu, Bentuk.lembarBawah)
                .border(Jarak.garis, w.garis, Bentuk.lembarBawah)
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 10.dp)
                    .size(40.dp, 5.dp)
                    .background(w.garisHalus, RoundedCornerShape(3.dp)),
            )
            isi()
        }
    }
}
