package org.bidon.sdk.ads.banner.render

import android.content.Context
import android.graphics.Point
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import org.bidon.sdk.ads.banner.render.ApplyInsetUseCase.applyWindowInsets
import org.bidon.sdk.databinders.app.UnitySpecificInfo
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.TAG

/**
 * Created by Aleksei Cherniaev on 24/11/2023.
 */
internal class RootAdContainer(context: Context) : FrameLayout(context) {

    init {
        applyWindowInsets()
        this.clipChildren = false
        this.clipToPadding = false
    }

    private val layoutChangeListener = OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        bringToFrontIfNeed()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlugin()) {
            runCatching {
                (this.parent as? ViewGroup)?.addOnLayoutChangeListener(layoutChangeListener)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isPlugin()) {
            runCatching {
                (this.parent as? ViewGroup)?.removeOnLayoutChangeListener(layoutChangeListener)
            }
        }
    }

    fun obtainSize(onFinished: (Point) -> Unit) {
        this.viewTreeObserver?.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val safeAreaScreenSize = Point(this@RootAdContainer.width, this@RootAdContainer.height)
                    this@RootAdContainer.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    onFinished(safeAreaScreenSize)
                }
            }
        )
    }

    fun clearRootContainer() {
        this.removeAllViews()
        (this.parent as? ViewGroup)?.removeView(this)
    }

    private fun isPlugin(): Boolean {
        return UnitySpecificInfo.pluginVersion != null && UnitySpecificInfo.frameworkVersion != null
    }

    private fun bringToFrontIfNeed() {
        if (isPlugin()) {
            runCatching {
                (parent as? ViewGroup)?.let { root ->
                    val rootAdContainerIndex = root.indexOfChild(this)
                    if (rootAdContainerIndex != -1) {
                        for (index in rootAdContainerIndex + 1..root.childCount) {
                            val child = root.getChildAt(index) ?: continue
                            if (child.javaClass.simpleName == "UnityPlayer") {
                                logInfo(this@RootAdContainer.TAG, "Bring to front")
                                this@RootAdContainer.bringToFront()
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}