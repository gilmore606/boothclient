package com.dlfsystems.BoothClient.apimodel

import java.io.Serializable

data class ModelDJProfile(
    val spinning: ArrayList<String> = ArrayList(0)
) : Serializable