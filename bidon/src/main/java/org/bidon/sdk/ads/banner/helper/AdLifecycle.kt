package org.bidon.sdk.ads.banner.helper

/**
 * Created by Aleksei Cherniaev on 11/04/2023.
 */
internal enum class AdLifecycle {
    Created,
    Loading,
    Loaded,
    LoadingFailed,
    Displaying,
    Displayed,
    DisplayingFailed,
    Destroyed,
}