package org.bidon.sdk.config.models.adapters

import android.app.Activity
import org.bidon.sdk.adapter.*

internal class TestAdapter(
    override val demandId: DemandId,
    private val testAdapterParameters: TestAdapterParameters,
) : Adapter,
    Initializable<TestAdapterParameters>,
    AdProvider.Interstitial<TestInterstitialParameters> {

    override val adapterInfo = AdapterInfo(adapterVersion = "adapterVersion1", sdkVersion = "sdkVersion1")

    override suspend fun init(activity: Activity, configParams: TestAdapterParameters) {
        // do nothing, init emulation
    }

    override fun parseConfigParam(json: String) = testAdapterParameters

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<TestInterstitialParameters> {
        return TestInterstitialImpl(
            demandId = demandId,
            auctionId = auctionId,
            roundId = roundId,
            testParameters = testAdapterParameters
        )
    }
}