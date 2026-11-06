package io.github.quazzong.gwado.pengingat

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.quazzong.gwado.MainActivity
import io.github.quazzong.gwado.R
import io.github.quazzong.gwado.db.Gudang
import io.github.quazzong.gwado.layanan.prefsGwado
import io.github.quazzong.gwado.tugas.BarisTugas
import io.github.quazzong.gwado.tugas.daftarTugas
import io.github.quazzong.gwado.widget.EKSTRA_TUGAS
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG_LOG = "Gwado"

/** Kunci SharedPreferences: penanda pengingat yang sudah dikirim (lihat [jatuhTempo]). */
internal const val KUNCI_TERKIRIM = "pengingat_terkirim"

/** Semua notifikasi pengingat di panel memakai ID ini; yang membedakan tag-nya. */
internal const val ID_NOTIF = 1

private const val EKSTRA_JENIS = "io.github.quazzong.gwado.JENIS_PENGINGAT"
private const val EKSTRA_IDS = "io.github.quazzong.gwado.TUGAS_PENGINGAT"

/** Kanal lama alarm yang membunyikan suara lewat notifikasi; sekarang suaranya diputar [LayananAlarm]. */
private const val KANAL_ALARM_LAMA = "pengingat_alarm"

/**
 * Kanal notifikasi per jenis, bisa dimatikan sendiri-sendiri di pengaturan Android.
 * Kanal alarm tanpa suara: dering dan getarnya diputar [LayananAlarm], supaya tetap
 * berbunyi walau HP dalam mode senyap.
 */
private fun kanal(j: Jenis) = if (j == Jenis.ALARM) "pengingat_alarm_dering" else "pengingat_${j.kode}"

/** Pengecekan pengingat tidak boleh berjalan bersamaan (penanda terkirim bisa tertimpa). */
private val giliran = Mutex()

/**
 * Hitung ulang pengingat dari database: tampilkan yang sudah tiba, rapikan notifikasi yang
 * isinya berubah (tugas dicentang, terkumpul, atau deadline-nya pindah), lalu pasang alarm
 * untuk pengingat berikutnya. Dipanggil setelah data berubah, dari alarm, dan setelah HP
 * menyala ulang atau aplikasi diperbarui. Gagal = diam.
 */
suspend fun perbaruiPengingat(context: Context, sekarang: Instant = Instant.now()) = giliran.withLock {
    try {
        val daftar = Gudang.pakai(context) { it.daftarTugas() }
        buatKanal(context)
        val prefs = prefsGwado(context)
        val tempo = jatuhTempo(daftar, sekarang, prefs.getStringSet(KUNCI_TERKIRIM, null).orEmpty())
        if (bolehNotif(context)) {
            for (p in tempo.tampil) {
                if (p.jenis == Jenis.ALARM && bunyikanAlarm(context, p, sekarang)) continue
                tampilkan(context, p, sekarang, ulang = false)
            }
        }
        prefs.edit().putStringSet(KUNCI_TERKIRIM, tempo.terkirim).apply()
        rapikan(context, daftar)
        pasangAlarm(context, berikutnya(daftar, sekarang)?.waktu)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG_LOG, "Pengingat gagal: ${e.javaClass.simpleName}")
    }
}

