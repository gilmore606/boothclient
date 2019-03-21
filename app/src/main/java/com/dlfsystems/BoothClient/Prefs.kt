package com.dlfsystems.BoothClient

import android.content.Context
import android.preference.PreferenceManager
import com.dlfsystems.BoothClient.fragments.SearchFragment
import com.google.gson.Gson

class Prefs(context: Context) {

    companion object {
        val ACCESS_TOKEN = "com.dlfsystems.BoothClient.ACCESS_TOKEN"
        val USER_ID = "com.dlfsystems.BoothClient.USER_ID"
        val SEARCH_STATE = "com.dlfsystems.BoothClient.SEARCH_STATE"
    }

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var accessToken
        get() = preferences.getString(ACCESS_TOKEN, "")
        set(value) = preferences.edit().putString(ACCESS_TOKEN, value).apply()

    var userId
        get() = preferences.getString(USER_ID, "")
        set(value) = preferences.edit().putString(USER_ID, value).apply()

    var searchState: SearchFragment.SearchState?
        get() {
            val json = preferences.getString(SEARCH_STATE, "")
            if (json != "") {
                return Gson().fromJson(json, SearchFragment.SearchState::class.java)
            }
            return null
        }
        set(value) {
            if (value == null) preferences.edit().putString(SEARCH_STATE, "").apply()
            else preferences.edit().putString(SEARCH_STATE, Gson().toJson(value)).apply()
        }
}