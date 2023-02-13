package com.appodealstack.bidon.ads.banner.helper.impl

import android.content.res.Resources
import android.util.TypedValue
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
val Number.dpToPx get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
).toInt()