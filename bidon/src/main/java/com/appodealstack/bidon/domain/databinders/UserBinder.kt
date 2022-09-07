package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.user.UserDataSource
import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.data.models.config.User
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class UserBinder(
    private val dataSource: UserDataSource
) : DataBinder {
    override val fieldName: String = "user"

    override suspend fun getJsonElement(): JsonElement = BidonJson.encodeToJsonElement(createUser())

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
