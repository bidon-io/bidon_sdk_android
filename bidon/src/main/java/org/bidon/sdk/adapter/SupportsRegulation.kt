package org.bidon.sdk.adapter

import org.bidon.sdk.regulation.Regulation

/**
 * Created by Aleksei Cherniaev on 21/06/2023.
 */
interface SupportsRegulation {
    fun updateRegulation(regulation: Regulation)
}