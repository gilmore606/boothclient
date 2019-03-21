package com.dlfsystems.BoothClient.apis

import android.util.Log
import com.dlfsystems.BoothClient.nav.Rudder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException

class ApiCall<T> (
    apiTarget: Observable<T>,
    subscription: (result: T) -> Unit
) : Disposable {

    val disposable: Disposable
    var disposed = false

    init {
        disposable = apiTarget.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(subscription, { handleError(it) })
    }

    fun handleError(error: Throwable) {
        if (error is HttpException) {
            Log.d("boothclient", "HTTP ERROR " + error.code())
            if (error.code() == 403) {
                Rudder.navTo(Rudder.NavTarget.Login())
            }
        } else {
            Log.d("boothclient", "HTTP NETWORK ERROR " + error.toString())
        }
    }

    override fun isDisposed() = disposable.isDisposed

    override fun dispose() = disposable.dispose()
}
