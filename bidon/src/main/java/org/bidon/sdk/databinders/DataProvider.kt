package org.bidon.sdk.databinders

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface DataProvider {
    suspend fun provide(dataBinders: List<DataBinderType>): Map<String, Any>
}
