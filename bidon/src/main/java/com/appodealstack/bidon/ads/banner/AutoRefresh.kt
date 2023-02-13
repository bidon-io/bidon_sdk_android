package com.appodealstack.bidon.ads.banner
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal sealed interface AutoRefresh {
    object Off : AutoRefresh
    data class On(val timeoutMs: Long) : AutoRefresh
}
