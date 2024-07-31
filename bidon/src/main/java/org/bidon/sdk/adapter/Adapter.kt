package org.bidon.sdk.adapter

import android.content.Context
import org.bidon.sdk.auction.AdTypeParam

/**
 * Created by Bidon Team on 06/02/2023.
 */
sealed interface Adapter {
    val demandId: DemandId
    val adapterInfo: AdapterInfo

    interface Bidding : Adapter {
        suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String?
    }

    interface Network : Adapter
}