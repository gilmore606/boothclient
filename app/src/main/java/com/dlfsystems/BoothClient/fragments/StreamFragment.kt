package com.dlfsystems.BoothClient.fragments

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.dlfsystems.BoothClient.*
import com.dlfsystems.BoothClient.apimodel.ModelDJProfile
import com.dlfsystems.BoothClient.apimodel.ModelStream
import com.dlfsystems.BoothClient.apimodel.ModelStreamConfig
import com.dlfsystems.BoothClient.apimodel.ModelTrack
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.apis.ApiCall
import com.dlfsystems.BoothClient.nav.FragAnimPair
import com.dlfsystems.BoothClient.nav.BaseKey
import com.dlfsystems.BoothClient.nav.ContentFragment
import com.dlfsystems.BoothClient.nav.Rudder
import com.dlfsystems.BoothClient.views.Tagbag
import com.dlfsystems.BoothClient.views.TrackitemView
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.parcel.Parcelize
import java.util.concurrent.TimeUnit

class StreamFragment : ContentFragment() {

    companion object {
        val ACTION_DELETE = 1001
        val ACTION_CONFIG = 1002
        val ACTION_SEARCH = 1003
        val ACTION_EXPLORE = 1004
    }

    @Parcelize
    data class StreamKey(val mountPoint: String)
             : BaseKey() {
        override fun createFragment(): ContentFragment = StreamFragment().apply {
            arguments = (arguments ?: Bundle()).also { bundle ->
                bundle.putSerializable("mountpoint", mountPoint)
            }
        }

        override fun getAnimation(): FragAnimPair =
                FragAnimPair(R.anim.slide_in_left, R.anim.slide_out_left)

        override fun getBackAnimation(): FragAnimPair =
                FragAnimPair(R.anim.slide_in_right, R.anim.slide_out_right)
    }

    data class StreamState(
        val stream: ModelStream = ModelStream(),
        val playingMountPoint: String = "",
        val servicePlaying: Boolean = false,
        val servicePreparing: Boolean = false,
        val playingImageUrl: String = ""
    ) : BoothState()

    class StreamView(
        activity: MainActivity,
        val mainView: View
    )
        : ContentViewController(activity) {

        val creatorView: TextView = mainView.findViewById(R.id.stream_creator)
        val descView: TextView = mainView.findViewById(R.id.stream_desc)
        val spinningTagbag: Tagbag = mainView.findViewById(R.id.stream_tagbag)
        val logoImage: ImageView = mainView.findViewById(R.id.stream_logo)

        var loadedImageUrl: String = ""

        val nowPlayingView: TrackitemView = mainView.findViewById(R.id.stream_nowplaying)

        val recyclerView: androidx.recyclerview.widget.RecyclerView = mainView.findViewById(R.id.streamqueue_recycler)
        val recyclerCache = ArrayList<ModelTrack>(0)
        val recyclerAdapter = StreamqueueRecyclerAdapter(this, recyclerCache)

        init {
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(mainView.context)
            recyclerView.adapter = recyclerAdapter
        }

        override fun renderTitle(state: BoothState) =
            updateTitle((state as StreamState).stream.mountPoint)

        override fun renderActions(state: BoothState, menu: Menu) {
            state as StreamState

            menu.add(
                Menu.NONE,
                ACTION_SEARCH,
                1,
                getString(R.string.menu_search)
                ).setIcon(R.drawable.search_add_icon)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)

            menu.add(
                Menu.NONE,
                ACTION_EXPLORE,
                2,
                getString(R.string.menu_explore)
                ).setIcon(R.drawable.icon_dig)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)

            if (Prefs(mainView.context).userId == state.stream.owner.email) {
                menu.add(
                    Menu.NONE,
                    ACTION_DELETE,
                    3,
                    getString(R.string.menu_delete_stream)
                ).setIcon(R.drawable.delete_forever_icon)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)

