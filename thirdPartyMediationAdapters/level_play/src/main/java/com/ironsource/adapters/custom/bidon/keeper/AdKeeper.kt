package com.ironsource.adapters.custom.bidon.keeper

internal interface AdKeeper<AdInstance> {
    /**
     * Registers the given eCPM value for tracking.
     * This ensures that the eCPM is considered when determining valid ad ranges.
     *
     * @param ecpm The eCPM value to be registered for range calculations.
     */
    fun registerEcpm(ecpm: Double)

    /**
     * Returns the most recently registered eCPM value.
     * This value typically represents the eCPM floor of the last requested ad unit
     * and is used for range-based ad matching and selection.
     *
     * @return The last eCPM value registered via {@link #registerEcpm(double)}.
     */
    fun lastRegisteredEcpm(): Double?

    /**
     * Keeps the given ad instance for future use.
     * If an ad instance with a higher eCPM is already kept, the given ad instance is rejected.
     *
     * @param adInstance The ad instance to be kept.
     * @return The rejected ad instance if the given ad instance is not kept, or null otherwise.
     */
    fun keepAd(adInstance: AdInstance): AdInstance?

    /**
     * Consumes the ad instance if its eCPM is within the specified range.
     *
     * @param ecpm The eCPM value to compare against.
     * @return The consumed ad instance if its eCPM is within the range, or null otherwise.
     */
    fun consumeAd(ecpm: Double): AdInstance?
}
