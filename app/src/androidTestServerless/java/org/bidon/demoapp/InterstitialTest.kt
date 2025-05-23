package org.bidon.demoapp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import org.bidon.demoapp.theme.AppTheme
import org.bidon.demoapp.ui.InterstitialScreen
import org.bidon.sdk.auction.impl.ServerlessAuctionConfig
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.impl.ServerlessConfigSettings
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Rule
import org.junit.Test

/**
 * Created by Bidon Team on 03/03/2023.
 */
class InterstitialTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun interstitial_OneRoundAdmob() {
        ServerlessConfigSettings.useAdapters("admob")
        ServerlessAuctionConfig.setLocalAuctionResponse(
            pricefloor = 0.0,
            adUnits = listOf(
                AdUnit(
                    demandId = "admob",
                    label = "admob",
                    pricefloor = 0.01,
                    uid = "uid123",
                    bidType = BidType.CPM,
                    timeout = 5000,
                    ext = jsonObject {
                        "adUnitId" hasValue "ca-app-pub-3940256099942544/1033173712"
                    }.toString()
                )
            )
        )
        rule.setContent {
            AppTheme {
                InterstitialScreen(navController = rememberNavController())
            }
        }
        with(rule) {
            StepSdkInitialization.perform(activity)
            clickOnComposeButton("LOAD")
            checkTextOnScreen("ROUND_1")
            checkTextOnScreen("WINNER")

            Thread.sleep(1000)
            clickOnComposeButton("SHOW")

            Thread.sleep(1000)
            clickOnXmlButton(buttonDescription = AdmobInterstitialCloseButtonDescription)

            checkTextOnScreen("onRevenuePaid")
            checkTextOnScreen("onAdShown")
            checkTextOnScreen("onAdClosed")
        }
    }

    @Test
    fun interstitial_NoAppropriateAdUnitAdmob() {
        ServerlessConfigSettings.useAdapters("admob")
        ServerlessAuctionConfig.setLocalAuctionResponse(
            pricefloor = 1.0,
            adUnits = listOf(
                AdUnit(
                    demandId = "admob",
                    label = "admob",
                    pricefloor = 0.01,
                    uid = "uid123",
                    bidType = BidType.CPM,
                    timeout = 5000,
                    ext = jsonObject {
                        "adUnitId" hasValue "ca-app-pub-3940256099942544/1033173712"
                    }.toString()
                )
            )
        )
        rule.setContent {
            AppTheme {
                InterstitialScreen(navController = rememberNavController())
            }
        }
        with(rule) {
            StepSdkInitialization.perform(activity)
            clickOnComposeButton("LOAD")
            checkTextOnScreen("auctionStarted")
            checkTextOnScreen("RoundStarted")
            checkTextOnScreen("roundFailed")
            checkTextOnScreen("auctionFailed")
            checkTextOnScreen("onAdLoadFailed")

            clickOnComposeButton("SHOW")
            checkTextOnScreen("onAdShowFailed")
        }
    }

    @Test
    fun interstitial_OneRoundUnityAds() {
        ServerlessConfigSettings.useAdapters("unityads")
        ServerlessAuctionConfig.setLocalAuctionResponse(
            pricefloor = 0.0,
            adUnits = listOf(
                AdUnit(
                    demandId = "unityads",
                    label = "unityads",
                    pricefloor = 0.01,
                    uid = "uid123",
                    bidType = BidType.CPM,
                    timeout = 5000,
                    ext = jsonObject {
                        "placementId" hasValue "video"
                    }.toString()
                )
            )
        )
        rule.setContent {
            AppTheme {
                InterstitialScreen(navController = rememberNavController())
            }
        }
        with(rule) {
            StepSdkInitialization.perform(activity)
            clickOnComposeButton("LOAD")
            checkTextOnScreen("ROUND_1")
            checkTextOnScreen("WINNER")

            Thread.sleep(1000)
            clickOnComposeButton("SHOW")

            Thread.sleep(20000)
            clickBackButton()
            Thread.sleep(1000)
//            val title = "Unity Ads WebView"
//            onView(ViewMatchers.withText(title)).perform(ViewActions.pressBack())

            checkTextOnScreen("onRevenuePaid")
            checkTextOnScreen("onAdShown")
            checkTextOnScreen("onAdClosed")
        }
    }
}

private const val AdmobInterstitialCloseButtonDescription = "Interstitial close button"