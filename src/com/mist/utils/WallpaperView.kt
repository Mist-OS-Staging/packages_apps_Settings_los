/*
 * Copyright (C) 2023-2024 The risingOS Android Project
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

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.content.res.use
import com.android.settings.R
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

open class WallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var currentWallpaperDrawable: Drawable? = null
    private var isBlurred: Boolean = false
    private var cachedBitmap: Bitmap? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.WallpaperView, 0, 0).use {
            isBlurred = it.getBoolean(R.styleable.WallpaperView_blurred, false)
        }
        startWallpaperUpdates()
    }

    private fun startWallpaperUpdates() {
        scope.launch {
            while (isActive) {
                setWallpaperPreview()
                delay(2000)
            }
        }
    }

    protected open fun updateWallpaper() {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperDrawable: Drawable? = wallpaperManager.drawable

        if (wallpaperDrawable != currentWallpaperDrawable) {
            currentWallpaperDrawable = wallpaperDrawable
            wallpaperDrawable?.let { drawable ->
                setImageDrawable(drawable)
                applyBlurEffect()
                scope.launch(Dispatchers.IO) {
                    val compressedBitmap = compress(drawable)
                    withContext(Dispatchers.Main) {
                        compressedBitmap?.let { bitmap ->
                            cachedBitmap = bitmap
                            setImageDrawable(BitmapDrawable(resources, bitmap))
                        }
                    }
                }
            }
        } else if (cachedBitmap != null) {
            setImageDrawable(BitmapDrawable(resources, cachedBitmap))
            applyBlurEffect()
        }
    }

    protected open fun setWallpaperPreview() {
        updateWallpaper()
    }

    private fun compress(drawable: Drawable): Bitmap? {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun applyBlurEffect() {
        if (isBlurred) {
            val blurEffect = RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.MIRROR)
            setRenderEffect(blurEffect)
        } else {
            setRenderEffect(null)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
