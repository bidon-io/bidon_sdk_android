package org.bidon.sdk.config.models.data.models.auction

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResponseParser
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 08/02/2023.
 */
internal class AuctionResponseParserTest {

    private val testee by lazy { JsonParsers }

    @Test
    fun `it should parse auction response`() {
        val result = testee.parseOrNull<AuctionResponse>(responseJsonStr)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(expectedModel)
    }

    private val expectedModel = AuctionResponse(
        adUnits = listOf(
            AdUnit(
                demandId = "bidmachine",
                label = "bm_interstitial_cpm",
                uid = "1718930569917632512",
                pricefloor = 10000.0,
                timeout = 5000,
                bidType = BidType.RTB,
                ext = null,
            ),
            AdUnit(
                demandId = "admob",
                label = "admob_android_interstitial_26",
                pricefloor = 26.0,
                uid = "1687095657711665152",
                timeout = 5000,
                bidType = BidType.CPM,
                ext = jsonObject { "ad_unit_id" hasValue "ca-app-pub-7174718190807894/4883431752" }.toString(),
            )
        ),
        pricefloor = 0.01,
        auctionId = "54e4c13b-f642-4fc2-88aa-527181061390",
        auctionConfigurationId = 83,
        auctionConfigurationUid = "1801267324553007104",
        externalWinNotificationsEnabled = false,
        auctionTimeout = 15000,
        noBids = listOf(
            AdUnit(
                bidType = BidType.RTB,
                demandId = "dem7",
                label = "dem7_label",
                pricefloor = 0.021,
                uid = "123567",
                timeout = 5000L,
                ext = ""
            )
        )
    )

    private val responseJsonStr = """
                  {
                  	"auction_configuration_id": 83,
                  	"auction_configuration_uid": "1801267324553007104",
                  	"external_win_notifications": false,
                  	"ad_units": [{
                  		"demand_id": "bidmachine",
                  		"uid": "1718930569917632512",
                  		"label": "bm_interstitial_cpm",
                  		"pricefloor": 10000,
                        "timeout": 5000,
                  		"bid_type": "RTB",
                  	}, {
                  		"demand_id": "admob",
                  		"uid": "1687095657711665152",
                  		"label": "admob_android_interstitial_26",
                  		"pricefloor": 26,
                        "timeout": 5000,
                  		"bid_type": "CPM",
                  		"ext": {
                  			"ad_unit_id": "ca-app-pub-7174718190807894/4883431752"
                  		}
                  	}],
                  	"segment": {
                  		"id": "",
                  		"uid": ""
                  	},
                  	"token": "{}",
                  	"auction_pricefloor": 0.01,
                  	"auction_timeout": 15000,
                  	"auction_id": "54e4c13b-f642-4fc2-88aa-527181061390"
                  }
    """.trimIndent()

    @Test
    fun `it should parse auction_configuration_uid as String`() {
        val responseJsonStr = """
        {
        	"auction_configuration_id": 83,
        	"auction_configuration_uid": "1801267324553007104",
        	"external_win_notifications": false,
        	"ad_units": [{
        		"demand_id": "bidmachine",
        		"uid": "1718930569917632512",
        		"label": "bm_interstitial_cpm",
        		"pricefloor": 10000,
        		"bid_type": "CPM",
        		"ext": {}
        	}, {
        		"demand_id": "admob",
        		"uid": "1687095657711665152",
        		"label": "admob_android_interstitial_26",
        		"pricefloor": 26,
        		"bid_type": "CPM",
        		"ext": {
        			"ad_unit_id": "ca-app-pub-7174718190807894/4883431752"
        		}
        	}, {
        		"demand_id": "dtexchange",
        		"uid": "1659215744401014784",
        		"label": "dt_android_inter_25",
        		"pricefloor": 25,
        		"bid_type": "CPM",
        		"ext": {
        			"spot_id": "1311439"
        		}
        	}, {
        		"demand_id": "admob",
        		"uid": "1677285864872476672",
        		"label": "mergeblocks android admob inter 13",
        		"pricefloor": 13,
        		"bid_type": "CPM",
        		"ext": {
        			"ad_unit_id": "ca-app-pub-7174718190807894/7935438563"
        		}
        	}, {
        		"demand_id": "dtexchange",
        		"uid": "1633841366150807552",
        		"label": "dt_android_inter_12",
        		"pricefloor": 12,
        		"bid_type": "CPM",
        		"ext": {
        			"spot_id": "1187218"
        		}
        	}, {
        		"demand_id": "admob",
        		"uid": "1669346307724148736",
        		"label": "admob_android_interstitial_6",
        		"pricefloor": 6,
        		"bid_type": "CPM",
        		"ext": {
        			"ad_unit_id": "ca-app-pub-7174718190807894/8924924287"
        		}
        	}, {
        		"demand_id": "admob",
        		"uid": "1677285114813480960",
        		"label": "mergeblocks android admob inter 3",
        		"pricefloor": 3,
        		"bid_type": "CPM",
        		"ext": {
        			"ad_unit_id": "ca-app-pub-7174718190807894/8701725322"
        		}
        	}, {
        		"demand_id": "dtexchange",
        		"uid": "1659215523843538944",
        		"label": "dt_android_inter_2",
        		"pricefloor": 2,
        		"bid_type": "CPM",
        		"ext": {
        			"spot_id": "1311431"
        		}
        	}, {
        		"demand_id": "vungle",
        		"uid": "1687107176709095424",
        		"label": "vungle_bidding_android_inter",
        		"pricefloor": 1.48127613,
        		"bid_type": "RTB",
        		"ext": {
        			"payload": "payload",
        			"placement_id": "INTER_TEST-1131362"
        		}
        	}],
        	"segment": {
        		"id": "",
        		"uid": ""
        	},
        	"token": "{}",
        	"auction_pricefloor": 0.001,
        	"auction_timeout": 15000,
        	"auction_id": "54e4c13b-f642-4fc2-88aa-527181061390"
        }
        """.trimIndent()
        val res = AuctionResponseParser().parseOrNull(responseJsonStr)
        assertThat(res?.auctionConfigurationUid).isEqualTo("1801267324553007104")
    }

    @Test
    fun `it should parse auction_configuration_id as Long`() {
        val responseJsonStr = """
        {
        	"auction_configuration_id": 83,
        	"auction_configuration_uid": "1801267324553007104",
        	"external_win_notifications": false,
        	"ad_units": [{
        		"demand_id": "bidmachine",
        		"uid": "1718930569917632512",
        		"label": "bm_interstitial_cpm",
        		"pricefloor": 10000,
              "timeout": 5000,
        		"bid_type": "RTB",
        	}, {
        		"demand_id": "admob",
        		"uid": "1687095657711665152",
        		"label": "admob_android_interstitial_26",
        		"pricefloor": 26,
              "timeout": 5000,
        		"bid_type": "CPM",
        		"ext": {
        			"ad_unit_id": "ca-app-pub-7174718190807894/4883431752"
        		}
        	}],
        	"segment": {
        		"id": "",
        		"uid": ""
        	},
        	"token": "{}",
        	"auction_pricefloor": 0.01,
        	"auction_timeout": 15000,
        	"auction_id": "54e4c13b-f642-4fc2-88aa-527181061390"
        }
        """.trimIndent()
        val res = AuctionResponseParser().parseOrNull(responseJsonStr)
        assertThat(res?.auctionConfigurationId).isEqualTo(83)
    }
}