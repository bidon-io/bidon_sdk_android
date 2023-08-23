package org.bidon.sdk.adapter

import android.content.Context
import org.bidon.sdk.auction.Auction

/**
 * Created by Aleksei Cherniaev on 30/05/2023.
 *
 * [AdSource] working modes: [Mode.Network], [Mode.Bidding]
 * Necessary for [AdSource] to work in [Auction]:
 */
sealed interface Mode {
    interface Network : Mode
    interface Bidding : Mode {
        suspend fun getToken(context: Context): String?
    }
}