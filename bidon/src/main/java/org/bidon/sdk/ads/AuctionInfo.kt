package org.bidon.sdk.ads

class AuctionInfo(
    val auctionId: String,
    val auctionConfigurationId: Long?,
    val auctionConfigurationUid: String?,
    val auctionTimeout: Long,
    val auctionPricefloor: Double,
    val noBids: List<AdUnitInfo>?,
    val adUnits: List<AdUnitInfo>?,
)

class AdUnitInfo(
    val demandId: String,
    val label: String?,
    val price: Double?,
    val uid: String?,
    val bidType: String?,
    val fillStartTs: Long?,
    val fillFinishTs: Long?,
    val timeout: Long?,
    val status: String?,
    val errorMessage: String? = null,
    val ext: String?,
)