package org.bidon.sdk.adapter

import android.content.Context

/**
 * Created by Aleksei Cherniaev on 22/05/2023.
 */
interface BiddingProvider {
    fun getToken(context: Context): String?
}