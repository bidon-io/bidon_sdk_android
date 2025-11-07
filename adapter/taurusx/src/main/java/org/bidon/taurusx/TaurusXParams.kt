package org.bidon.taurusx

import org.bidon.sdk.adapter.AdapterParameters

internal class TaurusXParams(
    val appId: String,
    val channel: String,
    val placementIds: List<TaurusXPlacement>,
) : AdapterParameters

internal data class TaurusXPlacement(
    val adUnitId: String,
    val adFormat: TaurusXAdFormat
)

internal enum class TaurusXAdFormat(val format: String) {
    Mrec("MREC"),
    Banner("BANNER"),
    Interstitial("INTERSTITIAL"),
    Rewarded("REWARDED");

    companion object {
        fun fromString(value: String?): TaurusXAdFormat {
            return requireNotNull(entries.firstOrNull {
                it.format.equals(
                    value,
                    ignoreCase = true
                )
            }) {
                "Unknown TaurusX ad format: $value"
            }
        }
    }
}
