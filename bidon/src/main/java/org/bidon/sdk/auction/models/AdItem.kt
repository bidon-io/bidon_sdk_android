package org.bidon.sdk.auction.models

/**
 * Created by Aleksei Cherniaev on 26/09/2023.
 */
internal class AdItem(
    val lineItem: LineItem,
) {
    companion object {
        const val MAX_HISTORY_SIZE = 20
    }

    /**
     * History of ecpms for this line item
     */
    private val history = mutableListOf<Boolean>()

    fun loaded(loaded: Boolean) {
        history.add(0, loaded)
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeLast()
        }
    }

    fun weight(): Double {
        val step = 1.0 - 0.3 / MAX_HISTORY_SIZE.toDouble()
        val multiplier: (Boolean) -> Double = { succeed ->
            1.0.takeIf { succeed } ?: -1.0
        }
        val a = history.mapIndexed { index, loaded ->
            multiplier(loaded) * (1.0 - index * step)
        }
        return a.sum() * lineItem.pricefloor
    }

    override fun toString(): String {
        return "AdItem(lineItem=$lineItem, history=$history)"
    }
}
