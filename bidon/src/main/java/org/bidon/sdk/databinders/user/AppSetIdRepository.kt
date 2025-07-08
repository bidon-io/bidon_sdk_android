package org.bidon.sdk.databinders.user

interface AppSetIdRepository {
    suspend fun getAppSetId(): String?
    suspend fun isDeveloperScope(): Boolean?
}