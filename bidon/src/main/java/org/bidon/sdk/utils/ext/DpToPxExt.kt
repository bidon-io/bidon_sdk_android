package org.bidon.sdk.utils.ext

import android.content.res.Resources
import android.util.TypedValue

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal val Number.dpToPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

internal val Number.pxToDp
    get() = (this.toInt() / Resources.getSystem().displayMetrics.density).toInt()