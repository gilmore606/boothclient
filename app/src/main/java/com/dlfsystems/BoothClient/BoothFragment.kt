package com.dlfsystems.BoothClient

import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.io.Serializable
import java.util.concurrent.Executor

abstract class BoothFragment : androidx.fragment.app.Fragment() {

    enum class SwipeDir { UP, DOWN, LEFT, RIGHT }

    abstract class BoothState : Serializable

    class MainThreadExecutor : Executor {
        val handler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) { handler.post(command) }
    }

    // TODO: inject activity
    abstract class BoothViewController(val activity: MainActivity) {
        open fun render(state: BoothState) { }
        open fun renderActions(state: BoothState, menu: Menu) { }
        open fun onDestroy() { }
        open fun onRestoreView(state: BoothState) { }
        fun getString(id: Int, vararg argsToPass: Any) = activity.getString(id, *argsToPass)
    }

    // TODO: inject activity
    abstract class BoothPresenter(
        val activity: MainActivity,
        val viewController: BoothViewController
    ) {

        lateinit var lastState: BoothState
        var disposables = CompositeDisposable()

        open fun makeSubscriptions() { }

        open fun onCreate(initialState: BoothState) {
            lastState = initialState
            activity.runOnUiThread {
                viewController.render(lastState)
            }
            makeSubscriptions()
        }

        fun onPause() {
            disposables.dispose()
        }

        fun onResume() {
            if (!disposables.isDisposed) disposables.dispose()
            disposables = CompositeDisposable()
            makeSubscriptions()
        }

        fun onDestroy() {
            if (!disposables.isDisposed) disposables.dispose()
        }

        open fun changeState(newState: BoothState) {
            if (newState != lastState) {
                lastState = newState
                activity.runOnUiThread {
                    viewController.render(newState)
                }
            }
        }

        fun onSaveInstanceState(bundle: Bundle) {
            Log.d("boothclient", "STATE saving instance state " + lastState.toString())
            bundle.putSerializable("state", lastState)
        }

        fun getString(id: Int, vararg argsToPass: Any) = activity.getString(id, *argsToPass)
    }


    var viewController: BoothViewController? = null
    var presenter: BoothPresenter? = null
    var mainView: View? = null

    abstract fun getLayout(): Int

    open fun makeInitialState(bundle: Bundle?, arguments: Bundle?): BoothState {
        Log.d("boothclient", "FNORD make initial state for " + toString())
        if (bundle != null && bundle.containsKey("state")) {
            Log.d("boothclient", "restoring fragment state (" + bundle.getSerializable("state")!!.toString() + " from bundle")
            return bundle.getSerializable("state") as BoothState
        }
        if (arguments != null) {
            Log.d("boothclient", "FNORD (from arguments) " + arguments.toString())
            return makeStateFromArguments(arguments)
        }
        Log.d("boothclient", "FNORD makeDefaultState()")
        return makeDefaultState()
    }

    open fun makeStateFromArguments(arguments: Bundle) = makeDefaultState()

    abstract fun makeDefaultState(): BoothState
    abstract fun makeViewController(initialState: BoothState): BoothViewController
    abstract fun makePresenter(): BoothPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View? {
        if (mainView != null && viewController != null && presenter != null) {
            viewController!!.onRestoreView(presenter!!.lastState)
            return mainView
        }

        mainView = inflater.inflate(getLayout(), container, false)

        val initialState = makeInitialState(bundle, arguments)
        viewController = makeViewController(initialState)
        presenter = makePresenter().apply {
            onCreate(initialState)
        }

        return mainView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) onHide()
        else onUnhide()
    }

    protected open fun onHide() {
        Log.d("boothclient", "FRAGSTATE onHide " + toString())
        presenter?.onPause()
    }

    protected open fun onUnhide() {
        Log.d("boothclient", "FRAGSTATE onUnhide " + toString())
        presenter?.onResume()
    }

    override fun onPause() {
        Log.d("boothclient", "FRAGSTATE onPause " + toString())
        presenter?.onPause()
        super.onPause()
    }

    override fun onResume() {
        Log.d("boothclient", "FRAGSTATE onResume " + toString())
        super.onResume()
        if (!isHidden) presenter?.onResume()
    }

    override fun onDestroy() {
        Log.d("boothclient", "FRAGSTATE onDestroy " + toString())
        presenter?.onDestroy()
        viewController?.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        presenter?.onSaveInstanceState(bundle)
        super.onSaveInstanceState(bundle)
    }
}