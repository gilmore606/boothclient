package com.dlfsystems.BoothClient.nav

import android.util.Log
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.dlfsystems.BoothClient.AudioService
import com.dlfsystems.BoothClient.MainActivity
import com.dlfsystems.BoothClient.apimodel.ModelTrack
import com.dlfsystems.BoothClient.fragments.*
import io.reactivex.Emitter
import io.reactivex.observables.ConnectableObservable
import io.reactivex.subjects.BehaviorSubject

object Rudder {

    sealed class NavTarget(val addToBackStack: Boolean = true) {
        abstract fun makeKey(): BaseKey

        class Login : NavTarget(false) {
            override fun makeKey() = LoginFragment.LoginKey()
        }
        class Streamlist : NavTarget() {
            override fun makeKey() = StreamlistFragment.StreamlistKey()
        }
        class Stream(val mountPoint: String) : NavTarget() {
            override fun makeKey() = StreamFragment.StreamKey(mountPoint)
        }
        class StreamConfig(val mountPoint: String, val description: String) : NavTarget() {
            override fun makeKey() = StreamConfigFragment.StreamConfigKey(mountPoint, description)
        }
        class Track(val track: ModelTrack) : NavTarget() {
            override fun makeKey() = TrackFragment.TrackKey(track.trackId.toString())
        }
        class Album(val title: String, val artist: String): NavTarget() {
            override fun makeKey() = AlbumFragment.AlbumKey(title, artist)
        }
        class Artist(val name: String): NavTarget() {
            override fun makeKey() = ArtistFragment.ArtistKey(name)
        }
        class Search(val tags: ArrayList<String>, val mountPoint: String) : NavTarget() {
            override fun makeKey() = SearchFragment.SearchKey(tags, mountPoint)
        }
    }

    sealed class PlayTarget {
        abstract fun execute(activity: MainActivity)
        class Stop : PlayTarget() {
            override fun execute(activity: MainActivity) {
                if (playState.value is PlayState.Playing)
                    activity.sendServiceCommand(AudioService.ACTION_STOP_PLAYING)
            }
        }
        class Start(val mountPoint: String) : PlayTarget() {
            override fun execute(activity: MainActivity) {
                activity.startServicePlaying(mountPoint)
            }
        }
    }

    sealed class PlayState(
        val playing: Boolean,
        val preparing: Boolean,
        val mountPoint: String,
        val audioSessionId: Int
    ) {
        class Stopped : PlayState(false, false, "", -1)
        class Preparing : PlayState(false, true, "", -1)
        class Playing(
            mountPoint: String,
            audioSessionId: Int
        ) : PlayState(true, false, mountPoint, audioSessionId)
    }

    lateinit var navDestEmitter: Emitter<NavTarget>

    val navDest: ConnectableObservable<NavTarget> = ConnectableObservable.create<NavTarget> {
        navDestEmitter = it
    }.publish()

    fun navTo(dest: NavTarget) {
        navDestEmitter.onNext(dest)
    }

    lateinit var playEmitter: Emitter<PlayTarget>
    val playTarget: ConnectableObservable<PlayTarget> = ConnectableObservable.create<PlayTarget> {
        playEmitter = it
    }.publish()

    fun play(target: PlayTarget) {
        playEmitter.onNext(target)
        if (target is PlayTarget.Start) {
            Answers.getInstance().logContentView(
                ContentViewEvent()
                    .putContentName("playStream")
                    .putContentId(target.mountPoint)
            )
        }
    }

    val playState: BehaviorSubject<PlayState> = BehaviorSubject.create<PlayState>()

    val playingMountPoint: String?
    get() =
        if (playState.value is PlayState.Playing) playState.value?.mountPoint
        else null

    var lastPlayingMountPoint: String = ""

    fun setPlayState(state: PlayState) {
        Log.d("boothclient", "Emitting playstate " + state.toString())
        if (state.mountPoint != "") lastPlayingMountPoint = state.mountPoint
        playState.onNext(state)
    }

    init {
        navDest.connect()
        playTarget.connect()
    }
}