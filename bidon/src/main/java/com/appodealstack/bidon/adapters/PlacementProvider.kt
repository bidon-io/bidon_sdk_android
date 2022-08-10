package com.appodealstack.bidon.adapters

interface PlacementProvider {
    fun setPlacement(placement: String?)
    fun getPlacement(): String?
}

interface PlacementSource {
    fun setPlacement(demandAd: DemandAd, placement: String?)
    fun getPlacement(demandAd: DemandAd): String?
}

class PlacementSourceImpl : PlacementSource {
    private val placements = mutableMapOf<DemandAd, String?>()

    override fun setPlacement(demandAd: DemandAd, placement: String?) {
        placements[demandAd] = placement
    }

    override fun getPlacement(demandAd: DemandAd): String? {
        return placements[demandAd]
    }
}