package org.bidon.sdk.databinders.reg

import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal class RegulationsBinder(
    private val dataSource: RegulationDataSource
) : DataBinder<JSONObject> {
    override val fieldName: String
        get() = "regs"

    override suspend fun getJsonObject(): JSONObject {
        return dataSource.regulationsRequestBody.serialize()
    }
}