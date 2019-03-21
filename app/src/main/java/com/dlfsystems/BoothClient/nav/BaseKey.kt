package com.dlfsystems.BoothClient.nav

import android.os.Bundle
import android.os.Parcelable

// make a Key that uniquely identifies a Fragment (including its arguments/what it's showing)

abstract class BaseKey : Parcelable {
    val fragmentTag: String
    get() = toString()

    fun newFragment(): ContentFragment = createFragment().apply {
        arguments = (arguments ?: Bundle()).also { bundle ->
            bundle.putParcelable("KEY", this@BaseKey)
        }
    }

    protected abstract fun createFragment(): ContentFragment

    open fun getAnimation(): FragAnimPair =
        FragAnimPair(0, 0)

    open fun getBackAnimation(): FragAnimPair =
        FragAnimPair(0, 0)
}