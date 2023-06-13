package org.bidon.sdk.databinders.extras

/**
 * Created by Bidon Team on 24/03/2023.
 *
 * Allows to collect extra data.
 */
interface Extras {
    /**
     * @param key name of extra data
     * @param value value of extra data. Null removes data if exists.
     *              Possible types are String, Int, Long, Double, Float, Boolean, Char
     */
    fun addExtra(key: String, value: Any?)
    fun getExtras(): Map<String, Any>
}