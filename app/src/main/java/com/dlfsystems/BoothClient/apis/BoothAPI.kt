package com.dlfsystems.BoothClient.apis

import com.dlfsystems.BoothClient.apimodel.*
import io.reactivex.Completable
import io.reactivex.Observable
import retrofit2.http.*

interface BoothAPI {

    @GET("streams/{mountPoint}")
    fun streams(
        @Header("Authorization") authHeader: String,
        @Path("mountPoint") mountPoint: String
    ): Observable<ModelStream>

    @GET("streams/")
    fun streamlist(@Header("Authorization") authHeader: String): Observable<ArrayList<ModelStream>>

    @GET("tracks/{trackId}")
    fun track(
        @Header("Authorization") authHeader: String,
        @Path("trackId") trackId: String
    ): Observable<ModelTrack>

    @DELETE("streams/{mountPoint}")
    fun streamDelete(
        @Header("Authorization") authHeader: String,
        @Path("mountPoint") mountPoint: String
    ): Completable

    @POST("streams")
    fun streamCreate(
        @Header("Authorization") authHeader: String,
        @Body model: ModelStreamConfig
    ): Completable

    @PUT("streams/{mountPoint}")
    fun streamConfig(
        @Header("Authorization") authHeader: String,
        @Path("mountPoint") mountPoint: String,
        @Body model: ModelStreamConfig
    ): Completable

    @POST("/tracks/search")
    fun searchTracks(
        @Header("Authorization") authHeader: String,
        @Body model: ModelRequestSearch
    ): Observable<ModelSearchResult>

    @GET("/streams/{mountPoint}/add/{trackId}")
    fun queueTrack(
        @Header("Authorization") authHeader: String,
        @Path("mountPoint") mountPoint: String,
        @Path("trackId") trackId: Int
    ): Completable

    @GET("/streams/{mountPoint}/remove/{trackId}")
    fun unqueueTrack(
        @Header("Authorization") authHeader: String,
        @Path("mountPoint") mountPoint: String,
        @Path("trackId") trackId: Int
    ): Completable

    @GET("/tracks/{trackId}/yay")
    fun upvoteTrack(
        @Header("Authorization") authHeader: String,
        @Path("trackId") trackId: Int
    ): Completable

    @GET("/tracks/{trackId}/boo")
    fun downvoteTrack(
        @Header("Authorization") authHeader: String,
        @Path("trackId") trackId: Int
    ): Completable

    @GET("/tracks/{trackId}/meh")
    fun unvoteTrack(
        @Header("Authorization") authHeader: String,
        @Path("trackId") trackId: Int
    ): Completable
}