package com.dlfsystems.BoothClient

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import android.view.*
import com.auth0.android.jwt.JWT
import com.auth0.android.result.Credentials
import com.dlfsystems.BoothClient.fragments.LoginFragment
import com.dlfsystems.BoothClient.fragments.StreamlistFragment
import com.dlfsystems.BoothClient.nav.BaseKey
import com.dlfsystems.BoothClient.nav.ContentFragment
import com.dlfsystems.BoothClient.nav.FragmentStateChanger
import com.zhuinden.simplestack.BackstackDelegate
import com.zhuinden.simplestack.History
import com.zhuinden.simplestack.StateChange
import com.zhuinden.simplestack.StateChanger
import kotlinx.android.synthetic.main.activity_main.*
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import com.crashlytics.android.answers.LoginEvent
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.nav.Rudder

class MainActivity : AppCompatActivity(), StateChanger {

    lateinit var backstackDelegate: BackstackDelegate
    lateinit var fragmentStateChanger: FragmentStateChanger
    var disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        backstackDelegate = BackstackDelegate()
        backstackDelegate.onCreate(savedInstanceState,
            lastCustomNonConfigurationInstance,
            History.single(LoginFragment.LoginKey()))
        backstackDelegate.registerForLifecycleCallbacks(this)

        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())

        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        fragmentStateChanger = FragmentStateChanger(supportFragmentManager, R.id.content_frame)
        backstackDelegate.setStateChanger(this)

        disposables += Rudder.navDest.distinctUntilChanged().observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                navigateTo(it.makeKey())
            }

        disposables += Rudder.playTarget.observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                it.execute(this)
            }

        val savedAuthToken = Prefs(this).accessToken
        if (savedAuthToken != "") {
            API.token = savedAuthToken
            val lastPlayState = Rudder.playState
            if (lastPlayState.value?.playing == true) {
                setBackStackRoot(Rudder.NavTarget.Streamlist().makeKey())
                Rudder.navTo(Rudder.NavTarget.Stream(lastPlayState.value!!.mountPoint))
            } else {
                Rudder.navTo(Rudder.NavTarget.Streamlist())
            }
        }
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }

    // content navigation

    override fun onRetainCustomNonConfigurationInstance() =
        backstackDelegate.onRetainCustomNonConfigurationInstance()

    override fun onBackPressed() {
        hideKeyboard()
        if (!backstackDelegate.onBackPressed()) {
            super.onBackPressed()
        }
    }

    private fun setBackStackRoot(rootKey: BaseKey) {
        backstackDelegate.backstack.setHistory(History.single(rootKey), StateChange.REPLACE)
    }

    fun navigateTo(key: BaseKey) {
        if ((key is StreamlistFragment.StreamlistKey) or (key is LoginFragment.LoginKey)) {
            setBackStackRoot(key)
        }
        hideKeyboard()
        backstackDelegate.backstack.goTo(key)
    }

    fun navigateBack() {
        hideKeyboard()
        backstackDelegate.backstack.goBack()
    }

    override fun handleStateChange(stateChange: StateChange, completionCallback: StateChanger.Callback) {
        if (stateChange.isTopNewStateEqualToPrevious) {
            completionCallback.stateChangeComplete()
            return
        }
        fragmentStateChanger.handleStateChange(stateChange)
        completionCallback.stateChangeComplete()
    }

    // service

    fun sendServiceCommand(command: String) {
        startService(Intent(this, AudioService::class.java).setAction(command))
    }

    fun startServicePlaying(mountPoint: String) {
        startService(Intent(this, AudioService::class.java).setAction(AudioService.ACTION_START_PLAYING).putExtra("mountPoint", mountPoint))
    }

    fun makeToast(text: String) {
        runOnUiThread {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    var hasRecordPermissions: Boolean = false
    get() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
            return false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (!grantResults.isEmpty())
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Rudder.playState.value != null)
                    Rudder.setPlayState(Rudder.playState.value!!)
                Answers.getInstance().logCustom(CustomEvent("Mic Permission Granted"))
            } else {
                Answers.getInstance().logCustom(CustomEvent("Mic Permissions Denied"))
            }
    }

    // action bar

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_actionbar, menu)
        val profileMenuAction = menu?.findItem(R.id.actionProfile)
        profileMenuAction?.title = Prefs(this).userId ?: ""
        if (menu != null) contentFragment()?.renderActions(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            (android.R.id.home) -> {
                onBackPressed()
            }
            (R.id.actionAbout) -> {
                showAboutDialog()
            }
            (R.id.actionProfile) -> {
            }
            (R.id.actionSettings) -> {
            }
            (R.id.actionLogout) -> {
                logOut()
            }
            else -> {
                return contentFragment()?.onOptionsItemSelected(item) ?: false
            }
        }
        return true
    }

    fun showAboutDialog() {
        AlertDialog.Builder(this, R.style.DialogStyle)
            .setTitle("DJBooth for Android")
            .setMessage("Version 0.01a\n2019 DLF Systems\nFor more information:\nhttp://tspigot.net")
            .create().show()
    }

    fun logIn(creds: Credentials) {
        val token = creds.idToken
        if (token != null) {
            makeToast("Logged in.")
            val jwt = JWT(token)
            val email = jwt.getClaim("email").asString()
            Prefs(this).accessToken = token
            Prefs(this).userId = email
            API.token = token
            Crashlytics.setUserEmail(email)
            Answers.getInstance().logLogin(LoginEvent().putSuccess(true))
            Rudder.navTo(Rudder.NavTarget.Streamlist())
        } else {
            makeToast("Login failed.")
            Answers.getInstance().logLogin(LoginEvent().putSuccess(false))
        }
    }

    fun logOut() {
        Rudder.lastPlayingMountPoint = ""
        Prefs(this).accessToken = ""
        Prefs(this).userId = ""
        Rudder.play(Rudder.PlayTarget.Stop())
        Rudder.navTo(Rudder.NavTarget.Login())
    }

    fun updateTitle(title: String, subtitle: String = "", enableBackButton: Boolean = true) {
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(enableBackButton)
        supportActionBar?.title = title
        supportActionBar?.subtitle = subtitle
        invalidateOptionsMenu()
        supportActionBar?.show()
    }

    fun hideActionBar() {
        supportActionBar?.hide()
    }

    fun contentFragment(): ContentFragment? =
        supportFragmentManager.findFragmentById(R.id.content_frame) as ContentFragment?
}
