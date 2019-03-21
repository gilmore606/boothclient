package com.dlfsystems.BoothClient.apimodel

import java.io.Serializable

data class ModelUser(
    val _id: String = "",
    val userId: Int = 0,
    val email: String = ""
) : Serializable