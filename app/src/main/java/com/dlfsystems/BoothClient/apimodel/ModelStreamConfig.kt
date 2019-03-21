package com.dlfsystems.BoothClient.apimodel

import java.io.Serializable

data class ModelStreamConfig(
    val mountPoint: String = "",
    val description: String = "",
    val profile: ModelDJProfile = ModelDJProfile()
) : Serializable
