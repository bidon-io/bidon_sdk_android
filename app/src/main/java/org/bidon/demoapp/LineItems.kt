package org.bidon.demoapp

import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 09/09/2023.
 *
 * [Applovin](https://dash.applovin.com/o/zones?r=2)
 * [UnityAds](https://dashboard.unity3d.com/gaming/organizations/7971940023451/projects/020dc98e-105d-4862-99fd-a452220532cd/monetization/placement-management?adUnits=google_unityads_inter_1_0)
 * [Vungle-Liftoff](https://publisher.vungle.com/placements)
 * [BigoAds](https://www.bigossp.com/media/app?pageNo=1&pageSize=10)
 */
object LineItems {
    object Interstitial {
        val dsp get() = unityAds + admob + applovin + yandex
        val bidding get() = listOf("bigoads", "bidmachine", "vungle")

        private val unityAds = listOf(
            LineItem(demandId = "unityads", pricefloor = 1.0, adUnitId = "unity_inter_1_", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 2.0, adUnitId = "unity_inter_2", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 5.0, adUnitId = "unity_inter_5", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 7.0, adUnitId = "unity_inter_7", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 10.0, adUnitId = "unity_inter_10", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 15.0, adUnitId = "unity_inter_15", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 20.0, adUnitId = "unity_inter_20", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 25.0, adUnitId = "unity_inter_25", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 35.0, adUnitId = "unity_inter_35", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 50.0, adUnitId = "unity_inter_50", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 70.0, adUnitId = "unity_inter_70", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 95.0, adUnitId = "unity_inter_95", uid = ""),
        )

        private val admob = listOf(
            LineItem(demandId = "admob", pricefloor = 1.0, adUnitId = "ca-app-pub-9069030271740050/2719222246", uid = ""),
            LineItem(demandId = "admob", pricefloor = 2.0, adUnitId = "ca-app-pub-9069030271740050/9682455534", uid = ""),
            LineItem(demandId = "admob", pricefloor = 5.0, adUnitId = "ca-app-pub-9069030271740050/2527650551", uid = ""),
            LineItem(demandId = "admob", pricefloor = 7.0, adUnitId = "ca-app-pub-9069030271740050/4250165457", uid = ""),
            LineItem(demandId = "admob", pricefloor = 10.0, adUnitId = "ca-app-pub-9069030271740050/7371701555", uid = ""),
            LineItem(demandId = "admob", pricefloor = 15.0, adUnitId = "ca-app-pub-9069030271740050/4553966523", uid = ""),
            LineItem(demandId = "admob", pricefloor = 20.0, adUnitId = "ca-app-pub-9069030271740050/4579098822", uid = ""),
            LineItem(demandId = "admob", pricefloor = 25.0, adUnitId = "ca-app-pub-9069030271740050/6796986482", uid = ""),
            LineItem(demandId = "admob", pricefloor = 35.0, adUnitId = "ca-app-pub-9069030271740050/8326772146", uid = ""),
            LineItem(demandId = "admob", pricefloor = 50.0, adUnitId = "ca-app-pub-9069030271740050/5292333129", uid = ""),
            LineItem(demandId = "admob", pricefloor = 70.0, adUnitId = "ca-app-pub-9069030271740050/5009509512", uid = ""),
            LineItem(demandId = "admob", pricefloor = 95.0, adUnitId = "ca-app-pub-9069030271740050/2091454718", uid = ""),
        )

        private val applovin = listOf(
            LineItem(demandId = "applovin", pricefloor = 1.0, adUnitId = "9dd72d785cbc17fe", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 2.0, adUnitId = "82f494dc84767bcd", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 5.0, adUnitId = "71095830df3679ad", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 7.0, adUnitId = "ad6f7fcab26463d5", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 10.0, adUnitId = "abdc2d3be8365b37", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 15.0, adUnitId = "5d2bb9df44c033b7", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 20.0, adUnitId = "5dc9a141c5241bf5", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 25.0, adUnitId = "230931c9ca69cfe1", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 35.0, adUnitId = "ca86b0231fe94b4d", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 50.0, adUnitId = "cd2281f8cfe4d5e7", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 70.0, adUnitId = "6ebb8e46b8984c08", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 95.0, adUnitId = "ec1e3701da66ef14", uid = ""),
        )

        private val yandex = listOf(
            LineItem(demandId = "yandex", pricefloor = 1.0, adUnitId = "R-M-3032723-2", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 2.0, adUnitId = "R-M-3032723-17", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 5.0, adUnitId = "R-M-3032723-18", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 7.0, adUnitId = "R-M-3032723-19", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 10.0, adUnitId = "R-M-3032723-20", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 15.0, adUnitId = "R-M-3032723-21", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 20.0, adUnitId = "R-M-3032723-22", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 25.0, adUnitId = "R-M-3032723-23", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 35.0, adUnitId = "R-M-3032723-24", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 50.0, adUnitId = "R-M-3032723-25", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 70.0, adUnitId = "R-M-3032723-26", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 95.0, adUnitId = "R-M-3032723-27", uid = ""),
        )

        private val bigoads = listOf(
            LineItem(demandId = "bigoads", pricefloor = 2.0, adUnitId = "10137724-10068211", uid = ""),
            LineItem(demandId = "bigoads", pricefloor = 1.0, adUnitId = "10137724-10060400", uid = ""),
        )
    }

    object Banners {
        val dsp get() = unityAds + admob + applovin + yandex
        val bidding get() = listOf("bigoads", "bidmachine")

        private val unityAds = listOf(
            LineItem(demandId = "unityads", pricefloor = 0.01, adUnitId = "unity_banner_0_01", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 0.05, adUnitId = "unity_banner_0_05", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 0.1, adUnitId = "unity_banner_0_1", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 0.2, adUnitId = "unity_banner_0_2", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 0.3, adUnitId = "unity_banner_0_3", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 0.5, adUnitId = "unity_banner_0_5", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 0.7, adUnitId = "unity_banner_0_7", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 1.0, adUnitId = "unity_banner_1_0", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 1.5, adUnitId = "unity_banner_1_5", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 2.0, adUnitId = "unity_banner_2", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 3.0, adUnitId = "unity_banner_3", uid = ""),
            LineItem(demandId = "unityads", pricefloor = 4.0, adUnitId = "unity_banner_4", uid = ""),
        )
        private val admob = listOf(
            LineItem(demandId = "admob", pricefloor = 0.01, adUnitId = "ca-app-pub-9069030271740050/3238096991", uid = ""),
            LineItem(demandId = "admob", pricefloor = 0.05, adUnitId = "ca-app-pub-9069030271740050/1636444558", uid = ""),
            LineItem(demandId = "admob", pricefloor = 0.1, adUnitId = "ca-app-pub-9069030271740050/1444872866", uid = ""),
            LineItem(demandId = "admob", pricefloor = 0.2, adUnitId = "ca-app-pub-9069030271740050/7874549840", uid = ""),
            LineItem(demandId = "admob", pricefloor = 0.3, adUnitId = "ca-app-pub-9069030271740050/3743733147", uid = ""),
            LineItem(demandId = "admob", pricefloor = 0.5, adUnitId = "ca-app-pub-9069030271740050/4426259429", uid = ""),
            LineItem(demandId = "admob", pricefloor = 0.7, adUnitId = "ca-app-pub-9069030271740050/7982361057", uid = ""),
            LineItem(demandId = "admob", pricefloor = 1.0, adUnitId = "ca-app-pub-9069030271740050/1416952700", uid = ""),
            LineItem(demandId = "admob", pricefloor = 1.5, adUnitId = "ca-app-pub-9069030271740050/6477707698", uid = ""),
            LineItem(demandId = "admob", pricefloor = 2.0, adUnitId = "ca-app-pub-9069030271740050/9351283040", uid = ""),
            LineItem(demandId = "admob", pricefloor = 3.0, adUnitId = "ca-app-pub-9069030271740050/6286136008", uid = ""),
            LineItem(demandId = "admob", pricefloor = 4.0, adUnitId = "ca-app-pub-9069030271740050/3659972663", uid = ""),
            LineItem(demandId = "admob", pricefloor = 5.0, adUnitId = "ca-app-pub-9069030271740050/1281221331", uid = ""),
            LineItem(demandId = "admob", pricefloor = 7.0, adUnitId = "ca-app-pub-9069030271740050/9842237630", uid = ""),
        )

        private val applovin = listOf(
            LineItem(demandId = "applovin", pricefloor = 0.01, adUnitId = "43619c29a94e0269", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 0.05, adUnitId = "ce6f29210ba4f0f1", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 0.1, adUnitId = "46737e007e374aa8", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 0.2, adUnitId = "12ad4228f6acf6d0", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 0.3, adUnitId = "8d59ec67c8851e4b", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 0.5, adUnitId = "da99849a06674bb6", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 0.7, adUnitId = "ae61fbd91eef9699", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 1.0, adUnitId = "975f2dcb3e4465fe", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 1.5, adUnitId = "c2a6f185d5ff6213", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 2.0, adUnitId = "a2ee7eb2294a0790", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 3.0, adUnitId = "59f1125269b818f7", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 4.0, adUnitId = "f15ca10b889ec5e1", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 5.0, adUnitId = "063324ab333adc7e", uid = ""),
            LineItem(demandId = "applovin", pricefloor = 7.0, adUnitId = "9ff21515af0cabdc", uid = ""),
        )

        private val yandex = listOf(
            LineItem(demandId = "yandex", pricefloor = 0.01, adUnitId = "R-M-3032723-1", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 0.05, adUnitId = "R-M-3032723-3", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 0.1, adUnitId = "R-M-3032723-4", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 0.2, adUnitId = "R-M-3032723-5", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 0.3, adUnitId = "R-M-3032723-6", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 0.5, adUnitId = "R-M-3032723-7", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 0.7, adUnitId = "R-M-3032723-8", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 1.0, adUnitId = "R-M-3032723-9", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 1.5, adUnitId = "R-M-3032723-10", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 2.0, adUnitId = "R-M-3032723-11", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 3.0, adUnitId = "R-M-3032723-12", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 4.0, adUnitId = "R-M-3032723-13", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 5.0, adUnitId = "R-M-3032723-14", uid = ""),
            LineItem(demandId = "yandex", pricefloor = 7.0, adUnitId = "R-M-3032723-15", uid = ""),
        )
    }
}
