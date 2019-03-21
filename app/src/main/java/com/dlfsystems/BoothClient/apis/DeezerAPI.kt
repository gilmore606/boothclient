package com.dlfsystems.BoothClient.apis

import com.dlfsystems.BoothClient.apimodel.DeezerResult
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface DeezerAPI {

    @GET("/search/autocomplete")
    fun search(@Query("q") searchString: String): Observable<DeezerResult>

    @GET("/search/autocomplete?limit=1")
    fun searchOne(@Query("q") searchString: String): Observable<DeezerResult>
}