package com.dlfsystems.BoothClient.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.*
import com.dlfsystems.BoothClient.apimodel.ModelStream
import com.dlfsystems.BoothClient.apimodel.ModelTrack
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.nav.Rudder
import com.dlfsystems.BoothClient.plusAssign
import com.dlfsystems.BoothClient.views
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class TracklistView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    private var disposables = CompositeDisposable()

    val tracks = ArrayList<ModelTrack>(0)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL

        disposables += API.streamInfo.observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                updateTrackStatus(it)
            }
    }

    fun updateTrackStatus(stream: ModelStream) {
        trackViews().forEach {
            when {
                it.track?.trackId == stream.playing?.track?.trackId -> {
                    it.setIsPlaying()
                }
                it.track?.trackId in stream.queue.map { it.track.trackId } -> {
                    it.setInQueue()
                }
                else -> {
                    it.setNormal()
                }
            }
        }
    }

    fun populate(newtracks: ArrayList<ModelTrack>) {
        newtracks.filter { !(it in tracks) }
            .forEach {
                tracks.add(it)
                val view = TrackitemView(context)
                view.bindTrack(it)
                addView(view)
                disposables += view.clickEvent.observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        onTrackClick(it)
                    }
                disposables += view.queueEvent.observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        onTrackQueue(it)
                    }
                disposables += view.unqueueEvent.observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        onTrackUnqueue(it)
                    }
                disposables += view.skipEvent.observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        onTrackSkip(it)
                    }
                disposables += view.openEvent.observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        closeAllSwipesBut(it)
                    }
            }
    }

    fun trackViews(): List<TrackitemView> = views.filter { it is TrackitemView } as List<TrackitemView>

    fun closeAllSwipesBut(openview: TrackitemView) {
        trackViews().filter { it.mIsOpenBeforeInit && it != openview }
            .forEach { it.close(true) }
    }

    fun onTrackClick(track: ModelTrack) {
        Rudder.navTo(Rudder.NavTarget.Track(track))
    }

    fun onTrackQueue(track: ModelTrack) {
        disposables += API.queueOnCurrentStream(track.trackId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Toast.makeText(context, ("Added " + track.title + " to queue."), Toast.LENGTH_SHORT).show()
                API.refreshStreamInfo()
            }
    }

    fun onTrackUnqueue(track: ModelTrack) {
        disposables += API.unqueueOnCurrentStream(track.trackId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Toast.makeText(context, ("Removed " + track.title + " from queue."), Toast.LENGTH_SHORT).show()
                API.refreshStreamInfo()
            }
    }

    fun onTrackSkip(track: ModelTrack) {

    }
}