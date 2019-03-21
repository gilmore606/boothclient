package com.dlfsystems.BoothClient.apimodel

import java.io.Serializable

data class LastfmAlbum(
    val name: String = "",
    val artist: String = "",
    val id: Int = 0,
    val images: ArrayList<LastfmImage>
) : Serializable