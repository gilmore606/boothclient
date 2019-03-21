package com.dlfsystems.BoothClient.fragments

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.dlfsystems.BoothClient.MainActivity
import com.dlfsystems.BoothClient.R
import com.dlfsystems.BoothClient.views.TracklistView
import com.dlfsystems.BoothClient.apimodel.ModelTrack
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.nav.BaseKey
import com.dlfsystems.BoothClient.nav.ContentFragment
import com.dlfsystems.BoothClient.nav.FragAnimPair
import com.dlfsystems.BoothClient.nav.Rudder
import com.dlfsystems.BoothClient.plusAssign
import kotlinx.android.parcel.Parcelize

class AlbumFragment
    : ContentFragment() {

    @Parcelize
    data class AlbumKey(
        val title: String,
        val artist: String
    )
        : BaseKey() {
        override fun createFragment() = AlbumFragment().apply {
            arguments = (arguments ?: Bundle()).also {
                it.putSerializable("title", title)
                it.putSerializable("artist", artist)
            }
        }

        override fun getAnimation() =
                FragAnimPair(R.anim.grow_fade_in_from_bottom, R.anim.stationary)

        override fun getBackAnimation() =
                FragAnimPair(R.anim.stationary, R.anim.shrink_fade_out_from_bottom)
    }

    data class AlbumState(
        val title: String = "",
        val artist: String = "",
        val label: String = "Unknown Label",
        val releaseDate: String = "?",
        val tracks: ArrayList<ModelTrack> = ArrayList(0),
        val imageUrl: String = ""
    )
        : BoothState()

    class AlbumView(
        activity: MainActivity,
        initialState: AlbumState,
        val mainView: View
        )
        : ContentViewController(activity) {

        val scrollView: ScrollView = mainView.findViewById(R.id.album_scrollview)
        val artView: ImageView = mainView.findViewById(R.id.album_image)
        val labelText: TextView = mainView.findViewById(R.id.album_labeltext)
        val trackList: TracklistView = mainView.findViewById(R.id.album_tracklist)

        var loadedImageUrl: String = ""

        init {
            scrollView.viewTreeObserver.addOnScrollChangedListener {
                scrollArt(scrollView.scrollY)
            }
        }

        fun scrollArt(offset: Int) {
            artView.y = (offset / 2).toFloat()
        }

        override fun renderTitle(state: BoothState) {
            state as AlbumState
            updateTitle(state.title, state.artist)
        }

        override fun render(state: BoothState) {
            state as AlbumState

            labelText.text = "Released " + state.releaseDate + " by " + state.label

            trackList.populate(state.tracks)

            if (state.imageUrl != "")
                Glide.with(mainView.context)
                    .load(state.imageUrl)
                    .fitCenter()
                    .placeholder(R.drawable.albumart_placeholder)
                    .crossFade()
                    .into(artView)
        }
    }

    class AlbumPresenter(
        activity: MainActivity,
        viewController: AlbumView
    )
        : ContentPresenter(activity, viewController) {

        override fun makeSubscriptions() {
            viewController as AlbumView
            val state = lastState as AlbumState

            disposables += API.getArtForTrack("", state.artist, state.title)
                .subscribe {
                    changeState(
                        (lastState as AlbumState).copy(
                            imageUrl = it.big
                        )
                    )
                }

            disposables += API.getAlbumTracks(state.title, state.artist)
                .subscribe {
                    changeState(
                        (lastState as AlbumState).copy(
                            tracks = it.results
                        )
                    )
                }
        }
    }

    override fun makeStateFromArguments(arguments: Bundle): BoothState =
            AlbumState(
                title = arguments.getSerializable("title") as String,
                artist = arguments.getSerializable("artist") as String
            )

    override fun makeDefaultState() =
            AlbumState()

    override fun getLayout() =
            R.layout.fragment_album

    override fun makeViewController(initialState: BoothState) = AlbumView(
            activity as MainActivity,
            initialState as AlbumState,
            mainView!!)

    override fun makePresenter() = AlbumPresenter(
        activity as MainActivity,
        viewController as AlbumView)

}