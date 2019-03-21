package com.dlfsystems.BoothClient.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import com.dlfsystems.BoothClient.R
import com.dlfsystems.BoothClient.apimodel.ModelTrack
import io.reactivex.subjects.PublishSubject

class TrackitemView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : SwipeRevealLayout(context, attrs, defStyle) {

    var track = ModelTrack()
    var isQueued = false
    var isPlaying = false

    val clickEvent = PublishSubject.create<ModelTrack>()
    val queueEvent = PublishSubject.create<ModelTrack>()
    val openEvent = PublishSubject.create<TrackitemView>()
    val unqueueEvent = PublishSubject.create<ModelTrack>()
    val skipEvent = PublishSubject.create<ModelTrack>()

    val trackTitle: TextView
    val trackArtist: TextView
    val trackTopView: FrameLayout
    val buttonQueue: ImageButton
    val buttonUnqueue: ImageButton
    val buttonSkip: ImageButton
    val statusText: TextView

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.tracklistview_item, this, true)
        onFinishInflate()

        trackTitle = findViewById(R.id.searchlist_item_title)
        trackArtist = findViewById(R.id.searchlist_item_artist)
        trackTopView = findViewById(R.id.swipeable_tracklist_toplayer)
        buttonQueue = findViewById(R.id.tracklistview_queue)
        buttonUnqueue = findViewById(R.id.tracklistview_unqueue)
        buttonSkip = findViewById(R.id.tracklistview_skip)
        statusText = findViewById(R.id.tracklistview_item_status)
    }

    fun bindTrack(newtrack: ModelTrack) {
        track = newtrack
        trackTopView.setOnClickListener { clickEvent.onNext(track ?: ModelTrack()) }
        buttonQueue.setOnClickListener {
            close(true)
            queueEvent.onNext(track ?: ModelTrack())
        }
        buttonUnqueue.setOnClickListener {
            close(true)
            unqueueEvent.onNext(track ?: ModelTrack())
        }
        buttonSkip.setOnClickListener {
            close(true)
            skipEvent.onNext(track ?: ModelTrack())
        }
        trackTitle.text = track?.title
        trackArtist.text = track?.artist
    }

    override fun open(animation: Boolean) {
        super.open(animation)
        openEvent.onNext(this)
    }

    fun setIsPlaying(highlight: Boolean = true) {
        if (!isPlaying) {

            isPlaying = true
            isQueued = false
            if (highlight) {
                statusText.text = "now playing"
                trackTopView.background = context.resources.getDrawable(R.drawable.bg_lightest, null)
            }
            buttonQueue.visibility = View.GONE
            buttonSkip.visibility = View.VISIBLE
            buttonUnqueue.visibility = View.GONE
        }
    }

    fun setInQueue(highlight: Boolean = true) {
        if (!isQueued) {
            isPlaying = false
            isQueued = true
            if (highlight) {
                statusText.text = "in queue"
                trackTopView.background = context.resources.getDrawable(R.drawable.bg_lightest, null)
            }
            buttonQueue.visibility = View.GONE
            buttonSkip.visibility = View.GONE
            buttonUnqueue.visibility = View.VISIBLE
        }
    }

    fun setNormal() {
        if (isPlaying || isQueued) {
            statusText.text = ""
            isPlaying = false
            isQueued = false
            trackTopView.background = context.resources.getDrawable(R.drawable.bg_lesslight, null)
            buttonQueue.visibility = View.VISIBLE
            buttonSkip.visibility = View.GONE
            buttonUnqueue.visibility = View.GONE
        }
    }
}

