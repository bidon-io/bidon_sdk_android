package org.bidon.sdk.databinders

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal enum class DataBinderType {
    Device,
    App,
    Geo,
    Session,
    User,
    Token,
    Placement, // - not ready to use
    AvailableAdapters,
    Segment
}