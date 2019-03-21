package com.dlfsystems.BoothClient.apis

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.dlfsystems.BoothClient.MainActivity
import com.dlfsystems.BoothClient.nav.Rudder
import com.dlfsystems.BoothClient.apimodel.*
import com.dlfsystems.BoothClient.plusAssign
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object API {

    val URL_API: String = "OBSCURED"
    val URL_LASTFMAPI: String = "http://ws.audioscrobbler.com"
    val KEY_LASTFMAPI: String = "OBSCURED"
    val URL_DEEZERAPI: String = "http://api.deezer.com/"

    val api: BoothAPI
    val lastfmApi: LastfmAPI
    val deezerApi: DeezerAPI

    val streamInfo: BehaviorSubject<ModelStream>
    var streamInfoDisposable: Disposable? = null
    val streamInfoRefreshMillis: Long = 2000
    var token: String = ""

    private val disposables = CompositeDisposable()

    init {
        val okhttpClient: OkHttpClient.Builder = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))

        api = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(URL_API)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .client(okhttpClient.build()).build()
            .create(BoothAPI::class.java)

        streamInfo = BehaviorSubject.create<ModelStream>()
        streamInfoDisposable = Observable.interval(streamInfoRefreshMillis, TimeUnit.MILLISECONDS)
            .subscribe {
                refreshStreamInfo()
            }

        lastfmApi = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(URL_LASTFMAPI)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .client(okhttpClient.build()).build()
            .create(LastfmAPI::class.java)

        deezerApi = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(URL_DEEZERAPI)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .client(okhttpClient.build()).build()
            .create(DeezerAPI::class.java)
    }

    fun getStreamInfo(mountPoint: String): Observable<ModelStream> {
        return api.streams(token, mountPoint)
    }

    fun getStreamList(): Observable<ArrayList<ModelStream>> {
        return api.streamlist(token)
    }

    fun createStream(model: ModelStreamConfig): Completable {
        Answers.getInstance().logCustom(
            CustomEvent("Create Stream").putCustomAttribute("mountPoint", model.mountPoint)
        )
        return api.streamCreate(token, model)
    }

    fun configStream(model: ModelStreamConfig): Completable {
        return api.streamConfig(token, model.mountPoint, model)
    }

    fun deleteStream(mountPoint: String): Completable {
        Answers.getInstance().logCustom(
            CustomEvent("Delete Stream").putCustomAttribute("mountPoint", mountPoint)
        )
        return api.streamDelete(token, mountPoint)
    }

    fun getTrackInfo(trackId: Int): Observable<ModelTrack> {
        return api.track(token, trackId.toString())
    }

    fun searchTracks(
        title: String = "",
        artist: String = "",
        album: String = "",
        tags: ArrayList<String> = ArrayList(0),
        matchAnyTags: Boolean = false,
        matchAnyTextField: Boolean = true,
        offset: Int = 0,
        limit: Int = 0
    ): Observable<ModelSearchResult> {
        return api.searchTracks(token,
            ModelRequestSearch(title, artist, album, tags, matchAnyTags, matchAnyTextField, offset, limit))
    }

    fun getAlbumTracks(
        albumTitle: String = "",
        artist: String = ""
    ): Observable<ModelSearchResult> {
        return api.searchTracks(token,
            ModelRequestSearch("", artist, albumTitle, ArrayList<String>(0), false, false, 0, 50))
    }

    fun getArtistTracks(
        artistName: String
    ): Observable<ModelSearchResult> {
        return api.searchTracks(token,
            ModelRequestSearch("", artistName, "", ArrayList<String>(0), false, false, 0, 100))
    }

    fun queueTrack(mountPoint: String, trackId: Int): Completable {
        Answers.getInstance().logCustom(
            CustomEvent("Queue Track")
                .putCustomAttribute("trackId", trackId.toString())
                .putCustomAttribute("mountPoint", mountPoint)
        )
        return api.queueTrack(token, mountPoint, trackId)
    }

    fun queueOnCurrentStream(trackId: Int): Completable {
        if (Rudder.lastPlayingMountPoint == "") return Completable.complete()
        return queueTrack(Rudder.lastPlayingMountPoint, trackId)
    }

    fun unqueueTrack(mountPoint: String, trackId: Int): Completable {
        return api.unqueueTrack(token, mountPoint, trackId)
    }

    fun unqueueOnCurrentStream(trackId: Int): Completable {
        if (Rudder.lastPlayingMountPoint == "") return Completable.complete()
        return unqueueTrack(Rudder.lastPlayingMountPoint, trackId)
    }


    fun refreshStreamInfo() {
        if (Rudder.lastPlayingMountPoint != "") {
            val disposable = api.streams(token, Rudder.lastPlayingMountPoint).subscribe({
                streamInfo.onNext(it)
            }, {
                // TODO handle failure
            })
        }
    }

    fun rateTrack(trackId: Int, vote: Int): Completable {
        when {
            vote > 0 -> { return api.upvoteTrack(token, trackId) }
            vote < 0 -> { return api.downvoteTrack(token, trackId) }
            else -> { return api.unvoteTrack(token, trackId) }
        }
    }

    fun getLastfmAlbum(artist: String, title: String): Single<LastfmAlbum> {
        return lastfmApi.getAlbum(KEY_LASTFMAPI, artist, title)
    }

    fun getDeezer(searchString: String): Observable<DeezerResult> {
        return deezerApi.search(searchString)
    }

    fun getDeezerOne(searchString: String): Observable<DeezerResult> {
        return deezerApi.searchOne(searchString)
    }

    fun getArtForTrack(title: String, artist: String, albumTitle: String): Observable<AlbumInfo> {
        val observable = PublishSubject.create<AlbumInfo>()

        var queryString = if (albumTitle != "") "$albumTitle $artist" else "$title $artist"

        val leftParenPos = queryString.indexOf('(')
        var rightParenPos = queryString.indexOf(')')
        if (leftParenPos > -1 && rightParenPos > -1 && leftParenPos < rightParenPos)
            queryString = queryString.removeRange(leftParenPos, rightParenPos)

        disposables += ApiCall(API.getDeezer(queryString)) {
            if (it.tracks.data.size > 0) {
                val result = AlbumInfo(small = it.tracks.data[0].album.cover_small,
                                        medium = it.tracks.data[0].album.cover_medium,
                                        big = it.tracks.data[0].album.cover_big,
                                        xl = it.tracks.data[0].album.cover_xl)
                observable.onNext(result)
            } else {
                observable.onNext(AlbumInfo())
            }
        }
        return observable
    }

    fun getArtForArtist(name: String): Observable<String> {
        val observable = PublishSubject.create<String>()

        disposables += ApiCall(API.getDeezerOne(name)) {
            if (it.artists.data.size > 0) {
                observable.onNext(it.artists.data[0].picture_big)
            }
        }
        return observable
    }

}