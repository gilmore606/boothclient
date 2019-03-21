package com.dlfsystems.BoothClient.fragments

import android.os.Bundle
import com.google.android.material.textfield.TextInputEditText
import android.view.View
import android.widget.Button
import com.dlfsystems.BoothClient.*
import com.dlfsystems.BoothClient.apimodel.ModelDJProfile
import com.dlfsystems.BoothClient.apimodel.ModelStreamConfig
import com.dlfsystems.BoothClient.apis.API
import com.dlfsystems.BoothClient.nav.FragAnimPair
import com.dlfsystems.BoothClient.nav.BaseKey
import com.dlfsystems.BoothClient.nav.ContentFragment
import com.dlfsystems.BoothClient.nav.Rudder
import com.dlfsystems.BoothClient.views.Tagbag
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.parcel.Parcelize

class StreamConfigFragment
    : ContentFragment() {

    @Parcelize
    data class StreamConfigKey(val mountPoint: String, val description: String)
             : BaseKey() {

        constructor(): this("StreamConfigKey", "")

        override fun createFragment(): ContentFragment = StreamConfigFragment().apply {
            arguments = (arguments ?: Bundle()).also { bundle ->
                bundle.putSerializable("mountpoint", mountPoint)
                bundle.putSerializable("description", description)
            }
        }

        override fun getAnimation(): FragAnimPair =
                FragAnimPair(R.anim.slide_in_top, R.anim.stationary)

        override fun getBackAnimation(): FragAnimPair =
                FragAnimPair(R.anim.stationary, R.anim.slide_out_top)
    }

    data class StreamConfigState(
        val createOnSubmit: Boolean = true,
        val mountPoint: String = "",
        val description: String = "",
        val spinning: ArrayList<String> = ArrayList(0)
    ) : BoothState()

    class StreamConfigView(
        activity: MainActivity,
        mainView: View
    )
        : ContentViewController(activity) {

        val inputMount: TextInputEditText = mainView.findViewById(R.id.streamconfig_input_mount)
        val inputDescription: TextInputEditText = mainView.findViewById(R.id.streamconfig_input_description)
        val inputTagbag: Tagbag = mainView.findViewById(R.id.streamconfig_tagbag)
        val buttonApply: Button = mainView.findViewById(R.id.streamconfig_button_done)

        override fun renderTitle(state: BoothState) {
            state as StreamConfigState
            if (!state.createOnSubmit) updateTitle(getString(R.string.title_stream_settings, state.mountPoint))
            else updateTitle(getString(R.string.title_new_stream))
        }

        override fun render(state: BoothState) {
            state as StreamConfigState
            if (!state.createOnSubmit) {
                inputMount.focusable = View.NOT_FOCUSABLE
                inputMount.isClickable = false
            }
            inputMount.setText(state.mountPoint)
            inputDescription.setText(state.description)
            inputTagbag.replaceTags(state.spinning)
        }
    }

    class StreamConfigPresenter(
        activity: MainActivity,
        viewController: StreamConfigView
    )
        : ContentPresenter(activity, viewController) {

        override fun makeSubscriptions() {
            viewController as StreamConfigView
            val mount = (lastState as StreamConfigState).mountPoint
            if (mount != "") {
                disposables += API.getStreamInfo(mount).asyncIO()
                    .subscribe({
                        changeState(
                            (lastState as StreamConfigState).copy(
                                spinning = it.profile.spinning
                            )
                        )
                    }, {
                    })
            }

            disposables += viewController.inputTagbag.tagStrings
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    lastState = (lastState as StreamConfigState).copy(
                        spinning = it)
                }
            disposables += viewController.inputMount.textChanges()
                .liveSearchDebounce()
                .subscribe {
                    lastState = (lastState as StreamConfigState).copy(
                        mountPoint = it.toString())
                }
            disposables += viewController.inputDescription.textChanges()
                .liveSearchDebounce()
                .subscribe {
                    lastState = (lastState as StreamConfigState).copy(
                        description = it.toString())
                }

            disposables += viewController.buttonApply.clicks().onMainThread()
                .subscribe {
                    val state = lastState as StreamConfigState
                    val model = ModelStreamConfig(
                        mountPoint = state.mountPoint,
                        description = state.description,
                        profile = ModelDJProfile(spinning = state.spinning)
                    )
                    if (state.createOnSubmit) {
                        disposables += API.createStream(model).observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                activity.navigateBack()
                                activity.makeToast(getString(R.string.stream_created, model.mountPoint))
                                Rudder.play(Rudder.PlayTarget.Start(model.mountPoint))
                                Rudder.lastPlayingMountPoint = model.mountPoint
                                Rudder.navTo(Rudder.NavTarget.Stream(model.mountPoint))
                            }, {
                                activity.makeToast(getString(R.string.stream_create_failed))
                            })
                    } else {
                        disposables += API.configStream(model).observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                activity.navigateBack()
                                activity.makeToast(getString(R.string.stream_settings_updated, model.mountPoint))
                            }, {
                                activity.makeToast(getString(R.string.stream_config_failed))
                            })
                    }
                }
        }
    }

    override fun makeStateFromArguments(arguments: Bundle): BoothState {
        val mountPoint = arguments.getSerializable("mountpoint") as String
        val description = arguments.getSerializable("description") as String
        return StreamConfigState(
            mountPoint = mountPoint,
            description = description,
            createOnSubmit = (mountPoint == "")
        )
    }

    override fun makeDefaultState() =
        StreamConfigState()

    override fun getLayout() =
        R.layout.fragment_streamconfig

    override fun makeViewController(initialState: BoothState) =
        StreamConfigView(activity as MainActivity, mainView!!)

    override fun makePresenter() =
        StreamConfigPresenter(activity as MainActivity, viewController as StreamConfigView)
}