package com.dlfsystems.BoothClient.apimodel

import java.io.Serializable

data class ModelSearchResult(
    val matchCount: Int,
    val results: ArrayList<ModelTrack>
) : Serializable