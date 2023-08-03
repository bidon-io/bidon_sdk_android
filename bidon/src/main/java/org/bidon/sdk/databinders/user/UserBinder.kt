package org.bidon.sdk.databinders.user

import org.bidon.sdk.config.models.User
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class UserBinder(
    private val dataSource: UserDataSource
) : DataBinder<JSONObject> {
    override val fieldName: String = "user"

    override suspend fun getJsonObject(): JSONObject = createUser().serialize()

    private fun createUser(): User {
        return User(
            platformAdvertisingId = dataSource.getAdvertisingId(),
            trackingAuthorizationStatus = dataSource.getTrackingAuthorizationStatus(),
            applicationId = dataSource.getApplicationId(),
        )
    }
}
