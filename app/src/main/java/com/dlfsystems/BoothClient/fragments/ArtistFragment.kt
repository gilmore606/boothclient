package com.dlfsystems.BoothClient.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.dlfsystems.BoothClient.MainActivity
import com.dlfsystems.BoothClient.R
import com.dlfsystems.BoothClient.views.SwipeRevealLayout
import com.dlfsystems.BoothClient.apimodel.ModelTrack
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.nav.BaseKey
import com.dlfsystems.BoothClient.nav.ContentFragment
import com.dlfsystems.BoothClient.nav.FragAnimPair
import com.dlfsystems.BoothClient.nav.Rudder
import com.dlfsystems.BoothClient.plusAssign
import com.dlfsystems.BoothClient.views.TracklistView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.parcel.Parcelize

class ArtistFragment
    : ContentFragment() {

    @Parcelize
    data class ArtistKey(
        val name: String
    )
        : BaseKey() {
        override fun createFragment() = ArtistFragment().apply {
            arguments = (arguments ?: Bundle()).also {
                it.putSerializable("name", name)
            }
        }

        override fun getAnimation() =
            FragAnimPair(R.anim.grow_fade_in_from_bottom, R.anim.stationary)

        override fun getBackAnimation() =
            FragAnimPair(R.anim.stationary, R.anim.shrink_fade_out_from_bottom)
    }

    data class ArtistState(
        val name: String = "?",
        val tracks: ArrayList<ModelTrack> = ArrayList(0),
        val imageUrl: String = ""
    )
        : BoothState()

    class ArtistView(
        activity: MainActivity,
        initialState: ArtistState,
        val mainView: View
    )
        : ContentViewController(activity) {

        val scrollView: ScrollView = mainView.findViewById(R.id.artist_scrollView)
        val artView: ImageView = mainView.findViewById(R.id.artist_image)
        val trackList: TracklistView = mainView.findViewById(R.id.artist_tracklist)

        init {
            scrollView.viewTreeObserver.addOnScrollChangedListener {
                scrollArt(scrollView.scrollY)
            }
        }

        fun scrollArt(offset: Int) {
            artView.y = (offset / 2).toFloat()
        }

        override fun renderTitle(state: BoothState) {
            state as ArtistState
            updateTitle(state.name)
        }

        override fun render(state: BoothState) {
            state as ArtistState

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

    class ArtistPresenter(
        activity: MainActivity,
        viewController: ArtistView
    )
        : ContentPresenter(activity, viewController) {

        override fun makeSubscriptions() {
            viewController as ArtistView
            val state = lastState as ArtistState

            disposables += API.getArtistTracks(state.name)
                .subscribe {
                    changeState(
                        (lastState as ArtistState).copy(
                            tracks = it.results
                        )
                    )
                }

            disposables += API.getArtForArtist(state.name)
                .subscribe {
                    changeState(
                        (lastState as ArtistState).copy(
                            imageUrl = it
                        )
                    )
                }
        }
    }

    override fun makeStateFromArguments(arguments: Bundle): BoothState =
            ArtistState(
                name = arguments.getSerializable("name") as String
            )

    override fun makeDefaultState() =
            ArtistState()

    override fun getLayout() =
            R.layout.fragment_artist

    override fun makeViewController(initialState: BoothState) = ArtistView(
        activity as MainActivity,
        initialState as ArtistState,
        mainView!!)

    override fun makePresenter() = ArtistPresenter(
        activity as MainActivity,
        viewController as ArtistView)


}