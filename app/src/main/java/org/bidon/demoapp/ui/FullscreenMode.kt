package org.bidon.demoapp.ui

enum class FullscreenMode(val code: Int) {
    //    Normal (1),
    TranslucentNavigation(2),
    Immersive(4);

    companion object {
        val Default get() = TranslucentNavigation
    }
}