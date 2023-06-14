package org.bidon.sdk.databinders.test

import org.bidon.sdk.BidonSdk
import org.bidon.sdk.databinders.DataBinder

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal class TestModeBinder : DataBinder<Boolean> {
    override val fieldName: String = "test"

    override suspend fun getJsonObject(): Boolean = BidonSdk.bidon.isTestMode
}