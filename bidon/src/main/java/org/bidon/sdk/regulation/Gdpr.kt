package org.bidon.sdk.regulation

/**
 * Created by Aleksei Cherniaev on 21/06/2023.
 */
enum class Gdpr(val code: Int) {
    Unknown(code = -1),
    Denied(code = 0),
    Given(code = 1);

    companion object {
        val Default get() = Unknown
    }
}