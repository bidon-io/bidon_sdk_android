package org.bidon.sdk.utils.ext

/**
 * Created by Aleksei Cherniaev on 05/09/2023.
 */
internal val Any.TAG: String
    get() = this::class.java.simpleName