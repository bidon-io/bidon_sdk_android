package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.config.domain.models.AcceptedVendors
import com.appodealstack.bidon.config.domain.models.Consent
import com.appodealstack.bidon.config.domain.models.Iab
import com.appodealstack.bidon.config.domain.models.User
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.utilities.datasource.user.UserDataSource
import com.appodealstack.bidon.utilities.datasource.user.toconsentlib.Vendor
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class UserBinder(
    private val dataSource: UserDataSource
) : DataBinder {
    override val fieldName: String = "user"

    override suspend fun getJsonElement(): JsonElement = BidonJson.encodeToJsonElement(createUser())

    private fun createUser(): User {
        return User(
            idfa = dataSource.getIdfa(),
            trackingAuthorizationStatus = dataSource.getTrackingAuthorizationStatus(),
            idg = dataSource.getIdg(),
            consent = Consent(
                status = dataSource.getConsentStatus(),
                acceptedVendors = dataSource.getAcceptedVendors(),
                vendorListVersion = dataSource.getVendorListVersion(),
                createdAt = dataSource.getConsentCreatedAt(),
                updatedAt = dataSource.getConsentUpdatedAt(),
                zone = dataSource.getConsentZone(),
                iab = Iab(
                    IABUSPrivacyString = dataSource.getIABUSPrivacyString(),
                    IABConsentConsentString = dataSource.getIABConsentConsentString(),
                    IABConsentParsedPurposeConsents = dataSource.getIABConsentParsedPurposeConsents(),
                    IABConsentParsedVendorConsents = dataSource.getIABConsentParsedVendorConsents(),
                    IABConsentSubjectToGDPR = dataSource.getIABConsentSubjectToGDPR(),
                )
            ),
            coppa = dataSource.getCoppa(),
        )
    }
}