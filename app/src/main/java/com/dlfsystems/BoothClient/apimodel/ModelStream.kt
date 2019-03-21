package com.dlfsystems.BoothClient.apimodel

import java.io.Serializable

data class ModelStream(
    val _id: String = "",
    val mountPoint: String = "",
    val description: String = "",
    val iconUrl: String = "http://neptune.tspigot.net/boothclient/logo1.jpg",
    val owner: ModelUser = ModelUser(),
    val playing: ModelStreamtrack? = null,
    val queue: ArrayList<ModelStreamtrack> = ArrayList(0),
    val profile: ModelDJProfile = ModelDJProfile()
) : Serializable