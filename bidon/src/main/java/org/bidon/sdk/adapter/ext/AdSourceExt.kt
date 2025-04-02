package org.bidon.sdk.adapter.ext

import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.stats.StatisticsCollector

internal val AdSource<*>.ad get() = (this as StatisticsCollector).getAd()