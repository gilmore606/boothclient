package com.dlfsystems.BoothClient.apimodel

import java.io.Serializable

data class ModelRequestSearch(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val tags: ArrayList<String> = ArrayList(0),
    val matchAnyTags: Boolean = false,
    val matchAnyTextField: Boolean = true,
    val offset: Int = 0,
    val limit: Int = 0
) : Serializable
