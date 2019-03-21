package com.dlfsystems.BoothClient.apimodel

import androidx.recyclerview.widget.DiffUtil
import java.io.Serializable

data class ModelTrack(
    val _id: String = "",
    val trackId: Int = 0,
    val artist: String = "",
    val title: String = "",
    val album: String = "",
    val length: Int = 0,
    val plays: Int = 0,
    val userScore: Int = 0,
    val tags: ArrayList<String> = ArrayList(0)
) : Serializable {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<ModelTrack>() {
            override fun areItemsTheSame(oldI: ModelTrack, newI: ModelTrack): Boolean =
                oldI.trackId == newI.trackId

            override fun areContentsTheSame(oldI: ModelTrack, newI: ModelTrack): Boolean =
                oldI.trackId == newI.trackId
        }
    }
}