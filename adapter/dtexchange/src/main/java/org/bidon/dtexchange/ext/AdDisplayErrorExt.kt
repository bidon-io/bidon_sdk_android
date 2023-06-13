package org.bidon.dtexchange.ext

import com.fyber.inneractive.sdk.external.InneractiveUnitController
import org.bidon.dtexchange.DTExchangeDemandId
import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 01/03/2023.
 */
internal fun InneractiveUnitController.AdDisplayError?.asBidonError() =
    BidonError.Unspecified(demandId = DTExchangeDemandId, sourceError = this)