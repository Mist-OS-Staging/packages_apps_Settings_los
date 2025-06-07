/*
 * Copyright (C) 2023 the RisingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mist.utils

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.SystemProperties
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.widget.LayoutPreference
import com.mist.utils.DeviceInfoUtil

class mistInfoPreferenceController(context: Context) : AbstractPreferenceController(context) {

    private val defaultFallback = mContext.getString(R.string.device_info_default)
    private var firmwareVersionTextView: TextView? = null

    private val handler = Handler()
    private val updateTextRunnable = object : Runnable {
        override fun run() {
            animateTextChange()
            handler.postDelayed(this, 3000)
        }
    }
    private var currentMessageIndex = 0

    private val versionMessages = listOf(
        "#${getProp(PROP_MIST_CODE)}",
        "${getProp(PROP_MIST_CODE)}?",
        "v ${getMistVersion()}",
        "${getMistVersion() + 1} soon?"
    )

    private fun getProp(propName: String): String {
        return SystemProperties.get(propName, defaultFallback)
    }

    private fun getProp(propName: String, customFallback: String): String {
        val propValue = SystemProperties.get(propName)
        return if (propValue.isNotEmpty()) propValue else SystemProperties.get(customFallback, "Unknown")
    }

    private fun getMistChipset(): String {
        return getProp(PROP_MIST_CHIPSET, "ro.board.platform")
    }

    private fun getDeviceName(): String {
        val deviceName = "${Build.DEVICE}"
        return deviceName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun getMistBuildVersion(): String {
        return getProp(PROP_MIST_BUILD_VERSION)
    }

    private fun getMistSecurity(): String {
        return getProp(PROP_MIST_SECURITY)
    }

    private fun getMistVersion(): String {
        return SystemProperties.get(PROP_MIST_VERSION, "2.0")
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)

        val hwInfoPreference = screen.findPreference<LayoutPreference>(KEY_HW_INFO)

        hwInfoPreference?.apply {
            findViewById<TextView>(R.id.device_chipset)?.text = getMistChipset()
            findViewById<TextView>(R.id.device_storage)?.text =
                "${DeviceInfoUtil.getTotalRam()} | ${DeviceInfoUtil.getStorageTotal(mContext)}"
            findViewById<TextView>(R.id.device_battery_capacity)?.text = DeviceInfoUtil.getBatteryCapacity(mContext)
            findViewById<TextView>(R.id.device_resolution)?.text = DeviceInfoUtil.getScreenResolution(mContext)
            findViewById<TextView>(R.id.device_showcase)?.text = getDeviceName()
        }

        handler.post(updateTextRunnable)
    }

    private fun animateTextChange() {
        firmwareVersionTextView?.let { textView ->
            val fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f)
            fadeOut.duration = 300
            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    currentMessageIndex = (currentMessageIndex + 1) % versionMessages.size
                    textView.text = versionMessages[currentMessageIndex]
                    val fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f)
                    fadeIn.duration = 300
                    fadeIn.start()
                }
            })
            fadeOut.start()
        }
    }

    override fun isAvailable(): Boolean {
        return true
    }

    override fun getPreferenceKey(): String {
        return KEY_DEVICE_INFO
    }

    companion object {
        private const val FIRMWARE_NAME = "MistOS"
        private const val KEY_KERNEL_INFO = "kernel_version_sw"
        private const val KEY_SW_INFO = "my_device_sw_header"
        private const val KEY_HW_INFO = "my_device_hw_header"
        private const val KEY_DEVICE_INFO = "my_device_info_header"
        private const val KEY_BUILD_BANNER = "banner_logo"

        private const val PROP_MIST_CODE = "ro.mist.code"
        private const val PROP_MIST_VERSION = "ro.mist.version"
        private const val PROP_MIST_RELEASETYPE = "ro.mist.releasetype"
        private const val PROP_MIST_MAINTAINER = "ro.mist.maintainer"
        private const val PROP_MIST_BUILD_VERSION = "ro.mist.build.version"
        private const val PROP_MIST_CHIPSET = "ro.mist.chipset"
        private const val PROP_MIST_SECURITY = "ro.build.version.security_patch"
    }
}
