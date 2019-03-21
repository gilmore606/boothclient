package com.dlfsystems.BoothClient.fragments

import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.dlfsystems.BoothClient.*
import com.dlfsystems.BoothClient.apimodel.ModelStream
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.nav.Rudder
import com.gauravk.audiovisualizer.visualizer.BarVisualizer
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class PlayingFragment
    : BoothFragment() {

    data class PlayingState(
        val playingStream: ModelStream = ModelStream(),
        val mountPoint: String = "",
        val preparing: Boolean = false,
        val playing: Boolean = false,
        val audioSessionId: Int = -1,
        val trackVote: Int = 0
    )
        : BoothState()

    class PlayingView(
        activity: MainActivity,
        val mainView: View
    )
        : BoothViewController(activity) {

        val playingTitle: TextView = mainView.findViewById(R.id.playing_title)
        val playingArtist: TextView = mainView.findViewById(R.id.playing_artist)
        val visualizer: BarVisualizer = mainView.findViewById(R.id.visualizer)
        val loadingAnim: ProgressBar = mainView.findViewById(R.id.preparing_progressbar)
        val playButton: ImageButton = mainView.findViewById(R.id.playing_play_button)
        val stopButton: ImageButton = mainView.findViewById(R.id.playing_stop_button)
        val thumbsUpButton: ImageButton = mainView.findViewById(R.id.playing_thumbsup_button)
        val thumbsDownButton: ImageButton = mainView.findViewById(R.id.playing_thumbsdown_button)

        override fun render(state: BoothState) {
            state as PlayingState
            loadingAnim.visibleElseGone = state.preparing
            visualizer.visibleElseGone = state.playing
            playButton.visibleElseGone = !(
                    state.playing or state.preparing or (Rudder.lastPlayingMountPoint == "")
                    )
            stopButton.visibility = when {
                (state.preparing) -> { View.INVISIBLE }
                (state.playing) -> { View.VISIBLE }
                else -> { View.GONE }
            }

            thumbsUpButton.visibility = stopButton.visibility
            thumbsDownButton.visibility = stopButton.visibility
            if (state.playing) {
                thumbsUpButton.setImageDrawable(ContextCompat.getDrawable(mainView.context,
                    if (state.trackVote > 0) R.drawable.icon_thumbs_up_lit else R.drawable.icon_thumbs_up))
                thumbsDownButton.setImageDrawable(ContextCompat.getDrawable(mainView.context,
                    if (state.trackVote < 0) R.drawable.icon_thumbs_down_lit else R.drawable.icon_thumbs_down))
            }

            if (!state.playing) {
                playingTitle.text = ""
                playingArtist.text = ""
            } else {
                playingTitle.text = state.playingStream.playing?.track?.title
                playingArtist.text = state.playingStream.playing?.track?.artist
            }
        }

        fun setAudioSessionId(sessionId: Int) {
            visualizer.setAudioSessionId(sessionId)
        }
        override fun onDestroy() {
            visualizer.release()
        }
    }

    class PlayingPresenter(
        activity: MainActivity,
        viewController: PlayingView
    )
        : BoothPresenter(activity, viewController) {

        override fun makeSubscriptions() {
            viewController as PlayingView

            disposables += Rudder.playState.asyncIO()
                .subscribe {
                    val state = lastState as PlayingState
                    changeState(
                        state.copy(
                            mountPoint = it.mountPoint,
                            audioSessionId = it.audioSessionId,
                            preparing = it.preparing,
                            playing = it.playing
                        ))
                    if (it is Rudder.PlayState.Playing)
                        API.refreshStreamInfo()
                }

            disposables += API.streamInfo.asyncIO()
                .subscribe {
                    val state = lastState as PlayingState
                    changeState(
                        state.copy(
                            playingStream = it,
                            mountPoint = it.mountPoint,
                            trackVote = it.playing?.track?.userScore ?: 0
                        ))
                }

            disposables += viewController.mainView.clicks().onMainThread()
                .subscribe {
                    val state = lastState as PlayingState
                    if (state.playing && state.playingStream.playing?.track != null)
                        Rudder.navTo(Rudder.NavTarget.Track(state.playingStream.playing.track))
                }
            disposables += viewController.playButton.clicks().onMainThread()
                .subscribe {
                    Rudder.play(
                        Rudder.PlayTarget.Start(
                            Rudder.lastPlayingMountPoint
                        ))
                }

            disposables += viewController.stopButton.clicks().onMainThread()
                .subscribe {
                    Rudder.play(Rudder.PlayTarget.Stop())
                }

            disposables += viewController.thumbsUpButton.clicks().onMainThread()
                .subscribe {
                    voteOnTrack(if ((lastState as PlayingState).trackVote > 0) 0 else 1)
                }

            disposables += viewController.thumbsDownButton.clicks().onMainThread()
                .subscribe {
                    voteOnTrack(if ((lastState as PlayingState).trackVote < 0) 0 else -1)
                }

            disposables += Rudder.navDest.onMainThread()
                .subscribe {
                    val state = lastState as PlayingState
                    if (it is Rudder.NavTarget.Login)
                        changeState(
                            state.copy(
                                mountPoint = "",
                                audioSessionId = -999,
                                preparing = false,
                                playing = false
                            )
                        )
                }

            disposables += Observable.interval(1000, TimeUnit.MILLISECONDS).skip(1)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val state = lastState as PlayingState
                    if (state.playing &&
                        state.audioSessionId > -1 &&
                        activity.hasRecordPermissions)
                            viewController.setAudioSessionId(state.audioSessionId)
                }
        }

        private fun voteOnTrack(vote: Int) {
            disposables += API.rateTrack((lastState as PlayingState).playingStream.playing?.track?.trackId ?: 0, vote)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    changeState(
                        (lastState as PlayingState).copy(
                            trackVote = vote
                        )
                    )
                }
        }
    }

    override fun makeDefaultState() =
        PlayingState()

    override fun getLayout() =
        R.layout.fragment_playing

    override fun makeViewController(initialState: BoothState) =
        PlayingView(activity as MainActivity, mainView!!)

    override fun makePresenter() =
        PlayingPresenter(activity as MainActivity, viewController as PlayingView)
}
