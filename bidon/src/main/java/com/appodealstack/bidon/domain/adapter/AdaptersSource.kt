package com.appodealstack.bidon.domain.adapter
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface AdaptersSource {
    val adapters: List<Adapter>
    fun add(adapters: List<Adapter>)
}
