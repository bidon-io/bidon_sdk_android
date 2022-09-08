package com.appodealstack.bidon.view.helper.impl

import android.content.res.Resources
import android.util.TypedValue

val Number.dpToPx get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
).toInt()