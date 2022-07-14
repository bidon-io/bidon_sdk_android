package com.appodealstack.ironsource.impl

import com.appodealstack.ironsource.ISDecorator
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.impressionData.ImpressionData
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

typealias AdUnitId = String

internal class ImpressionsHolder : ISDecorator.Impressions {
    private val impressions = MutableStateFlow<Map<AdUnitId, ImpressionData>>(mapOf())
    private var userListener: ImpressionDataListener? = null

    init {
        IronSource.addImpressionDataListener { impressionData ->
            impressions.update {
                return@update it + mapOf(impressionData.adUnit to impressionData)
            }
            userListener?.onImpressionSuccess(impressionData)
        }
    }

    override fun addImpressionDataListener(impressionDataListener: ImpressionDataListener) {
        this.userListener = impressionDataListener
    }

    override fun observeImpressions(adUnitId: String): Flow<ImpressionData> {
        return impressions.mapNotNull {
            it[adUnitId]
        }
    }
}