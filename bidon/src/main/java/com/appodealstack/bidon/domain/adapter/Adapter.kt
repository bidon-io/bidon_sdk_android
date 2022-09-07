package com.appodealstack.bidon.domain.adapter

import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.domain.common.DemandId

interface Adapter {
    val demandId: DemandId
    val adapterInfo: AdapterInfo
}
