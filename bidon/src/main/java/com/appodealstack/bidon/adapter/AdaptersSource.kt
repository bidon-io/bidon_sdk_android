package com.appodealstack.bidon.adapter
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface AdaptersSource {
    val adapters: Set<Adapter>
    fun add(adapters: Collection<Adapter>)
}
