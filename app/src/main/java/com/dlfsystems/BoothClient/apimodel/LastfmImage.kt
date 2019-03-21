package com.dlfsystems.BoothClient.apimodel

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LastfmImage(
    @SerializedName("#text")
    val url: String = "",
    val size: String = ""
) : Serializable