package com.dlfsystems.BoothClient.nav

import android.util.Log
import com.zhuinden.simplestack.StateChange

class FragmentStateChanger(
    private val fragmentManager: androidx.fragment.app.FragmentManager,
    private val containerId: Int
) {

    fun handleStateChange(stateChange: StateChange) {
        fragmentManager.beginTransaction().disallowAddToBackStack().apply {

            val previousState = stateChange.getPreviousState<BaseKey>()
            val newState = stateChange.getNewState<BaseKey>()

            val animationSet = if (stateChange.direction == StateChange.FORWARD)
                                    stateChange.topNewState<BaseKey>().getAnimation()
                                else
                                    stateChange.topPreviousState<BaseKey>()?.getBackAnimation() ?: FragAnimPair(0, 0)

            setCustomAnimations(animationSet.animIn, animationSet.animOut)

            for (oldKey in previousState) {
                val fragment = fragmentManager.findFragmentByTag(oldKey.fragmentTag)
                if (fragment != null) {
                    if (!newState.contains(oldKey)) {
                        Log.d("boothclient", "FNORD FSC remove " + fragment.toString())
                        remove(fragment)
                    } else if (!fragment.isHidden) {
                        Log.d("boothclient", "FNORD FSC hide " + fragment.toString())
                        hide(fragment)
                    }
                }
            }
            for (newKey in newState) {
                var fragment: androidx.fragment.app.Fragment? = fragmentManager.findFragmentByTag(newKey.fragmentTag)
                if (newKey == stateChange.topNewState<Any>()) {
                    if (fragment != null) {
                        if (fragment.isHidden) {
                            Log.d("boothclient", "FNORD FSC show oldfrag " + fragment.toString())
                            show(fragment)
                        }
                    } else {
                        fragment = newKey.newFragment()
                        Log.d("boothclient", "FNORD FSC create and add " + fragment.toString())
                        add(containerId, fragment, newKey.fragmentTag)
                    }
                } else {
                    if (fragment != null && !fragment.isHidden) {
                        Log.d("boothclient", "FNORD FSC hide non-top frag in newstate " + fragment.toString())
                        hide(fragment)
                    }
                }
            }
        }
            .commitNow()
    }
}