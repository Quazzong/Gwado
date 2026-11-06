package io.github.quazzong.gwado.pengingat

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Display
import androidx.core.content.ContextCompat
import java.time.Duration

/** Notifikasi alarm yang sedang berdering punya ID sendiri, di luar notifikasi panel. */
private const val ID_BERDERING = 2

/** Dering berhenti sendiri setelah selama ini. */
private val LAMA_BERDERING: Duration = Duration.ofMinutes(1)

private val POLA_GETAR = longArrayOf(0, 800, 600)

private val SUARA_ALARM: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/** Sinyal sistem saat volume berubah (tidak didokumentasikan resmi, tapi tersedia sejak lama). */
private const val AKSI_VOLUME = "android.media.VOLUME_CHANGED_ACTION"
private const val EKSTRA_JENIS_VOLUME = "android.media.EXTRA_VOLUME_STREAM_TYPE"

/**
 * Dering alarm 3 jam sebelum deadline: nada alarm sistem berulang + getar, lewat jalur alarm
 * (tetap berbunyi saat HP senyap, volumenya ikut volume alarm). Layar dinyalakan dan dijaga
 * menyala selama berdering. Berhenti saat layar dimatikan (tombol power), tombol volume
 * ditekan, notifikasinya digeser atau tombol Matikan ditekan, Gwado dibuka, atau setelah
 * [LAMA_BERDERING]; notifikasinya lalu pindah ke panel tanpa bunyi.
 */
class LayananAlarm : Service() {
    private var dering: Ringtone? = null
    private var getar: Vibrator? = null
    private var sesi: MediaSession? = null
    private var penerima: BroadcastReceiver? = null
    private var layar: PowerManager.WakeLock? = null
    private var pengamatLayar: DisplayManager.DisplayListener? = null
    private var volumeAwal = -1
    private var sekarang: IsiNotif? = null
    private val penunda = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isi = intent?.let(IsiNotif::dariIntent)
        if (isi == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        sekarang?.let(::kePanel)                       // alarm sebelumnya masih berdering
        sekarang = isi
        val matikan = PendingIntent.getBroadcast(
            this, 0, Intent(this, PenerimaPengingat::class.java).setAction(AKSI_MATIKAN),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = bangunNotif(this, isi)
            .setAutoCancel(false)
            .setDeleteIntent(matikan)                  // digeser = matikan (Android 13+)
            .addAction(0, "Matikan", matikan)
            .build()
        startForeground(ID_BERDERING, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        mulaiBunyi()
        penunda.removeCallbacksAndMessages(null)
        penunda.postDelayed({ stopSelf() }, LAMA_BERDERING.toMillis())
        return START_NOT_STICKY
    }

    private fun mulaiBunyi() {
        if (dering == null) {
            nyalakanLayar()
            volumeAwal = getSystemService(AudioManager::class.java).getStreamVolume(AudioManager.STREAM_ALARM)
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            dering = RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = SUARA_ALARM
                isLooping = true
                play()
            }
            dengarTombol()
        }
        if (getar == null) {
            @Suppress("DEPRECATION")
            getar = getSystemService(Vibrator::class.java)?.takeIf { it.hasVibrator() }?.also {
                it.vibrate(VibrationEffect.createWaveform(POLA_GETAR, 0), SUARA_ALARM)
            }
        }
    }

    /**
     * Nyalakan layar dan jaga tetap menyala selama berdering, supaya tombol volume terbaca
     * (saat layar mati, tombol volume hanya diteruskan ke pemutar musik) dan layar tidak padam
     * sendiri lalu dikira tombol power.
     */
    @Suppress("DEPRECATION")
    private fun nyalakanLayar() {
        val pm = getSystemService(PowerManager::class.java)
        layar = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "gwado:alarm",
        ).apply { acquire(LAMA_BERDERING.toMillis() + 5_000) }
    }

    /**
     * Tombol fisik mematikan dering. Volume: sesi media "jarak jauh" menerima tekanan tombol
     * volume (atas maupun bawah) tanpa mengubah volume HP; sebagai cadangan, perubahan volume
     * alarm juga mematikan dering lalu volumenya dikembalikan. Power: layar utama berubah dari
     * menyala ke mati, dibaca langsung dari DisplayManager (sinyal SCREEN_OFF bisa terlambat
     * beberapa detik). Layar yang belum menyala saat dering mulai tidak dihitung.
     */
    private fun dengarTombol() {
        sesi = MediaSession(this, "GwadoAlarm").apply {
            // Tanpa callback, sesi membuang tekanan tombol volume sebelum sampai ke VolumeProvider.
            setCallback(object : MediaSession.Callback() {}, Handler(Looper.getMainLooper()))
            setPlaybackToRemote(object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 1, 0) {
                override fun onAdjustVolume(direction: Int) = matikan()
            })
            setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1f).build())
            isActive = true
        }
        penerima = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getIntExtra(EKSTRA_JENIS_VOLUME, -1) == AudioManager.STREAM_ALARM) {
                    matikan()
                    kembalikanVolume()
                }
            }
        }.also {
            ContextCompat.registerReceiver(this, it, IntentFilter(AKSI_VOLUME), ContextCompat.RECEIVER_NOT_EXPORTED)
        }
        val dm = getSystemService(DisplayManager::class.java)
        var pernahMenyala = dm.getDisplay(Display.DEFAULT_DISPLAY)?.state == Display.STATE_ON
        pengamatLayar = object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                if (displayId != Display.DEFAULT_DISPLAY) return
                val menyala = dm.getDisplay(displayId)?.state == Display.STATE_ON
                if (menyala) pernahMenyala = true else if (pernahMenyala) matikan()
            }
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
        }.also { dm.registerDisplayListener(it, Handler(Looper.getMainLooper())) }
    }

    private fun matikan() {
        stopSelf()
    }

    private fun kembalikanVolume() {
        if (volumeAwal < 0) return
        try {
            getSystemService(AudioManager::class.java).setStreamVolume(AudioManager.STREAM_ALARM, volumeAwal, 0)
        } catch (e: SecurityException) {
            // volume tidak bisa dikembalikan (misalnya saat Jangan Ganggu); dibiarkan
        }
    }

    /** Notifikasi alarm dipasang di panel tanpa bunyi, seperti pengingat lain. */
    private fun kePanel(isi: IsiNotif) {
        pasangDiPanel(this, bangunNotif(this, isi).setSilent(true), isi.tag)
    }

    override fun onDestroy() {
        penunda.removeCallbacksAndMessages(null)
        penerima?.let(::unregisterReceiver)
        penerima = null
        pengamatLayar?.let { getSystemService(DisplayManager::class.java).unregisterDisplayListener(it) }
        pengamatLayar = null
        sesi?.release()
        sesi = null
        dering?.stop()
        dering = null
        getar?.cancel()
        getar = null
        layar?.takeIf { it.isHeld }?.release()
        layar = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        sekarang?.let(::kePanel)
        sekarang = null
        super.onDestroy()
    }
}
