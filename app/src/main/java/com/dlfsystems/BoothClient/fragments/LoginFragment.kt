package com.dlfsystems.BoothClient.fragments

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.view.View
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.provider.AuthCallback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.dlfsystems.BoothClient.*
import com.dlfsystems.BoothClient.nav.FragAnimPair
import com.dlfsystems.BoothClient.nav.BaseKey
import com.dlfsystems.BoothClient.nav.ContentFragment
import com.jakewharton.rxbinding3.view.clicks
import kotlinx.android.parcel.Parcelize

class LoginFragment : ContentFragment() {

    @Parcelize
    data class LoginKey(val tag: String) : BaseKey() {
        constructor() : this("LoginKey")
        override fun createFragment() = LoginFragment()

        override fun getAnimation() =
                FragAnimPair(R.anim.grow_fade_in_from_bottom, R.anim.shrink_fade_out_from_bottom)
    }

    data class LoginState(val dummy: Boolean = true)
        : BoothState()

    class LoginView(
        activity: MainActivity,
        val mainView: View
    )
        : ContentViewController(activity) {

        override fun renderTitle(state: BoothState) =
            hideActionBar()
    }

    class LoginPresenter(
        activity: MainActivity,
        viewController: LoginView
    )
        : ContentPresenter(activity, viewController) {

        val auth: Auth0 = Auth0(activity as Context)

        override fun makeSubscriptions() {
            disposables += (viewController as LoginView).mainView.clicks().subscribe {
                startLogin()
            }
        }

        private fun startLogin() {
            WebAuthProvider.init(auth)
                .withScope("openid profile email")
                .withScheme("boothauth")
                .start(activity, object : AuthCallback {
                    override fun onSuccess(credentials: Credentials) {
                        activity.logIn(credentials)
                    }
                    override fun onFailure(exception: AuthenticationException) {
                        activity.makeToast(getString(R.string.msg_auth_failed))
                    }
                    override fun onFailure(dialog: Dialog) {
                    }
                })
        }
    }

    override fun makeDefaultState() =
        LoginState()

    override fun getLayout() =
        R.layout.fragment_login

    override fun makeViewController(initialState: BoothState) =
        LoginView(activity as MainActivity,
            mainView!!
        )

    override fun makePresenter() =
        LoginPresenter(activity as MainActivity,
            viewController as LoginView
        )
}
