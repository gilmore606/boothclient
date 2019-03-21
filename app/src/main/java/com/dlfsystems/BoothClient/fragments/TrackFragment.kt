package com.dlfsystems.BoothClient.fragments

import android.os.Bundle
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.dlfsystems.BoothClient.*
import com.dlfsystems.BoothClient.apimodel.ModelTrack
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.apis.ApiCall
import com.dlfsystems.BoothClient.nav.FragAnimPair
import com.dlfsystems.BoothClient.nav.BaseKey
import com.dlfsystems.BoothClient.nav.ContentFragment
import com.dlfsystems.BoothClient.nav.Rudder
import com.dlfsystems.BoothClient.views.Tagbag
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.parcel.Parcelize

class TrackFragment : ContentFragment() {

    @Parcelize
    data class TrackKey(val trackId: String)
        : BaseKey() {

        override fun createFragment(): ContentFragment = TrackFragment().apply {
            arguments = (arguments ?: Bundle()).also { bundle ->
                bundle.putSerializable("trackid", trackId)
            }
        }

        override fun getAnimation() =
                FragAnimPair(R.anim.slide_in_left, R.anim.stationary)

        override fun getBackAnimation() =
                FragAnimPair(R.anim.stationary, R.anim.slide_out_right)
    }

    data class TrackState(
        val track: ModelTrack = ModelTrack(),
        val imageUrl: String = ""
    ) : BoothState()

    class TrackView(
        activity: MainActivity,
        val mainView: View
    )
        : ContentViewController(activity) {

        val scrollView: ScrollView = mainView.findViewById(R.id.track_scrollview)
        val tagbagView: Tagbag = mainView.findViewById(R.id.trackdetail_tagbag)
        val artView: ImageView = mainView.findViewById(R.id.trackdetail_image)
        val actionButton: ImageButton = mainView.findViewById(R.id.action_button)
        val actionButtonLabel: TextView = mainView.findViewById(R.id.action_button_label)
        val albumButton: TextView = mainView.findViewById(R.id.track_album_button)
        val artistButton: TextView = mainView.findViewById(R.id.track_artist_button)

        init {
            scrollView.viewTreeObserver.addOnScrollChangedListener {
                scrollArt(scrollView.scrollY)
            }
        }

        fun scrollArt(offset: Int) {
            artView.y = (offset / 2).toFloat()
        }

        override fun renderTitle(state: BoothState) {
            state as TrackState
            updateTitle(state.track.title, state.track.artist)
        }

        override fun render(state: BoothState) {
            state as TrackState

            tagbagView.replaceTags(state.track.tags)

            artistButton.text = "More from " + state.track.artist
            albumButton.text = "More from album '" + state.track.album + "'"

            if (state.imageUrl != "")
                Glide.with(mainView.context)
                    .load(state.imageUrl)
                    .fitCenter()
                    .placeholder(R.drawable.albumart_placeholder)
                    .crossFade()
                    .into(artView)

            val stream = API.streamInfo.value
            var buttonResource = -1
            var buttonTextResource = -1
            when {
                (stream == null) -> { }
                (stream.playing?.track?.trackId == state.track.trackId) -> {
                    buttonResource = R.drawable.icon_skip
                    buttonTextResource = R.string.buttonlabel_skip
                }
                (stream.queue.any { it.track.trackId == state.track.trackId }) -> {
                    buttonResource = R.drawable.icon_unqueue
                    buttonTextResource = R.string.buttonlabel_unqueue
                }
                else -> {
                    buttonResource = R.drawable.icon_queue
                    buttonTextResource = R.string.buttonlabel_queue
                }
            }
            if (buttonResource > -1) {
                actionButton.setImageDrawable(mainView.context.getDrawable(buttonResource))
                actionButtonLabel.text = getString(buttonTextResource)
            } else {
                actionButton.visibility = View.INVISIBLE
                actionButtonLabel.visibility = View.INVISIBLE
            }
        }
    }

    class TrackPresenter(
        activity: MainActivity,
        viewController: TrackView
    )
        : ContentPresenter(activity, viewController) {

        override fun makeSubscriptions() {
            viewController as TrackView

            disposables += ApiCall(API.getTrackInfo((lastState as TrackState).track.trackId)) {
                val state = lastState as TrackState
                changeState(
                    state.copy(
                        track = it
                    )
                )
                fetchArt()
            }

            disposables += viewController.actionButton.clicks().onMainThread()
                .subscribe {
                    contextButtonClick()
                }

            disposables += viewController.albumButton.clicks().onMainThread()
                .subscribe {
                    val state = lastState as TrackState
                    Rudder.navTo(Rudder.NavTarget.Album(state.track.album, state.track.artist))
                }

            disposables += viewController.artistButton.clicks().onMainThread()
                .subscribe {
                    val state = lastState as TrackState
                    Rudder.navTo(Rudder.NavTarget.Artist(state.track.artist))
                }
        }

        private fun fetchArt() {
            val state = lastState as TrackState
            disposables += API.getArtForTrack(state.track.title, state.track.artist, state.track.album)
                .subscribe {
                    changeState(
                        state.copy(
                            imageUrl = it.big
                        )
                    )
                }
        }

        private fun contextButtonClick() {
            val stream = API.streamInfo.value
            val state = lastState as TrackState
            when {
                stream == null -> { }
                stream.playing?.track?.trackId == state.track.trackId -> {
                    skipTrack(state.track.trackId, state.track.title)
                }
                stream.queue.any { it.track.trackId == state.track.trackId } -> {
                    unqueueTrack(state.track.trackId, state.track.title)
                }
                else -> {
                    queueTrack(state.track.trackId, state.track.title)
                }
            }
        }

        private fun skipTrack(trackId: Int, title: String) {

        }

        private fun queueTrack(trackId: Int, title: String) {
            if (Rudder.lastPlayingMountPoint != "") {
                disposables += API.queueTrack(
                    Rudder.lastPlayingMountPoint,
                    trackId
                )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        activity.makeToast("Added " + title + " to queue.")
                        activity.navigateBack()
                    }, {
                    })
            }
        }

        private fun unqueueTrack(trackId: Int, title: String) {
            if (Rudder.playingMountPoint != null) {
                disposables += API.unqueueTrack(
                    Rudder.playingMountPoint!!,
                    trackId
                )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        activity.makeToast("Removed " + title + " from queue.")
                        activity.navigateBack()
                    }, {
                    })
            }
        }
    }

    override fun makeStateFromArguments(arguments: Bundle): BoothState =
        TrackState(
            track = ModelTrack(
                trackId = (arguments.getSerializable(
                    "trackid"
                ) as String).toInt()
            )
        )

    override fun makeDefaultState() =
        TrackState()

    override fun getLayout() =
        R.layout.fragment_track

    override fun makeViewController(initialState: BoothState) = TrackView(
            activity as MainActivity,
            mainView!!
        )

    override fun makePresenter() = TrackPresenter(
        activity as MainActivity,
        viewController as TrackView
    )
}