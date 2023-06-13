package org.bidon.sdk.ads.banner.helper.impl

import android.content.res.Resources
import android.util.TypedValue
/**
 * Created by Bidon Team on 06/02/2023.
 */
val Number.dpToPx get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
).toInt()