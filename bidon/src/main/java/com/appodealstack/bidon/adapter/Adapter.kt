package com.appodealstack.bidon.adapter

import com.appodealstack.bidon.ads.DemandId
import com.appodealstack.bidon.config.models.AdapterInfo

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface Adapter {
    val demandId: DemandId
    val adapterInfo: AdapterInfo
}
