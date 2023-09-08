package org.bidon.sdk.utils.ext

import android.content.res.Resources

/**
 * Created by Aleksei Cherniaev on 06/09/2023.
 */
internal val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()