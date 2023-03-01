package org.bidon.dtexchange

import org.bidon.sdk.adapter.AdapterParameters

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
data class DataExchangeParameters(
    val appId: String,
    val coppa: Boolean,
) : AdapterParameters