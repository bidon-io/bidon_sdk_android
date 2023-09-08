package org.bidon.sdk.ads.banner.render

import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Created by Aleksei Cherniaev on 07/09/2023.
 */
internal object ApplyInsetUseCase {
    fun FrameLayout.applyWindowInsets(): FrameLayout {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            (view.layoutParams as FrameLayout.LayoutParams).setMargins(
                maxOf(systemBarInsets.left, displayCutout.left),
                maxOf(systemBarInsets.top, displayCutout.top),
                maxOf(systemBarInsets.right, displayCutout.right),
                maxOf(systemBarInsets.bottom, displayCutout.bottom)
            )
            insets
        }
        requestApplyInsetsWhenAttached()
        return this
    }

    private fun View.requestApplyInsetsWhenAttached() {
        if (isAttachedToWindow) {
            requestApplyInsets()
        } else {
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    view.removeOnAttachStateChangeListener(this)
                    view.requestApplyInsets()
                }

                override fun onViewDetachedFromWindow(view: View) = Unit
            })
        }
    }
}