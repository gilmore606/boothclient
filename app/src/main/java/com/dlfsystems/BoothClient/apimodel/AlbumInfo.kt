package com.dlfsystems.BoothClient.apimodel

import java.io.Serializable

data class AlbumInfo(
    val small: String = "http://neptune.tspigot.net/boothclient/logo1.jpg",
    val medium: String = "http://neptune.tspigot.net/boothclient/logo1.jpg",
    val big: String = "http://neptune.tspigot.net/boothclient/logo1.jpg",
    val xl: String = "http://neptune.tspigot.net/boothclient/logo1.jpg",
    val artist: String = "?",
    val title: String = "?",
    val tracks: ArrayList<ModelTrack> = ArrayList(0)
) : Serializable