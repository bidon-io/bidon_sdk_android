package com.appodealstack.bidon.adapters

import android.app.Activity
import com.appodealstack.bidon.config.data.models.AdapterInfo
import kotlinx.serialization.json.JsonObject

internal object TestAdapterParameters : AdapterParameters

internal class TestAdapter(
    private val demandName: String,
    private val interstitialData: TestAdapterInterstitialParameters,
) : Adapter, Initializable<TestAdapterParameters>,
    AdProvider.Interstitial<TestAdapterInterstitialParameters> {
    override val demandId = DemandId(demandName)
    override val adapterInfo = AdapterInfo(adapterVersion = "adapterVersion1", sdkVersion = "sdkVersion1")
    override suspend fun init(activity: Activity, configParams: TestAdapterParameters) {
        // do nothing, init emulation
    }

    override fun parseConfigParam(json: JsonObject) = TestAdapterParameters

    override fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<TestAdapterInterstitialParameters> {
        return TestAdapterInterstitialImpl(demandId, roundId, interstitialData)
    }
}