package com.dlfsystems.BoothClient.apimodel

import java.io.Serializable

data class DeezerResult(
    val tracks: DeezerTrackData,
    val artists: DeezerArtistData,
    val albums: DeezerAlbumData
) : Serializable

data class DeezerTrackData(
    val data: ArrayList<DeezerTrack>
) : Serializable

data class DeezerArtistData(
    val data: ArrayList<DeezerArtist>
) : Serializable

data class DeezerAlbumData(
    val data: ArrayList<DeezerAlbum>
): Serializable

data class DeezerTrack(
    val id: Int,
    val title: String,
    val artist: DeezerArtist,
    val album: DeezerAlbum
) : Serializable

data class DeezerArtist(
    val id: Int,
    val name: String,
    val picture_small: String,
    val picture_medium: String,
    val picture_big: String,
    val picture_xl: String
) : Serializable

data class DeezerAlbum(
    val id: Int,
    val title: String,
    val release_date: String,
    val label: String,
    val cover_small: String,
    val cover_medium: String,
    val cover_big: String,
    val cover_xl: String
) : Serializable