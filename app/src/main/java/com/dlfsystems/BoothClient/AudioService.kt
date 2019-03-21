package com.dlfsystems.BoothClient

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.util.Log
import android.app.*
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dlfsystems.BoothClient.apimodel.ModelStream
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.nav.Rudder
import io.reactivex.disposables.Disposable

class AudioService : Service(),
    MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener,
    MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    var started = false

    private val broadcastReceiver = BoothBroadcastReceiver()
    private var mediaPlayer = MediaPlayer()
    var playingMountPoint: String = ""
    private var preparing = false
    val notification = NotificationCompat.Builder(this, "DJBooth")
            .setSmallIcon(R.drawable.logo_icon)
            .setContentTitle("DJBooth")
            .setOngoing(true)
            .setSound(null, 0)
            .setDefaults(Notification.DEFAULT_LIGHTS)
    var apiRefreshDisposable: Disposable? = null

    companion object {
        val URL_STREAM = "http://music.tspigot.net:8000/"
        val ACTION_START_PLAYING: String = "START_PLAYING"
        val ACTION_STOP_PLAYING: String = "STOP_PLAYING"
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@AudioService
    }
    override fun onBind(intent: Intent) = LocalBinder()

    inner class BoothBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            action?.let { doServiceCommand(action) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("boothclient", "BOOTHSERVICE heard intent " + intent?.toString())
        intent?.action?.let {
            doServiceCommand(it, (intent.extras?.get("mountPoint") as String?) ?: "")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun doServiceCommand(command: String, extra: String = "") {
        when (command) {
            (ACTION_START_PLAYING) -> {
                startPlaying(extra)
            }
            (ACTION_STOP_PLAYING) -> {
                stopPlaying()
                stopForegroundService()
            }
        }
    }

    fun startForegroundService() {

        if (!started) {

            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(applicationContext)
                .registerReceiver(broadcastReceiver, IntentFilter())

            makeNotification()
            startForeground(1337, notification.build())

            started = true
            Rudder.setPlayState(Rudder.PlayState.Stopped())
        }
    }

    fun makeNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = Intent(applicationContext, AudioService::class.java).setAction(ACTION_STOP_PLAYING)
        val stopPendingIntent = PendingIntent.getBroadcast(applicationContext, 0,
            stopIntent, PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "DJBooth",
                "DJBooth",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        notification.addAction(NotificationCompat.Action.Builder
            (R.drawable.logo_icon, "stop", stopPendingIntent).build())
        notification.setContentIntent(pendingIntent)
    }

    fun stopForegroundService() {

        if (mediaPlayer.isPlaying) stopPlaying()

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(broadcastReceiver)
        apiRefreshDisposable?.dispose()

        mediaPlayer.stop()
        mediaPlayer.release()

        stopForeground(true)
        stopSelf()

        started = false
    }

    fun initMediaPlayer(mountPoint: String) {
        if (mountPoint == playingMountPoint) return

        if (mediaPlayer.isPlaying) {
            stopPlaying()
        }
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnBufferingUpdateListener(this)
        mediaPlayer.setOnInfoListener(this)
        mediaPlayer.setDataSource(URL_STREAM + mountPoint)
        Log.d("boothclient", "init mediaPlayer with mountpoint " + mountPoint)
        playingMountPoint = mountPoint
    }

    fun startPlaying(mountPoint: String) {
        startForegroundService()
        initMediaPlayer(mountPoint)
        startPlaying()
    }

    fun startPlaying() {
        if (!mediaPlayer.isPlaying) {
            if (!preparing) {
                Log.d("boothclient", "asking mediaPlayer to prepare")
                mediaPlayer.prepareAsync()
                preparing = true
                Rudder.setPlayState(Rudder.PlayState.Preparing())
            } else {
                mediaPlayer.start()
                preparing = false
                Rudder.setPlayState(Rudder.PlayState.Playing(playingMountPoint, mediaPlayer.audioSessionId))
                startApiRefresh()
            }
        }
    }

    fun startApiRefresh() {
        apiRefreshDisposable?.dispose()
        apiRefreshDisposable = API.streamInfo.subscribe {
            updateNotification(it)
        }
    }

    fun updateNotification(stream: ModelStream?) {
        NotificationManagerCompat.from(this).apply {
            notification.setContentText(
                if (stream != null) stream.playing?.track?.artist + " - " + stream.playing?.track?.title
                else ""
            )
            notify(1337, notification.build())
        }
    }

    fun stopPlaying() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset()
        }
        playingMountPoint = ""
        updateNotification(null)
        Rudder.setPlayState(Rudder.PlayState.Stopped())
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
        Log.d("boothclient", "BUFFERING " + percent)
    }

    override fun onCompletion(mp: MediaPlayer) {
        stopPlaying()
        stopSelf()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        Log.d("MEDIA ERROR", "ERROR CODE " + extra)
        return false
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        Log.d("boothclient", "MEDIAPLAYER INFO " + what + " / " + extra)
        if (what == 703) {
            Rudder.setPlayState(Rudder.PlayState.Preparing())
        } else if (what == 702 && extra == 0) {
            Rudder.setPlayState(Rudder.PlayState.Playing(playingMountPoint, mediaPlayer.audioSessionId))
        }
        return false
    }

    override fun onPrepared(mp: MediaPlayer) {
        Log.d("boothclient", "AudioService.onPrepared")
        startPlaying()
    }

    override fun onAudioFocusChange(focusState: Int) {
        when (focusState) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (!mediaPlayer.isPlaying) mediaPlayer.start()
                mediaPlayer.setVolume(1f, 1f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
                mediaPlayer = MediaPlayer()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mediaPlayer.isPlaying) mediaPlayer.setVolume(0.2f, 0.2f)
            }
        }
    }
}