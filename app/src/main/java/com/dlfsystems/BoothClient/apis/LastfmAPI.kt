package com.dlfsystems.BoothClient.apis

import com.dlfsystems.BoothClient.apimodel.LastfmAlbum
import io.reactivex.Single
import retrofit2.http.*

interface LastfmAPI {

    @GET("/2.0/?method=album.getInfo&format=json")
    fun getAlbum(@Query("api_key") apiKey: String, @Query("artist") artist: String, @Query("title") title: String): Single<LastfmAlbum>
}