                menu.add(
                    Menu.NONE,
                    ACTION_CONFIG,
                    4,
                    getString(R.string.menu_stream_settings)
                ).setIcon(R.drawable.settings_icon)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
            }

        }

        override fun render(state: BoothState) {
            state as StreamState

            creatorView.text = getString(R.string.stream_creator, state.stream.owner.email)
            descView.text = state.stream.description

            spinningTagbag.replaceTags(state.stream.profile.spinning)
            spinningTagbag.editable = (Prefs(mainView.context).userId == state.stream.owner.email)

            nowPlayingView.bindTrack(state.stream.playing?.track ?: ModelTrack())
            nowPlayingView.setIsPlaying(highlight = false)

            if (loadedImageUrl != state.playingImageUrl) {
                if (state.playingImageUrl == "")
                    Glide.with(mainView.context).load(state.stream.iconUrl).into(logoImage)
                else
                    Glide.with(mainView.context).load(state.playingImageUrl).into(logoImage)
                loadedImageUrl = state.playingImageUrl
            }

            recyclerAdapter.updateQueue(
                ArrayList(state.stream.queue.map { it.track })
            )
        }

        fun refreshQueueDisplay() {
            recyclerAdapter.notifyDataSetChanged()
        }
    }

    class StreamPresenter(
        activity: MainActivity,
        viewController: StreamView
    )
        : ContentPresenter(activity, viewController) {

        override fun makeSubscriptions() {
            viewController as StreamView

            disposables += Rudder.playState.onMainThread()
                .subscribe {
                    val state = lastState as StreamState
                    changeState(
                        state.copy(
                            playingMountPoint = it.mountPoint,
                            servicePlaying = it.playing,
                            servicePreparing = it.preparing
                        ))
                }

            disposables += viewController.nowPlayingView.clicks().onMainThread()
                .subscribe {
                    val playingTrack = (lastState as StreamState).stream.playing?.track
                    if (playingTrack != null) {
                        Rudder.navTo(
                            Rudder.NavTarget.Track(
                                playingTrack
                            ))
                    }
                }

            disposables += viewController.recyclerAdapter.clickEvent
                .subscribe {
                    Rudder.navTo(Rudder.NavTarget.Track(it))
                }

            disposables += viewController.recyclerAdapter.unqueueEvent
                .subscribe {
                    unqueueTrack(it)
                }

            disposables += viewController.nowPlayingView.clickEvent
                .subscribe {
                    Rudder.navTo(Rudder.NavTarget.Track(it))
                }

            disposables += viewController.nowPlayingView.skipEvent
                .subscribe {
                    skipTrack(it)
                }

            disposables += viewController.spinningTagbag.tagStrings
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if ((lastState as StreamState).stream.profile.spinning != it) {
                        updateSpinningOnServer(it)
                    }
                }

            disposables += Observable.interval(2000, TimeUnit.MILLISECONDS)
                .subscribe {
                    refreshFromApi()
                }
            refreshFromApi()
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                (ACTION_CONFIG) -> {
                    Rudder.navTo(
                        Rudder.NavTarget.StreamConfig((lastState as StreamState).stream.mountPoint,
                        (lastState as StreamState).stream.description))
                }
                (ACTION_DELETE) -> {
                    confirmAndDeleteStream()
                }
                (ACTION_SEARCH) -> {
                    val state = lastState as StreamState
                    Rudder.navTo(
                        Rudder.NavTarget.Search(state.stream.profile.spinning,
                            state.stream.mountPoint))
                }
                (ACTION_EXPLORE) -> {

                }
                else -> { return false }
            }
            return true
        }

        private fun refreshFromApi() {
            val state = lastState as StreamState
            disposables += ApiCall(API.getStreamInfo(state.stream.mountPoint)) {
                changeState(
                    (lastState as StreamState).copy(
                        stream = it
                    )
                )
                if (it.playing?.track?.trackId != state.stream.playing?.track?.trackId) {
                disposables += API.getArtForTrack(it.playing?.track?.title ?: "",
                    it.playing?.track?.artist ?: "",
                    it.playing?.track?.album ?: "")
                    .subscribe {
                        changeState(
                            (lastState as StreamState).copy(
                                playingImageUrl = it.medium
                            )
                        )
                    }
                }
            }
        }

        private fun updateSpinningOnServer(tags: ArrayList<String>) {
            val state = lastState as StreamState
            val model = ModelStreamConfig(
                mountPoint = state.stream.mountPoint,
                description = state.stream.description,
                profile = ModelDJProfile(spinning = tags)
            )
            disposables += API.configStream(model).observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    activity.makeToast(getString(R.string.spinning_updated))
                    refreshFromApi()
                }, {
                })
        }

        private fun confirmAndDeleteStream() {
            val state = lastState as StreamState
            AlertDialog.Builder(activity, R.style.DialogStyle)
                .setTitle(getString(R.string.title_delete_stream, state.stream.mountPoint))
                .setMessage(getString(R.string.are_you_sure_delete_stream, state.stream.mountPoint))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    disposables += API.deleteStream(state.stream.mountPoint).observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                        onDeleteStream(state.stream.mountPoint)
                    }, {
                        })
                }
                .setNegativeButton(getString(R.string.no)) { _, _ ->
                }
                .create().show()
        }

        private fun onDeleteStream(mountPoint: String) {
            if (mountPoint == Rudder.playingMountPoint) {
                Rudder.play(Rudder.PlayTarget.Stop())
                Rudder.lastPlayingMountPoint = ""
            }
            Rudder.navTo(Rudder.NavTarget.Streamlist())
            activity.makeToast(getString(R.string.deleted_stream, mountPoint))
        }

        private fun unqueueTrack(track: ModelTrack) {
            disposables += API.unqueueTrack(
                (lastState as StreamState).stream.mountPoint,
                track.trackId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    activity.makeToast(getString(R.string.removed_from_queue, track.title))
                    refreshFromApi()
                }, {
                })
        }

        private fun skipTrack(track: ModelTrack) {

        }
    }

    class StreamqueueRecyclerAdapter(val viewController: StreamView, val queue: ArrayList<ModelTrack>)
        : androidx.recyclerview.widget.RecyclerView.Adapter<StreamqueueRecyclerAdapter.StreamqueueHolder>() {

        val clickSubject = PublishSubject.create<ModelTrack>()
        val clickEvent: Observable<ModelTrack> = clickSubject
        val unqueueSubject = PublishSubject.create<ModelTrack>()
        val unqueueEvent: Observable<ModelTrack> = unqueueSubject

        inner class StreamqueueHolder(val view: View)
            : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            var track: ModelTrack? = null
            var disposables = CompositeDisposable()

            init {
                view.setOnClickListener {
                    clickSubject.onNext(queue[layoutPosition])
                }
            }
            fun bindTrack(item: ModelTrack, adapter: StreamqueueRecyclerAdapter) {
                view as TrackitemView
                this.track = item
                view.bindTrack(item)
                view.setInQueue(highlight = false)

                disposables.dispose()
                disposables = CompositeDisposable()
                disposables += view.clickEvent.subscribe {
                    adapter.clickSubject.onNext(it)
                }
                disposables += view.unqueueEvent.subscribe {
                    adapter.unqueueSubject.onNext(it)
                }
                disposables += view.openEvent.subscribe {
                    adapter.closeAllSwipesBut(it)
                }
            }
        }

        override fun getItemCount() = queue.size

        override fun onBindViewHolder(holder: StreamqueueHolder, position: Int) {
            holder.bindTrack(queue[position], this)
            holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, android.R.anim.slide_in_left)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamqueueHolder {
            val view = TrackitemView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
            return StreamqueueHolder(view)
        }

        fun closeAllSwipesBut(openview: TrackitemView) {
            (viewController.recyclerView.views.filter { it is TrackitemView } as List<TrackitemView>)
                .filter { it.mIsOpenBeforeInit && it != openview }
                .forEach { it.close(true) }
        }

        fun updateQueue(newQueue: ArrayList<ModelTrack>) {
            val oldQueue = queue.clone() as ArrayList<ModelTrack>
            queue.clear()
            newQueue.forEachIndexed { i, it ->
                queue.add(it)
                if (!(it in oldQueue)) notifyItemInserted(i)
            }
            oldQueue.forEachIndexed { i, it ->
                if (!(it in newQueue)) notifyItemRemoved(i)
            }
        }
    }

    override fun onUnhide() {
        (viewController as StreamView).refreshQueueDisplay()
        super.onUnhide()
    }

    override fun makeStateFromArguments(arguments: Bundle): BoothState =
        StreamState(
            ModelStream(mountPoint = (arguments.getSerializable("mountpoint") as String)),
            "", false, false
        )

    override fun makeDefaultState() =
        StreamState()

    override fun getLayout() =
        R.layout.fragment_stream

    override fun makeViewController(initialState: BoothState) =
        StreamView(
            activity as MainActivity,
            mainView!!
        )

    override fun makePresenter() = StreamPresenter(
        activity as MainActivity,
        viewController as StreamView
    )
}
