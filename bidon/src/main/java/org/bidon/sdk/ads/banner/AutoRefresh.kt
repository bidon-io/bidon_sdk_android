package org.bidon.sdk.ads.banner
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal sealed interface AutoRefresh {
    object Off : AutoRefresh
    data class On(val timeoutMs: Long) : AutoRefresh
}
