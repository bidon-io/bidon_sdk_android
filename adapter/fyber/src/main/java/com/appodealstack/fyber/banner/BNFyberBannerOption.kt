package com.appodealstack.fyber.banner

import android.view.ViewGroup

class BNFyberBannerOption {
    sealed interface Position {
        object Top : Position
        object Bottom : Position
        class InViewGroup(val viewGroup: ViewGroup) : Position
    }

    private var position: Position = Position.Top

    fun placeAtTheTop(): BNFyberBannerOption {
        position = Position.Top
        return this
    }

    fun placeAtTheBottom(): BNFyberBannerOption {
        position = Position.Bottom
        return this
    }

    fun placeInContainer(viewGroup: ViewGroup): BNFyberBannerOption {
        position = Position.InViewGroup(viewGroup)
        return this
    }

    fun getPosition(): Position = position
}