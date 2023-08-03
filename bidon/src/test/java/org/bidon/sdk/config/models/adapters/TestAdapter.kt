package org.bidon.sdk.config.models.adapters

import android.content.Context
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.regulation.Regulation

internal class TestAdapter(
    override val demandId: DemandId,
    private val testAdapterParameters: TestAdapterParameters,
) : Adapter,
    SupportsRegulation,
    Initializable<TestAdapterParameters>,
    AdProvider.Interstitial<TestInterstitialParameters> {

    override val adapterInfo = AdapterInfo(adapterVersion = "adapterVersion1", sdkVersion = "sdkVersion1")

    override suspend fun init(context: Context, configParams: TestAdapterParameters) {
        // do nothing, init emulation
    }

    override fun parseConfigParam(json: String) = testAdapterParameters

    override fun interstitial(): AdSource.Interstitial<TestInterstitialParameters> {
        return TestInterstitialImpl(
            testParameters = testAdapterParameters
        )
    }

    override fun updateRegulation(regulation: Regulation) {
    }
}