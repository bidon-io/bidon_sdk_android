package org.bidon.sdk.adapter

import org.bidon.sdk.auction.AdTypeParam

/**
 * Created by Bidon Team on 06/02/2023.
 */
public sealed interface Adapter {
    public val demandId: DemandId
    public val adapterInfo: AdapterInfo

    public interface Bidding : Adapter {
        public suspend fun getToken(adTypeParam: AdTypeParam): String?
    }

    public interface Network : Adapter
}