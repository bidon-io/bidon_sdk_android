package com.appodealstack.bidon.config

import android.os.Bundle
import com.appodealstack.bidon.demands.DemandId
import org.json.JSONObject

class StaticJsonConfiguration : Configuration {
    private val initConfigs: Map<DemandId, Config.Demand> by lazy {
        parseJson()
    }

    override suspend fun getConfiguration(demandId: DemandId): Config.Demand? {
        return initConfigs[demandId]
    }

    private fun parseJson(): Map<DemandId, Config.Demand> {
        val json = JSONObject(jsonString)
        val array = json.getJSONArray("adapters")
        return (0 until array.length()).associate { index ->
            val adapterParams = array.getJSONObject(index)
            val demandId = adapterParams.getString("id")
            val params = adapterParams.keys()
                .asSequence()
                .associateWith { key -> adapterParams.get(key) }
            DemandId(demandId) to params.toConfigInit()
        }
    }

    private fun Map<String, Any>.toConfigInit() = Config.Demand {
        Bundle().apply {
            this@toConfigInit.forEach { (key, value) ->
                when (value) {
                    is String -> this.putString(key, value)
                    is Int -> this.putInt(key, value)
                    is Long -> this.putLong(key, value)
                    is Float -> this.putFloat(key, value)
                    is Double -> this.putDouble(key, value)
                    is Boolean -> this.putBoolean(key, value)
                }
            }
        }
    }

    private val jsonString = """
        {
            "launch":
            {
                "adapters":
                [
                    "appodeal",
                    "firebase"
                ]
            },
            "analytics":
            {
                "enabled": true,
                "log_level": "debug",
                "adapters":
                [
                    "user",
                    "firebase"
                ]
            },
            "measurment": {
                "adapters":
                [
                    "appsflyer"
                ]
            },
            "ads": {
                "interstital":
                {
                    "auction":   [
                        {
                            "id": "prebid",
                            "adapters":
                            [
                                "bidmachine"
                            ],
                            "tmax": 10000
                        },
                        {
                            "id": "mediation",
                            "adapters":
                            [
                                "appodeal"
                            ],
                            "tmax": 15000
                        },
                        {
                            "id": "postbid",
                            "adapters":
                            [
                                "bidmachine"
                            ],
                            "tmax": 5000
                        }
                    ]
                }
            },
            "adapters":
            [
                {
                    "id": "appsflyer",
                    "class": "Class",
                    "dev_key": "some dev key",
                    "appsflyer_id": "some_appsflyer_id"
                },
                {
                    "id": "user",
                    "class": "Class",
                    "features":
                    [
                        "ad_revenue",
                        "mediation",
                        "session"
                    ]
                },
                {
                    "id": "firebase",
                    "class": "Class",
                    "features":
                    [
                        "ad_revenue",
                        "mediation",
                        "session"
                    ]
                },
                {
                    "id": "appodeal",
                    "class": "Class",
                    "app_key": "key used for app",
                    "ad_types":
                    [
                        "banner",
                        "interstital",
                        "rewarded"
                    ],
                    "autocache":
                    [
                        "interstital",
                        "rewarded"
                    ]
                },
                {
                    "id": "bidmachine",
                    "class": "Class",
                    "url": "https://bidmachine.com"
                }
            ]
        }
    """.trimIndent()
}