internal fun bolehNotif(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** Buat kanal yang belum ada; kanal yang sudah ada tidak diubah (pilihan pengguna tetap). */
private fun buatKanal(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java)
    nm.deleteNotificationChannel(KANAL_ALARM_LAMA)
    val alarm = NotificationChannel(kanal(Jenis.ALARM), "3 jam sebelum deadline", NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Muncul di atas layar; nada alarm sistem berbunyi sampai dimatikan, juga saat HP senyap"
        setSound(null, null)
        enableVibration(false)
    }
    nm.createNotificationChannels(
        listOf(
            NotificationChannel(kanal(Jenis.H3), "3 hari sebelum deadline", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Senyap, hanya di panel notifikasi" },
            NotificationChannel(kanal(Jenis.H1), "1 hari sebelum deadline", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Notifikasi biasa" },
            NotificationChannel(kanal(Jenis.PAGI), "Ringkasan pagi", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Jam 07.00: tugas yang deadline hari ini" },
            alarm,
        ),
    )
}

/** Isi satu notifikasi pengingat yang cukup untuk membangunnya lagi (juga lewat Intent). */
internal class IsiNotif(
    val jenis: Jenis,
    val tag: String,
    val teks: TeksPengingat,
    val ids: List<String>,
    val kapan: Instant,
) {
    fun keIntent(i: Intent): Intent = i
        .putExtra(EKSTRA_JENIS, jenis.kode)
        .putExtra(EKSTRA_TAG, tag)
        .putExtra(EKSTRA_JUDUL, teks.judul)
        .putExtra(EKSTRA_ISI, teks.isi)
        .putExtra(EKSTRA_BARIS, teks.baris.toTypedArray())
        .putExtra(EKSTRA_IDS, ids.toTypedArray())
        .putExtra(EKSTRA_KAPAN, kapan.toEpochMilli())

    companion object {
        fun dari(p: Pengingat, kapan: Instant) = IsiNotif(p.jenis, p.tag, teks(p), p.tugas.map { it.id }, kapan)

        fun dariIntent(i: Intent): IsiNotif? {
            val jenis = Jenis.dariKode(i.getStringExtra(EKSTRA_JENIS)) ?: return null
            val tag = i.getStringExtra(EKSTRA_TAG) ?: return null
            val teks = TeksPengingat(
                i.getStringExtra(EKSTRA_JUDUL).orEmpty(),
                i.getStringExtra(EKSTRA_ISI).orEmpty(),
                i.getStringArrayExtra(EKSTRA_BARIS)?.toList().orEmpty(),
            )
            val ids = i.getStringArrayExtra(EKSTRA_IDS)?.toList().orEmpty()
            return IsiNotif(jenis, tag, teks, ids, Instant.ofEpochMilli(i.getLongExtra(EKSTRA_KAPAN, 0)))
        }
    }
}

private const val EKSTRA_TAG = "io.github.quazzong.gwado.TAG_PENGINGAT"
private const val EKSTRA_JUDUL = "io.github.quazzong.gwado.JUDUL_PENGINGAT"
private const val EKSTRA_ISI = "io.github.quazzong.gwado.ISI_PENGINGAT"
private const val EKSTRA_BARIS = "io.github.quazzong.gwado.BARIS_PENGINGAT"
private const val EKSTRA_KAPAN = "io.github.quazzong.gwado.KAPAN_PENGINGAT"

/** Notifikasi pengingat; ketuk = buka Gwado (satu tugas: langsung ke detailnya). */
internal fun bangunNotif(context: Context, n: IsiNotif): NotificationCompat.Builder {
    val buka = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    n.ids.singleOrNull()?.let { buka.putExtra(EKSTRA_TUGAS, it) }
    val ketuk = PendingIntent.getActivity(
        context, n.tag.hashCode(), buka,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val b = NotificationCompat.Builder(context, kanal(n.jenis))
        .setSmallIcon(R.drawable.ikon_jam)
        .setContentTitle(n.teks.judul)
        .setContentText(n.teks.isi)
        .setContentIntent(ketuk)
        .setAutoCancel(true)
        .setWhen(n.kapan.toEpochMilli())
        .setShowWhen(true)
        .setCategory(if (n.jenis == Jenis.ALARM) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
        .addExtras(Bundle().apply {
            putString(EKSTRA_JENIS, n.jenis.kode)
            putStringArray(EKSTRA_IDS, n.ids.toTypedArray())
        })
    if (n.teks.baris.size > 1) {
        b.setStyle(NotificationCompat.InboxStyle().also { s -> n.teks.baris.forEach(s::addLine) })
    } else {
        b.setStyle(NotificationCompat.BigTextStyle().bigText(n.teks.isi))
    }
    return b
}

/** Pasang notifikasi pengingat di panel (tag membedakan pengingatnya). */
internal fun pasangDiPanel(context: Context, b: NotificationCompat.Builder, tag: String) {
    if (!bolehNotif(context)) return
    context.getSystemService(NotificationManager::class.java).notify(tag, ID_NOTIF, b.build())
}

/**
 * Tampilkan satu pengingat. [ulang] = memperbarui notifikasi yang sudah ada tanpa bunyi lagi;
 * [kapan] = waktu yang tertulis di notifikasi.
 */
private fun tampilkan(context: Context, p: Pengingat, kapan: Instant, ulang: Boolean) {
    pasangDiPanel(context, bangunNotif(context, IsiNotif.dari(p, kapan)).setOnlyAlertOnce(ulang), p.tag)
}

/** Mode Jangan Ganggu sedang aktif (filter apa pun selain "semua boleh"). */
private fun janganGanggu(context: Context): Boolean =
    when (context.getSystemService(NotificationManager::class.java).currentInterruptionFilter) {
        NotificationManager.INTERRUPTION_FILTER_ALL, NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> false
        else -> true
    }

/**
 * Mulai dering alarm. Notifikasi H-1 tugas yang sama diangkat dari panel; setelah dering
 * berhenti, [LayananAlarm] memasang notifikasi alarmnya di panel. False kalau Jangan Ganggu
 * aktif, atau Android tidak mengizinkan layanan latar dimulai (misalnya alarm tidak tepat di
 * Android 12+): pengingat tampil sebagai notifikasi tanpa bunyi.
 */
private fun bunyikanAlarm(context: Context, p: Pengingat, kapan: Instant): Boolean = !janganGanggu(context) && try {
    ContextCompat.startForegroundService(
        context,
        IsiNotif.dari(p, kapan).keIntent(Intent(context, LayananAlarm::class.java)),
    )
    context.getSystemService(NotificationManager::class.java).cancel(p.tag, ID_NOTIF)
    true
} catch (e: Exception) {
    Log.w(TAG_LOG, "Dering alarm tidak bisa dimulai: ${e.javaClass.simpleName}")
    false
}

/**
 * Notifikasi pengingat yang masih di panel disesuaikan dengan data sekarang: hilang kalau
 * semua tugasnya sudah beres, diperbarui diam-diam kalau isinya berubah.
 */
private fun rapikan(context: Context, daftar: List<BarisTugas>) {
    val nm = context.getSystemService(NotificationManager::class.java)
    for (sbn in nm.activeNotifications) {
        if (sbn.id != ID_NOTIF || !tagPengingat(sbn.tag)) continue
        val ekstra = sbn.notification.extras
        val jenis = Jenis.dariKode(ekstra.getString(EKSTRA_JENIS)) ?: continue
        val lama = ekstra.getStringArray(EKSTRA_IDS)?.toList().orEmpty()
        val sekarang = anggota(sbn.tag, daftar)
        when {
            sekarang.isEmpty() -> nm.cancel(sbn.tag, ID_NOTIF)
            sekarang.map { it.id } != lama -> {
                val p = Pengingat(jenis, Instant.ofEpochMilli(sbn.notification.`when`), sbn.tag, sekarang)
                tampilkan(context, p, p.waktu, ulang = true)
            }
        }
    }
}

/**
 * Satu alarm untuk pengingat berikutnya; saat berbunyi, [PenerimaPengingat] menghitung ulang dan
 * memasang alarm sesudahnya. Tepat waktu kalau Android mengizinkan alarm tepat; tanpa izin itu
 * (Android 14+ bawaannya mati), alarm bisa meleset.
 */
private fun pasangAlarm(context: Context, waktu: Instant?) {
    val am = context.getSystemService(AlarmManager::class.java)
    val pi = PendingIntent.getBroadcast(
        context, 0, Intent(context, PenerimaPengingat::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    if (waktu == null) {
        am.cancel(pi)
        return
    }
    val ms = waktu.toEpochMilli()
    if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
    } else {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
    }
}

private val AKSI_SISTEM = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
)

/** Action tombol Matikan di notifikasi alarm yang sedang berdering. */
internal const val AKSI_MATIKAN = "io.github.quazzong.gwado.MATIKAN_ALARM"

/** Hentikan dering alarm (kalau ada); notifikasinya tetap di panel. */
fun hentikanAlarm(context: Context) {
    context.stopService(Intent(context, LayananAlarm::class.java))
}

/**
 * Alarm pengingat, HP selesai menyala ulang, aplikasi diperbarui, izin alarm tepat berubah,
 * atau tombol Matikan dering.
 */
class PenerimaPengingat : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AKSI_MATIKAN) {
            hentikanAlarm(context)
            return
        }
        if (intent.action != null && intent.action !in AKSI_SISTEM) return   // alarm sendiri tanpa action
        val selesai = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                perbaruiPengingat(app)
            } finally {
                selesai.finish()
            }
        }
    }
}
