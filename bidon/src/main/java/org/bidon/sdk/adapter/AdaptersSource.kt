package org.bidon.sdk.adapter

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface AdaptersSource {
    val adapters: Set<Adapter>
    fun add(adapter: Adapter)
}
