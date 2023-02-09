package com.appodealstack.bidon.adapters

import android.app.Activity
import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.domain.adapter.*
import com.appodealstack.bidon.domain.common.DemandAd
import com.appodealstack.bidon.domain.common.DemandId

internal object TestAdapterParameters : AdapterParameters

internal class TestAdapter(
    private val demandName: String,
    private val interstitialData: TestAdapterInterstitialParameters,
) : Adapter,
    Initializable<TestAdapterParameters>,
    AdProvider.Interstitial<TestAdapterInterstitialParameters> {
    override val demandId = DemandId(demandName)
    override val adapterInfo = AdapterInfo(adapterVersion = "adapterVersion1", sdkVersion = "sdkVersion1")
    override suspend fun init(activity: Activity, configParams: TestAdapterParameters) {
        // do nothing, init emulation
    }

    override fun parseConfigParam(json: String) = TestAdapterParameters

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<TestAdapterInterstitialParameters> {
        return TestAdapterInterstitialImpl(demandId, roundId, interstitialData)
    }
}