package org.bidon.sdk.databinders.user

import org.bidon.sdk.config.models.User
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.utils.json.JsonParsers
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class UserBinder(
    private val dataSource: UserDataSource
) : DataBinder<JSONObject> {
    override val fieldName: String = "user"

    override suspend fun getJsonObject(): JSONObject = JsonParsers.serialize(createUser())

    private fun createUser(): User {
        return User(
            platformAdvertisingId = dataSource.getAdvertisingId(),
            trackingAuthorizationStatus = dataSource.getTrackingAuthorizationStatus(),
            applicationId = dataSource.getApplicationId(),
            consent = null,
            coppa = false,
        )
    }
}
