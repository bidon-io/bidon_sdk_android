package org.bidon.bidmachine.impl

import android.content.Context
import io.bidmachine.BidMachine
import org.bidon.sdk.adapter.BiddingProvider

/**
 * Created by Aleksei Cherniaev on 22/05/2023.
 */
class BMTokenGetter : BiddingProvider {
    override fun getToken(context: Context): String {
        return BidMachine.getBidToken(context)
    }
}