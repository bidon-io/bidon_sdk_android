package com.appodealstack.bidon.data.binderdatasources

import com.appodealstack.bidon.domain.databinders.DataBinderType

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface DataProvider {
    suspend fun provide(dataBinders: List<DataBinderType>): Map<String, Any>
}
