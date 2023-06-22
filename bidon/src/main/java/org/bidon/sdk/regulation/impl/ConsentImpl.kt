package org.bidon.sdk.regulation.impl

import org.bidon.sdk.regulation.Consent
import org.bidon.sdk.regulation.Regulation
import org.bidon.sdk.utils.di.get

internal class ConsentImpl : Consent {
    override val regulation: Regulation by lazy { get() }
}