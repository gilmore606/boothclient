package com.dlfsystems.BoothClient.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.dlfsystems.BoothClient.*
import com.dlfsystems.BoothClient.apimodel.ModelStream
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.apis.ApiCall
import com.dlfsystems.BoothClient.nav.FragAnimPair
import com.dlfsystems.BoothClient.nav.BaseKey
import com.dlfsystems.BoothClient.nav.ContentFragment
import com.dlfsystems.BoothClient.nav.Rudder
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.parcel.Parcelize
import java.util.concurrent.TimeUnit

class StreamlistFragment : ContentFragment() {

    companion object {
        val ACTION_CREATE = 2001
    }

    @Parcelize
    data class StreamlistKey(val tag: String) : BaseKey() {
        constructor() : this("StreamlistKey")
        override fun createFragment() = StreamlistFragment()

        override fun getAnimation() =
                FragAnimPair(R.anim.grow_fade_in_from_bottom, R.anim.shrink_fade_out_from_bottom)
    }

    data class StreamlistState(
        val streamList: ArrayList<ModelStream> = ArrayList(0),
        val playingMountPoint: String = ""
    ) : BoothState()

    data class StreamlistItem(val stream: ModelStream, val isPlaying: Boolean)

    class StreamlistView(
        activity: MainActivity,
        mainView: View
    )
        : ContentViewController(activity) {

        val context: Context = mainView.context
        val recyclerView: androidx.recyclerview.widget.RecyclerView = mainView.findViewById(R.id.streamlist_recycler)
        val recyclerCache = ArrayList<StreamlistItem>(0)
        val recyclerAdapter =
            StreamlistRecyclerAdapter(recyclerCache)
        init {
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            recyclerView.adapter = recyclerAdapter
        }

        override fun renderTitle(state: BoothState) = updateTitle("Streams", "", false)

        override fun renderActions(state: BoothState, menu: Menu) {
            state as StreamlistState
            menu.add(
                Menu.NONE,
                ACTION_CREATE,
                1,
                "Create stream"
            ).setIcon(R.drawable.create_stream_icon)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        override fun render(state: BoothState) {
            state as StreamlistState

            recyclerCache.clear()
            state.streamList.forEach {
                recyclerCache.add(
                    StreamlistItem(
                        it, (it.mountPoint == state.playingMountPoint)
                    ))
            }
            recyclerAdapter.updateStreams(recyclerCache)
        }
    }

    class StreamlistPresenter(
        activity: MainActivity,
        viewController: StreamlistView
    )
        : ContentPresenter(activity, viewController) {

        override fun makeSubscriptions() {
            viewController as StreamlistView
            disposables += viewController.recyclerAdapter.clickEvent.subscribe {
                    Rudder.play(Rudder.PlayTarget.Start(it.mountPoint))
                    Rudder.navTo(Rudder.NavTarget.Stream(it.mountPoint))
                }

            disposables += Rudder.playState.onMainThread()
                .subscribe {
                    val state = lastState as StreamlistState
                        changeState(
                            state.copy(
                                playingMountPoint = it.mountPoint
                            ))
                }

            disposables += Observable.interval(2000, TimeUnit.MILLISECONDS)
                .subscribe {
                    refreshFromApi()
                }
            refreshFromApi()
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                (ACTION_CREATE) -> {
                    Rudder.navTo(Rudder.NavTarget.StreamConfig("", ""))
                }
                else -> { return false }
            }
            return true
        }

        private fun refreshFromApi() {
            disposables += ApiCall(API.getStreamList()) {
                changeState(
                    (lastState as StreamlistState).copy(
                        streamList = it
                    )
                )
            }
        }
    }

    class StreamlistRecyclerAdapter(private var streams: ArrayList<StreamlistItem>)
        : androidx.recyclerview.widget.RecyclerView.Adapter<StreamlistRecyclerAdapter.StreamlistHolder>() {

        private val clickSubject = PublishSubject.create<ModelStream>()
        val clickEvent: Observable<ModelStream> = clickSubject

        inner class StreamlistHolder(var view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            private var disposables = CompositeDisposable()
            private var imageLoadedForTrack: Int = -1
            private var stream: ModelStream? = null
            private var nameText: TextView = view.findViewById(R.id.streamlist_item_name)
            private var playingText: TextView = view.findViewById(R.id.streamlist_item_playing)
            private var spinningText: TextView = view.findViewById(R.id.streamlist_item_spinning)
            private var logoImage: ImageView = view.findViewById(R.id.streamlist_item_logo)
            private var playingImage: ImageView = view.findViewById(R.id.streamlist_item_logo_overlay)
            init {
                view.setOnClickListener {
                    clickSubject.onNext(streams[layoutPosition].stream)
                }
            }
            fun bindStream(item: StreamlistItem) {
                this.stream = item.stream
                nameText.text = item.stream.mountPoint
                val artistString = item.stream.playing?.track?.artist ?: "?"
                val titleString = item.stream.playing?.track?.title ?: "?"
                playingText.text = "$artistString - $titleString"
                spinningText.text = item.stream.profile.spinning.joinToString(" ")
                val playingTrack = item.stream.playing?.track
                if (playingTrack != null && imageLoadedForTrack != playingTrack.trackId) {
                    disposables += API.getArtForTrack(playingTrack.title, playingTrack.artist, playingTrack.album)
                        .subscribe {
                            if (it.medium != "")
                                Glide.with(view.context).load(it.medium).into(logoImage)
                            else
                                Glide.with(view.context).load(item.stream.iconUrl)
                            imageLoadedForTrack = playingTrack.trackId
                        }
                }
                playingImage.visibility = if (item.isPlaying) View.VISIBLE else View.INVISIBLE
                view.setBackgroundResource(if (item.isPlaying) R.drawable.bg_lightest else R.drawable.bg_lesslight)
            }
        }

        override fun getItemCount(): Int =
            streams.size

        override fun onBindViewHolder(holder: StreamlistHolder, position: Int) =
            holder.bindStream(streams[position])

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamlistHolder =
            StreamlistHolder(LayoutInflater.from(parent.context).inflate(R.layout.streamlist_item, parent, false))

        fun updateStreams(newStreams: ArrayList<StreamlistItem>) {
            streams = newStreams
            notifyDataSetChanged()
        }
    }

    override fun makeDefaultState() =
        StreamlistState()

    override fun getLayout() =
        R.layout.fragment_streamlist

    override fun makeViewController(initialState: BoothState) =
        StreamlistView(
            activity as MainActivity,
            mainView!!
        )

    override fun makePresenter() =
        StreamlistPresenter(
        activity as MainActivity,
        viewController as StreamlistView
    )

    override fun onUnhide() {
        if (context != null) Prefs(context!!).searchState = null
        super.onUnhide()
    }
}
