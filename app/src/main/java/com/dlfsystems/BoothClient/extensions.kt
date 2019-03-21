package com.dlfsystems.BoothClient

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.jakewharton.rxbinding3.InitialValueObservable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

operator fun CompositeDisposable.plusAssign(subscription: Disposable) {
    add(subscription)
}

fun InitialValueObservable<CharSequence>.liveSearchDebounce(): Observable<String> =
            map { it.toString().trim() }
            .distinct()
            .debounce(500, TimeUnit.MILLISECONDS)

fun <T> Observable<T>.asyncIO(): Observable<T> =
        subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())

fun <T> Observable<T>.onMainThread(): Observable<T> =
        observeOn(AndroidSchedulers.mainThread())

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

val View.isVisible: Boolean
    get() = (visibility == View.VISIBLE)

var View.visibleElseGone: Boolean
    get() = (visibility == View.VISIBLE)
    set(value) { visibility = if (value) View.VISIBLE else View.GONE }

var View.visibleElseInvisible: Boolean
    get() = (visibility == View.VISIBLE)
    set(value) { visibility = if (value) View.VISIBLE else View.INVISIBLE }

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

val ViewGroup.views: List<View>
get() = (0..getChildCount() - 1).map { getChildAt(it) }
