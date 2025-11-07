package org.bidon.sdk.ads

public class AuctionInfo(
    public val auctionId: String,
    public val auctionConfigurationId: Long?,
    public val auctionConfigurationUid: String?,
    public val auctionTimeout: Long,
    public val auctionPricefloor: Double,
    public val noBids: List<AdUnitInfo>?,
    public val adUnits: List<AdUnitInfo>?,
)

public class AdUnitInfo(
    public val demandId: String,
    public val label: String?,
    public val price: Double?,
    public val uid: String?,
    public val bidType: String?,
    public val fillStartTs: Long?,
    public val fillFinishTs: Long?,
    public val status: String?,
    public val ext: String?,
)