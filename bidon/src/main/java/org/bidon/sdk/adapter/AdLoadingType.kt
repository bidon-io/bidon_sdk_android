package org.bidon.sdk.adapter

import android.content.Context

/**
 * Created by Aleksei Cherniaev on 30/05/2023.
 */

sealed interface AdLoadingType<T : AdAuctionParams> {
    /**
     * Classic mediation ad network
     */
    interface Network<T : AdAuctionParams> : AdLoadingType<T> {
        fun fill(adParams: T)
    }

    /**
     * Bidding ad network
     */
    interface Bidding<T : AdAuctionParams> : AdLoadingType<T> {
        fun getToken(context: Context): String?
        fun adRequest(adParams: T)
        fun fill()
    }
}