package com.appodealstack.bidon.utilities.datasource.location

import com.appodealstack.bidon.utilities.datasource.DataSource

internal interface LocationDataSource : DataSource {

    /**
     * @return current device location latitude if permission is granted, if it's `null` - latitude
     * which were received from server. If it's not available by restrictions, will return `null`.
     */
    fun getLat(): Double?

    /**
     * @return current device location longitude if permission is granted, if it's `null` - longitude
     * which were received from server. If it's not available by restrictions, will return `null`.
     */
    fun getLon(): Double?

    fun getAccuracy(): Float?
    fun getLastFix(): Long?

    /**
     * Get county id code using ISO-3166-1-alpha-3 of current user
     *
     * @return county id code using ISO-3166-1-alpha-3 of current user
     * @ExcludeFromJavadoc
     */
    fun getCountry(): String?
    fun getRegion(): String?

    /**
     * Get city of current user
     *
     * @return city of current user
     * @ExcludeFromJavadoc
     */
    fun getCity(): String?

    /**
     * Get zip code of current user
     *
     * @return zip code of current user
     * @ExcludeFromJavadoc
     */
    fun getZip(): String?
    fun getUtcOffset(): Int
}