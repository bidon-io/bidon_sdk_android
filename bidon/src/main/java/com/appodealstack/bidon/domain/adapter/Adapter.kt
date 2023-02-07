package com.appodealstack.bidon.domain.adapter

import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.domain.common.DemandId
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface Adapter {
    val demandId: DemandId
    val adapterInfo: AdapterInfo
}
