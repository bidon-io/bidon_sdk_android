package org.bidon.sdk.databinders.extras

/**
 * Created by Bidon Team on 24/03/2023.
 *
 * Allows to collect extra data.
 */
public interface Extras {
    /**
     * @param key name of extra data
     * @param value value of extra data. Null removes data if exists.
     *              Possible types are String, Int, Long, Double, Float, Boolean, Char
     */
    public fun addExtra(key: String, value: Any?)
    public fun getExtras(): Map<String, Any>
}