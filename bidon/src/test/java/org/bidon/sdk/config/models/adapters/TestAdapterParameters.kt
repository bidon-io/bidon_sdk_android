package org.bidon.sdk.config.models.adapters

import org.bidon.sdk.adapter.AdapterParameters

internal data class TestAdapterParameters(
    val bid: Process = Process.Succeed,
    val fill: Process = Process.Succeed,
) : AdapterParameters