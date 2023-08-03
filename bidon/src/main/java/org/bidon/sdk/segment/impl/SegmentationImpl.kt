package org.bidon.sdk.segment.impl

import org.bidon.sdk.segment.Segment
import org.bidon.sdk.segment.Segmentation
import org.bidon.sdk.utils.di.get

internal class SegmentationImpl : Segmentation {
    override val segment: Segment get() = get()
}