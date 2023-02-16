package com.appodealstack.bidon.adapters

import android.app.Activity
import com.appodealstack.bidon.adapter.*
import com.appodealstack.bidon.adapter.AdapterInfo
import com.appodealstack.bidon.adapter.DemandAd
import com.appodealstack.bidon.adapter.DemandId

internal object TestAdapterParameters : AdapterParameters

internal class TestAdapter(
    demandName: String,
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