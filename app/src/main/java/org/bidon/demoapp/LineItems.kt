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
        val all get() = bigoads // + unityAds + admob + applovin

        private val unityAds = listOf(
            LineItem(demandId = "unityads", pricefloor = 1.0, adUnitId = "unity_inter_1_"),
            LineItem(demandId = "unityads", pricefloor = 2.0, adUnitId = "unity_inter_2"),
            LineItem(demandId = "unityads", pricefloor = 5.0, adUnitId = "unity_inter_5"),
            LineItem(demandId = "unityads", pricefloor = 7.0, adUnitId = "unity_inter_7"),
            LineItem(demandId = "unityads", pricefloor = 10.0, adUnitId = "unity_inter_10"),
            LineItem(demandId = "unityads", pricefloor = 15.0, adUnitId = "unity_inter_15"),
            LineItem(demandId = "unityads", pricefloor = 20.0, adUnitId = "unity_inter_20"),
            LineItem(demandId = "unityads", pricefloor = 25.0, adUnitId = "unity_inter_25"),
            LineItem(demandId = "unityads", pricefloor = 35.0, adUnitId = "unity_inter_35"),
            LineItem(demandId = "unityads", pricefloor = 50.0, adUnitId = "unity_inter_50"),
            LineItem(demandId = "unityads", pricefloor = 70.0, adUnitId = "unity_inter_70"),
            LineItem(demandId = "unityads", pricefloor = 95.0, adUnitId = "unity_inter_95")
        )

        private val admob = listOf(
            LineItem(demandId = "admob", pricefloor = 1.0, adUnitId = "ca-app-pub-9069030271740050/2719222246"),
            LineItem(demandId = "admob", pricefloor = 2.0, adUnitId = "ca-app-pub-9069030271740050/9682455534"),
            LineItem(demandId = "admob", pricefloor = 5.0, adUnitId = "ca-app-pub-9069030271740050/2527650551"),
            LineItem(demandId = "admob", pricefloor = 7.0, adUnitId = "ca-app-pub-9069030271740050/4250165457"),
            LineItem(demandId = "admob", pricefloor = 10.0, adUnitId = "ca-app-pub-9069030271740050/7371701555"),
            LineItem(demandId = "admob", pricefloor = 15.0, adUnitId = "ca-app-pub-9069030271740050/4553966523"),
            LineItem(demandId = "admob", pricefloor = 20.0, adUnitId = "ca-app-pub-9069030271740050/4579098822"),
            LineItem(demandId = "admob", pricefloor = 25.0, adUnitId = "ca-app-pub-9069030271740050/6796986482"),
            LineItem(demandId = "admob", pricefloor = 35.0, adUnitId = "ca-app-pub-9069030271740050/8326772146"),
            LineItem(demandId = "admob", pricefloor = 50.0, adUnitId = "ca-app-pub-9069030271740050/5292333129"),
            LineItem(demandId = "admob", pricefloor = 70.0, adUnitId = "ca-app-pub-9069030271740050/5009509512"),
            LineItem(demandId = "admob", pricefloor = 95.0, adUnitId = "ca-app-pub-9069030271740050/2091454718")
        )

        private val applovin = listOf(
            LineItem(demandId = "applovin", pricefloor = 1.0, adUnitId = "9dd72d785cbc17fe"),
            LineItem(demandId = "applovin", pricefloor = 2.0, adUnitId = "82f494dc84767bcd"),
            LineItem(demandId = "applovin", pricefloor = 5.0, adUnitId = "71095830df3679ad"),
            LineItem(demandId = "applovin", pricefloor = 7.0, adUnitId = "ad6f7fcab26463d5"),
            LineItem(demandId = "applovin", pricefloor = 10.0, adUnitId = "abdc2d3be8365b37"),
            LineItem(demandId = "applovin", pricefloor = 15.0, adUnitId = "5d2bb9df44c033b7"),
            LineItem(demandId = "applovin", pricefloor = 20.0, adUnitId = "5dc9a141c5241bf5"),
            LineItem(demandId = "applovin", pricefloor = 25.0, adUnitId = "230931c9ca69cfe1"),
            LineItem(demandId = "applovin", pricefloor = 35.0, adUnitId = "ca86b0231fe94b4d"),
            LineItem(demandId = "applovin", pricefloor = 50.0, adUnitId = "cd2281f8cfe4d5e7"),
            LineItem(demandId = "applovin", pricefloor = 70.0, adUnitId = "6ebb8e46b8984c08"),
            LineItem(demandId = "applovin", pricefloor = 95.0, adUnitId = "ec1e3701da66ef14")
        )

        private val bigoads = listOf(
            LineItem(demandId = "bigoads", pricefloor = 2.0, adUnitId = "10137724-10068211"),
            LineItem(demandId = "bigoads", pricefloor = 1.0, adUnitId = "10137724-10060400"),
        )
    }

    object Banners {
        val all get() = unityAds + admob + applovin

        private val unityAds = listOf(
            LineItem(demandId = "unityads", pricefloor = 0.01, adUnitId = "unity_banner_0_01"),
            LineItem(demandId = "unityads", pricefloor = 0.05, adUnitId = "unity_banner_0_05"),
            LineItem(demandId = "unityads", pricefloor = 0.1, adUnitId = "unity_banner_0_1"),
            LineItem(demandId = "unityads", pricefloor = 0.2, adUnitId = "unity_banner_0_2"),
            LineItem(demandId = "unityads", pricefloor = 0.3, adUnitId = "unity_banner_0_3"),
            LineItem(demandId = "unityads", pricefloor = 0.5, adUnitId = "unity_banner_0_5"),
            LineItem(demandId = "unityads", pricefloor = 0.7, adUnitId = "unity_banner_0_7"),
            LineItem(demandId = "unityads", pricefloor = 1.0, adUnitId = "unity_banner_1_0"),
            LineItem(demandId = "unityads", pricefloor = 1.5, adUnitId = "unity_banner_1_5"),
            LineItem(demandId = "unityads", pricefloor = 2.0, adUnitId = "unity_banner_2"),
            LineItem(demandId = "unityads", pricefloor = 3.0, adUnitId = "unity_banner_3"),
            LineItem(demandId = "unityads", pricefloor = 4.0, adUnitId = "unity_banner_4")
        )
        private val admob = listOf(
            LineItem(demandId = "admob", pricefloor = 0.01, adUnitId = "ca-app-pub-9069030271740050/3238096991"),
            LineItem(demandId = "admob", pricefloor = 0.05, adUnitId = "ca-app-pub-9069030271740050/1636444558"),
            LineItem(demandId = "admob", pricefloor = 0.1, adUnitId = "ca-app-pub-9069030271740050/1444872866"),
            LineItem(demandId = "admob", pricefloor = 0.2, adUnitId = "ca-app-pub-9069030271740050/7874549840"),
            LineItem(demandId = "admob", pricefloor = 0.3, adUnitId = "ca-app-pub-9069030271740050/3743733147"),
            LineItem(demandId = "admob", pricefloor = 0.5, adUnitId = "ca-app-pub-9069030271740050/4426259429"),
            LineItem(demandId = "admob", pricefloor = 0.7, adUnitId = "ca-app-pub-9069030271740050/7982361057"),
            LineItem(demandId = "admob", pricefloor = 1.0, adUnitId = "ca-app-pub-9069030271740050/1416952700"),
            LineItem(demandId = "admob", pricefloor = 1.5, adUnitId = "ca-app-pub-9069030271740050/6477707698"),
            LineItem(demandId = "admob", pricefloor = 2.0, adUnitId = "ca-app-pub-9069030271740050/9351283040"),
            LineItem(demandId = "admob", pricefloor = 3.0, adUnitId = "ca-app-pub-9069030271740050/6286136008"),
            LineItem(demandId = "admob", pricefloor = 4.0, adUnitId = "ca-app-pub-9069030271740050/3659972663"),
            LineItem(demandId = "admob", pricefloor = 5.0, adUnitId = "ca-app-pub-9069030271740050/1281221331"),
            LineItem(demandId = "admob", pricefloor = 7.0, adUnitId = "ca-app-pub-9069030271740050/9842237630")
        )

        private val applovin = listOf(
            LineItem(demandId = "applovin", pricefloor = 0.01, adUnitId = "43619c29a94e0269"),
            LineItem(demandId = "applovin", pricefloor = 0.05, adUnitId = "ce6f29210ba4f0f1"),
            LineItem(demandId = "applovin", pricefloor = 0.1, adUnitId = "46737e007e374aa8"),
            LineItem(demandId = "applovin", pricefloor = 0.2, adUnitId = "12ad4228f6acf6d0"),
            LineItem(demandId = "applovin", pricefloor = 0.3, adUnitId = "8d59ec67c8851e4b"),
            LineItem(demandId = "applovin", pricefloor = 0.5, adUnitId = "da99849a06674bb6"),
            LineItem(demandId = "applovin", pricefloor = 0.7, adUnitId = "ae61fbd91eef9699"),
            LineItem(demandId = "applovin", pricefloor = 1.0, adUnitId = "975f2dcb3e4465fe"),
            LineItem(demandId = "applovin", pricefloor = 1.5, adUnitId = "c2a6f185d5ff6213"),
            LineItem(demandId = "applovin", pricefloor = 2.0, adUnitId = "a2ee7eb2294a0790"),
            LineItem(demandId = "applovin", pricefloor = 3.0, adUnitId = "59f1125269b818f7"),
            LineItem(demandId = "applovin", pricefloor = 4.0, adUnitId = "f15ca10b889ec5e1"),
            LineItem(demandId = "applovin", pricefloor = 5.0, adUnitId = "063324ab333adc7e"),
            LineItem(demandId = "applovin", pricefloor = 7.0, adUnitId = "9ff21515af0cabdc")
        )
    }
}
