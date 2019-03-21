package com.dlfsystems.BoothClient.nav

import android.view.Menu
import android.view.MenuItem
import com.dlfsystems.BoothClient.BoothFragment
import com.dlfsystems.BoothClient.MainActivity

abstract class ContentFragment : BoothFragment() {

    val requireArguments
        get() = this.arguments ?: throw IllegalStateException("Fragment arguments should exist!")

    fun <T : BaseKey> getKey(): T? = requireArguments.getParcelable<T>("KEY")

    fun renderActions(menu: Menu) {
        viewController?.renderActions(presenter!!.lastState, menu)
    }

    abstract class ContentViewController(activity: MainActivity)
        : BoothViewController(activity) {

        open fun renderTitle(state: BoothState) { }

        protected fun updateTitle(title: String, subtitle: String = "", enableBackButton: Boolean = true) {
            activity.updateTitle(title, subtitle, enableBackButton)
        }

        protected fun hideActionBar() {
            activity.hideActionBar()
        }

        override fun onRestoreView(state: BoothState) {
            renderTitle(state)
        }
    }

    abstract class ContentPresenter(
        activity: MainActivity,
        viewController: ContentViewController
    )
        : BoothPresenter(activity, viewController) {

        override fun onCreate(initialState: BoothState) {
            lastState = initialState
            activity.runOnUiThread {
                (viewController as ContentViewController).renderTitle(lastState)
                viewController.render(lastState)
            }
            makeSubscriptions()
        }

        override fun changeState(newState: BoothState) {
            if (newState != lastState) {
                lastState = newState
                activity.runOnUiThread {
                    (viewController as ContentViewController).renderTitle(newState)
                    viewController.render(newState)
                }
            }
        }

        open fun onOptionsItemSelected(item: MenuItem): Boolean = false
    }

    override fun onUnhide() {
        super.onUnhide()
        if (viewController != null)
            (viewController as ContentViewController).renderTitle(presenter?.lastState ?: makeInitialState(null, null))
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            return (presenter as ContentPresenter).onOptionsItemSelected(item)
        }
        return false
    }
}