package com.dlfsystems.BoothClient.fragments

import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.paging.PositionalDataSource
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.dlfsystems.BoothClient.*
import com.dlfsystems.BoothClient.apimodel.ModelStream
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
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.parcel.Parcelize
import java.util.concurrent.TimeUnit

class SearchFragment
    : ContentFragment() {

    @Parcelize
    data class SearchKey(
        val tags: ArrayList<String>,
        val mountPoint: String
    )
        : BaseKey() {
        override fun createFragment() = SearchFragment().apply {
            arguments = (arguments ?: Bundle()).also {
                it.putSerializable("tags", tags.joinToString(separator = " "))
                it.putSerializable("mountPoint", mountPoint)
            }
        }

        override fun getAnimation() =
                FragAnimPair(R.anim.grow_fade_in_from_bottom, R.anim.stationary)

        override fun getBackAnimation() =
                FragAnimPair(R.anim.stationary, R.anim.shrink_fade_out_from_bottom)
    }

    data class SearchState(
        val text: String = "",
        val textTarget: Int = 0,
        val tags: ArrayList<String> = ArrayList(0),
        val matchAnyTags: Boolean = false,
        val mountPoint: String = ""
    )
             : BoothState()

    class SearchView(
        activity: MainActivity,
        initialState: SearchState,
        mainView: View
    )
        : ContentViewController(activity) {

        var streamState = ModelStream()
        val context: Context = mainView.context
        val searchInput: EditText = mainView.findViewById(R.id.search_input)
        val searchSpinner: Spinner = mainView.findViewById(R.id.search_spinner)
        val searchSpinnerSubject = PublishSubject.create<Int>()
        val searchSpinnerEvent: Observable<Int> = searchSpinnerSubject
        val searchTagbag: Tagbag = mainView.findViewById(R.id.search_tagbag)
        val searchReset: ImageButton = mainView.findViewById(R.id.search_reset)
        val recyclerView: androidx.recyclerview.widget.RecyclerView = mainView.findViewById(R.id.search_recycler)

        var dataSource = SearchDataSource(initialState.text, initialState.textTarget, initialState.tags, initialState.matchAnyTags)
        val recyclerAdapter = SearchRecyclerAdapter(this)
        init {
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            recyclerView.adapter = recyclerAdapter

            searchTagbag.replaceTags(initialState.tags)
            searchInput.setText(initialState.text)
            searchSpinner.setSelection(initialState.textTarget)

            ArrayAdapter.createFromResource(context,
                R.array.search_spin_array,
                R.layout.spinner_item).also { adapter ->
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                searchSpinner.adapter = adapter
                searchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                        searchSpinnerSubject.onNext(pos)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }
            }
        }

        override fun renderTitle(state: BoothState) = updateTitle(getString(R.string.title_search))

        override fun render(state: BoothState) {
            state as SearchState
            Log.d("boothclient", "FNORD render search with mountpoint=" + state.mountPoint)
            searchTagbag.replaceTags(state.tags)
            if (searchInput.text.toString() != state.text) searchInput.setText(state.text)
            searchSpinner.setSelection(state.textTarget)

            recyclerAdapter.submitList(makePagedList(state))
        }

        override fun onDestroy() { dataSource.onDestroy() }

        private fun makePagedList(state: SearchState): PagedList<ModelTrack> {
            dataSource.onDestroy()
            dataSource = SearchDataSource(state.text, state.textTarget, state.tags, state.matchAnyTags)
            val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setInitialLoadSizeHint(20)
                .setPageSize(20)
                .build()
            return PagedList.Builder(dataSource, pagedListConfig)
                .setFetchExecutor(MainThreadExecutor())
                .setNotifyExecutor(MainThreadExecutor())
                .build()
        }

        fun updateStreamState(stream: ModelStream) {
            streamState = stream
            val queuedIds = ArrayList(stream.queue.map { it.track.trackId })
            val nowPlayingId = stream.playing?.track?.trackId ?: -999
            for (i in 0..recyclerView.childCount-1) {
                val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as SearchRecyclerAdapter.SearchHolder
                val shouldBeQueued: Boolean = holder.track?.trackId in queuedIds
                val shouldBePlaying: Boolean = holder.track?.trackId == nowPlayingId
                val view = holder.view as TrackitemView
                if ((view.isQueued && !shouldBeQueued) || (view.isPlaying && !shouldBePlaying) ||
                    (!view.isQueued && shouldBeQueued) || (!view.isPlaying && shouldBePlaying))
                   recyclerAdapter.notifyItemChanged(holder.adapterPosition)
            }
        }
    }

    inner class SearchPresenter(
        activity: MainActivity,
        viewController: SearchView
    )
        : ContentPresenter(activity, viewController) {

        override fun makeSubscriptions() {
            viewController as SearchView

            disposables += viewController.recyclerAdapter.clickEvent.subscribe {
                    Rudder.navTo(Rudder.NavTarget.Track(it))
                }

            disposables += viewController.recyclerAdapter.queueEvent.subscribe {
                    queueTrack(it)
                }

            disposables += viewController.recyclerAdapter.unqueueEvent.subscribe {
                    unqueueTrack(it)
                }

            disposables += viewController.recyclerAdapter.skipEvent.subscribe {
                    skipTrack(it)
                }

            disposables += viewController.searchInput.textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribe {
                    changeState(
                        (lastState as SearchState).copy(
                            text = it.toString()
                        )
                    )
                }

            disposables += viewController.searchTagbag.tagStrings.onMainThread()
                .subscribe {
                    changeState(
                        (lastState as SearchState).copy(
                            tags = it
                        )
                    )
                }

            disposables += viewController.searchSpinnerEvent.onMainThread()
                .subscribe {
                    changeState(
                        (lastState as SearchState).copy(
                            textTarget = it
                        )
                    )
                }

            disposables += viewController.searchReset.clicks().onMainThread()
                .subscribe {
                    if (arguments != null) changeState(makeStateFromArguments(arguments!!))
                    else changeState(makeDefaultState())
                }

            disposables += API.streamInfo.onMainThread()
                .subscribe {
                    viewController.updateStreamState(it)
                }
        }

        private fun queueTrack(track: ModelTrack) {
            val state = lastState as SearchState
            disposables += API.queueTrack(
                state.mountPoint, track.trackId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    activity.makeToast(getString(R.string.added_track_to_queue, track.title))
                    API.refreshStreamInfo()
                }, {
                }
            )
        }

        private fun unqueueTrack(track: ModelTrack) {
            val state = lastState as SearchState
            disposables += API.unqueueTrack(
                state.mountPoint, track.trackId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    activity.makeToast(getString(R.string.removed_from_queue, track.title))
                    API.refreshStreamInfo()
                }
        }

        private fun skipTrack(track: ModelTrack) {

        }

        override fun changeState(newState: BoothState) {
            Prefs(activity as Context).searchState = newState as SearchState
            super.changeState(newState)
        }
    }

    class SearchDataSource(
        var text: String,
        var textTarget: Int,
        var tags: ArrayList<String> = ArrayList(0),
        var matchAnyTags: Boolean = false
    )
        : PositionalDataSource<ModelTrack>() {

        companion object {
            val SEARCH_TITLE = 0
            val SEARCH_ARTIST = 1
            val SEARCH_ALBUM = 2
            val SEARCH_ANY = 3
        }

        val disposables = CompositeDisposable()

        override fun loadInitial(
            params: LoadInitialParams,
            callback: LoadInitialCallback<ModelTrack>
        ) {
            disposables += ApiCall(
                API.searchTracks(
                    title = searchTitleFromSelections(text, textTarget),
                    artist = searchArtistFromSelections(text, textTarget),
                    album = searchAlbumFromSelections(text, textTarget),
                    tags = tags,
                    offset = params.requestedStartPosition,
                    limit = params.requestedLoadSize,
                    matchAnyTags = matchAnyTags,
                    matchAnyTextField = (textTarget == SEARCH_ANY)
                )
            ) {
                callback.onResult(it.results, params.requestedStartPosition, it.matchCount)
            }
        }

        override fun loadRange(
            params: LoadRangeParams,
            callback: LoadRangeCallback<ModelTrack>
        ) {
            disposables += ApiCall(
                API.searchTracks(
                    title = searchTitleFromSelections(text, textTarget),
                    artist = searchArtistFromSelections(text, textTarget),
                    album = searchAlbumFromSelections(text, textTarget),
                    tags = tags,
                    offset = params.startPosition,
                    limit = params.loadSize,
                    matchAnyTags = matchAnyTags,
                    matchAnyTextField = (textTarget == SEARCH_ANY)
                )
            ) {
                callback.onResult(it.results)
            }
        }

        private fun searchTitleFromSelections(text: String, textTarget: Int): String =
                if (textTarget == SEARCH_TITLE || textTarget == SEARCH_ANY) text
                else ""

        private fun searchArtistFromSelections(text: String, textTarget: Int): String =
                if (textTarget == SEARCH_ARTIST || textTarget == SEARCH_ANY) text
                else ""

        private fun searchAlbumFromSelections(text: String, textTarget: Int): String =
                if (textTarget == SEARCH_ALBUM || textTarget == SEARCH_ANY) text
                else ""

        fun onDestroy() {
            invalidate()
            disposables.dispose()
        }
    }

    class SearchRecyclerAdapter(val viewController: SearchView)
        : PagedListAdapter<ModelTrack, SearchRecyclerAdapter.SearchHolder>(ModelTrack.diffCallback) {

        val clickSubject = PublishSubject.create<ModelTrack>()
        val clickEvent: Observable<ModelTrack> = clickSubject
        val queueSubject = PublishSubject.create<ModelTrack>()
        val queueEvent: Observable<ModelTrack> = queueSubject
        val unqueueSubject = PublishSubject.create<ModelTrack>()
        val unqueueEvent: Observable<ModelTrack> = unqueueSubject
        val skipSubject = PublishSubject.create<ModelTrack>()
        val skipEvent: Observable<ModelTrack> = skipSubject

        class SearchHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            var track: ModelTrack? = null
            var disposables = CompositeDisposable()

            fun bindTrack(item: ModelTrack?, streamState: ModelStream,
                          adapter: SearchRecyclerAdapter) {
                view as TrackitemView
                this.track = item ?: ModelTrack()
                view.tag = item?.trackId ?: "empty"
                if (view.mIsOpenBeforeInit) view.close(false)
                view.bindTrack(item ?: ModelTrack())
                if (item != null) {
                    if (item.trackId in streamState.queue.map { it.track.trackId }) {
                        view.setInQueue()
                    } else if (item.trackId == streamState.playing?.track?.trackId) {
                        view.setIsPlaying()
                    } else {
                        view.setNormal()
                    }
                }
                disposables.dispose()
                disposables = CompositeDisposable()
                disposables += view.clickEvent.subscribe {
                    adapter.clickSubject.onNext(it)
                }
                disposables += view.queueEvent.subscribe {
                    adapter.queueSubject.onNext(it)
                }
                disposables += view.unqueueEvent.subscribe {
                    adapter.unqueueSubject.onNext(it)
                }
                disposables += view.skipEvent.subscribe {
                    adapter.skipSubject.onNext(it)
                }
                disposables += view.openEvent.subscribe {
                    adapter.closeAllSwipesBut(it)
                }
            }
        }

        override fun onBindViewHolder(holder: SearchHolder, position: Int) {
            holder.bindTrack(getItem(position), viewController.streamState, this)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHolder {
            val view = TrackitemView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
            return SearchHolder(view)
        }

        fun closeAllSwipesBut(openview: TrackitemView) {
            (viewController.recyclerView.views.filter { it is TrackitemView } as List<TrackitemView>)
            .filter { it.mIsOpenBeforeInit && it != openview }
                .forEach { it.close(true) }
        }
    }

    override fun makeInitialState(bundle: Bundle?, arguments: Bundle?): BoothState {
        context?.let {
            Prefs(it).searchState?.let {
                prefState -> return prefState.copy(mountPoint = arguments?.getSerializable("mountPoint") as String)
            }
        }
        return super.makeInitialState(bundle, arguments)
    }

    override fun makeStateFromArguments(arguments: Bundle): BoothState =
        SearchState(
            tags = ArrayList((arguments.getSerializable("tags") as String).split(" ")),
            mountPoint = arguments.getSerializable("mountPoint") as String
        )

    override fun makeDefaultState() =
        SearchState()

    override fun getLayout() =
        R.layout.fragment_search

    override fun makeViewController(initialState: BoothState) = SearchView(
                activity as MainActivity,
                initialState as SearchState,
                mainView!!)

    override fun makePresenter() = SearchPresenter(
        activity as MainActivity,
        viewController as SearchView)
}