package org.bidon.sdk.regulation

/**
 * Created by Aleksei Cherniaev on 21/06/2023.
 */
enum class Coppa(val code: Int) {
    Unknown(code = -1),
    No(code = 0),
    Yes(code = 1);

    companion object {
        val Default get() = Unknown
    }
}