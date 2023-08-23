/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.mediation.googlebiddingadapter

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Size
import android.view.View.GONE
import com.chartboost.heliumsdk.domain.AdFormat
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException
import com.chartboost.heliumsdk.domain.ChartboostMediationError
import com.chartboost.heliumsdk.domain.GdprConsentStatus
import com.chartboost.heliumsdk.domain.PartnerAd
import com.chartboost.heliumsdk.domain.PartnerAdListener
import com.chartboost.heliumsdk.domain.PartnerAdLoadRequest
import com.chartboost.heliumsdk.domain.PartnerAdapter
import com.chartboost.heliumsdk.domain.PartnerConfiguration
import com.chartboost.heliumsdk.domain.PreBidRequest
import com.chartboost.heliumsdk.utils.PartnerLogController
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.BIDDER_INFO_FETCH_FAILED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.BIDDER_INFO_FETCH_STARTED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.BIDDER_INFO_FETCH_SUCCEEDED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.CCPA_CONSENT_DENIED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.CCPA_CONSENT_GRANTED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.COPPA_NOT_SUBJECT
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.COPPA_SUBJECT
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.CUSTOM
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.DID_CLICK
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.DID_DISMISS
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.DID_REWARD
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.DID_TRACK_IMPRESSION
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.GDPR_APPLICABLE
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.GDPR_CONSENT_DENIED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.GDPR_CONSENT_GRANTED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.GDPR_CONSENT_UNKNOWN
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.GDPR_NOT_APPLICABLE
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.GDPR_UNKNOWN
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.INVALIDATE_FAILED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.INVALIDATE_STARTED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.INVALIDATE_SUCCEEDED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.LOAD_FAILED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.LOAD_STARTED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.LOAD_SUCCEEDED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.SETUP_FAILED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.SETUP_STARTED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.SETUP_SUCCEEDED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.SHOW_FAILED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.SHOW_STARTED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.SHOW_SUCCEEDED
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.AdapterStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.query.QueryInfo
import com.google.android.gms.ads.query.QueryInfoGenerationCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.asFailure
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GoogleBiddingAdapter : PartnerAdapter {
    companion object {
        /**
         * List containing device IDs to be set for enabling Google Bidding test ads. It can be populated at
         * any time and will take effect for the next ad request. Remember to empty this list or
         * stop setting it before releasing your app.
         */
        public var testDeviceIds = listOf<String>()
            set(value) {
                field = value
                PartnerLogController.log(
                    CUSTOM,
                    "Google Bidding test device ID(s) to be set: ${
                    if (value.isEmpty()) "none"
                    else value.joinToString()
                    }"
                )
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder().setTestDeviceIds(value).build()
                )
            }
    }

    init {
        logInfo("ChartboostMediation", "GoogleBiddingAdapter init")
    }

    /**
     * A map of Chartboost Mediation's listeners for the corresponding load identifier.
     */
    private val listeners = mutableMapOf<String, PartnerAdListener>()

    /**
     * Indicate whether GDPR currently applies to the user.
     */
    private var gdprApplies: Boolean? = null

    /**
     * Indicate whether the user has consented to allowing personalized ads when GDPR applies.
     */
    private var allowPersonalizedAds = false

    /**
     * Indicate whether the user has given consent per CCPA.
     */
    private var ccpaPrivacyString: String? = null

    /**
     * Get the Google Mobile Ads SDK version.
     *
     * Note that the version string will be in the format of afma-sdk-a-v221908999.214106000.1.
     */
    override val partnerSdkVersion: String
        get() = MobileAds.getVersion().toString()

    /**
     * Get the Google Bidding adapter version.
     *
     * You may version the adapter using any preferred convention, but it is recommended to apply the
     * following format if the adapter will be published by Chartboost Mediation:
     *
     * Chartboost Mediation.Partner.Adapter
     *
     * "Chartboost Mediation" represents the Chartboost Mediation SDK’s major version that is compatible with this adapter. This must be 1 digit.
     * "Partner" represents the partner SDK’s major.minor.patch.x (where x is optional) version that is compatible with this adapter. This can be 3-4 digits.
     * "Adapter" represents this adapter’s version (starting with 0), which resets to 0 when the partner SDK’s version changes. This must be 1 digit.
     */
    override val adapterVersion: String
        get() = "4.22.1.0.0" // BuildConfig.CHARTBOOST_MEDIATION_GOOGLE_BIDDING_ADAPTER_VERSION

    /**
     * Get the partner name for internal uses.
     */
    override val partnerId: String
        get() = "google_googlebidding"

    /**
     * Get the partner name for external uses.
     */
    override val partnerDisplayName: String
        get() = "Google Bidding"

    /**
     * Initialize the Google Mobile Ads SDK so that it is ready to request ads.
     *
     * @param context The current [Context].
     * @param partnerConfiguration Configuration object containing relevant data to initialize Google Bidding.
     */
    override suspend fun setUp(
        context: Context,
        partnerConfiguration: PartnerConfiguration
    ): Result<Unit> {
        PartnerLogController.log(SETUP_STARTED)
        // Since Chartboost Mediation is the mediator, no need to initialize Google Bidding's partner SDKs.
        // https://developers.google.com/android/reference/com/google/android/gms/ads/MobileAds?hl=en#disableMediationAdapterInitialization(android.content.Context)
        MobileAds.disableMediationAdapterInitialization(context)

        return suspendCoroutine { continuation ->
            MobileAds.initialize(context) { status ->
                continuation.resume(getInitResult(status.adapterStatusMap[MobileAds::class.java.name]))
            }
        }
    }

    suspend fun setUp(
        context: Context,
    ): Result<Unit> {
        PartnerLogController.log(SETUP_STARTED)
        // Since Chartboost Mediation is the mediator, no need to initialize Google Bidding's partner SDKs.
        // https://developers.google.com/android/reference/com/google/android/gms/ads/MobileAds?hl=en#disableMediationAdapterInitialization(android.content.Context)
        MobileAds.disableMediationAdapterInitialization(context)

        return suspendCoroutine { continuation ->
            MobileAds.initialize(context) { status ->
                continuation.resume(getInitResult(status.adapterStatusMap[MobileAds::class.java.name]))
            }
        }
    }

    /**
     * Notify the Google Mobile Ads SDK of the GDPR applicability and consent status.
     *
     * @param context The current [Context].
     * @param applies True if GDPR applies, false otherwise.
     * @param gdprConsentStatus The user's GDPR consent status.
     */
    override fun setGdpr(
        context: Context,
        applies: Boolean?,
        gdprConsentStatus: GdprConsentStatus
    ) {
        PartnerLogController.log(
            when (applies) {
                true -> GDPR_APPLICABLE
                false -> GDPR_NOT_APPLICABLE
                else -> GDPR_UNKNOWN
            }
        )

        PartnerLogController.log(
            when (gdprConsentStatus) {
                GdprConsentStatus.GDPR_CONSENT_UNKNOWN -> GDPR_CONSENT_UNKNOWN
                GdprConsentStatus.GDPR_CONSENT_GRANTED -> GDPR_CONSENT_GRANTED
                GdprConsentStatus.GDPR_CONSENT_DENIED -> GDPR_CONSENT_DENIED
            }
        )

        this.gdprApplies = applies

        if (applies == true) {
            allowPersonalizedAds = gdprConsentStatus == GdprConsentStatus.GDPR_CONSENT_GRANTED
        }
    }

    /**
     * Save the current CCPA privacy String to be used later.
     *
     * @param context The current [Context].
     * @param hasGrantedCcpaConsent True if the user has granted CCPA consent, false otherwise.
     * @param privacyString The CCPA privacy String.
     */
    override fun setCcpaConsent(
        context: Context,
        hasGrantedCcpaConsent: Boolean,
        privacyString: String
    ) {
        PartnerLogController.log(
            if (hasGrantedCcpaConsent) CCPA_CONSENT_GRANTED
            else CCPA_CONSENT_DENIED
        )

        ccpaPrivacyString = privacyString
    }

    /**
     * Notify Google Bidding of the COPPA subjectivity.
     *
     * @param context The current [Context].
     * @param isSubjectToCoppa True if the user is subject to COPPA, false otherwise.
     */
    override fun setUserSubjectToCoppa(context: Context, isSubjectToCoppa: Boolean) {
        PartnerLogController.log(
            if (isSubjectToCoppa) COPPA_SUBJECT
            else COPPA_NOT_SUBJECT
        )

        MobileAds.setRequestConfiguration(
            MobileAds.getRequestConfiguration().toBuilder()
                .setTagForChildDirectedTreatment(
                    if (isSubjectToCoppa) {
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                    } else {
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
                    }
                ).build()
        )
    }

    /**
     * Get a bid token if network bidding is supported.
     *
     * @param context The current [Context].
     * @param request The [PreBidRequest] instance containing relevant data for the current bid request.
     *
     * @return A Map of biddable token Strings.
     */
    override suspend fun fetchBidderInformation(
        context: Context,
        request: PreBidRequest
    ): Map<String, String> {
        PartnerLogController.log(BIDDER_INFO_FETCH_STARTED)

        // Google-defined specs for Chartboost Mediation
        val extras = Bundle()
//        extras.putString("query_info_type", "requester_type_2")

        val adRequest = AdRequest.Builder()
//            .setRequestAgent("Chartboost")
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()

        val adFormat = getGoogleBiddingAdFormat(request.format)

        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                QueryInfo.generate(
                    context,
                    adFormat,
                    adRequest,
                    object : QueryInfoGenerationCallback() {
                        override fun onSuccess(queryInfo: QueryInfo) {
                            PartnerLogController.log(BIDDER_INFO_FETCH_SUCCEEDED)
                            continuation.resumeWith(
                                Result.success(
                                    mapOf("token" to queryInfo.query)
                                )
                            )
                        }

                        override fun onFailure(error: String) {
                            PartnerLogController.log(BIDDER_INFO_FETCH_FAILED, error)
                            continuation.resumeWith(
                                Result.success(
                                    emptyMap()
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    /**
     * Attempt to load a Google Bidding ad.
     *
     * @param context The current [Context].
     * @param request An [PartnerAdLoadRequest] instance containing relevant data for the current ad load call.
     * @param partnerAdListener A [PartnerAdListener] to notify Chartboost Mediation of ad events.
     *
     * @return Result.success(PartnerAd) if the ad was successfully loaded, Result.failure(Exception) otherwise.
     */
    override suspend fun load(
        context: Context,
        request: PartnerAdLoadRequest,
        partnerAdListener: PartnerAdListener
    ): Result<PartnerAd> {
        PartnerLogController.log(LOAD_STARTED)

        logInfo("ChartboostMediation", "loadInterstitialAd $request")
        logInfo("ChartboostMediation", "${request.adm};;;")
        DeleteMe.setPayload(
            when (request.format) {
                AdFormat.INTERSTITIAL -> {
                    DeleteMe.AdType.Interstitial(request.adm ?: "no adm")
                }

                AdFormat.REWARDED -> {
                    DeleteMe.AdType.Rewarded(request.adm ?: "no adm")
                }

                AdFormat.BANNER -> {
                    DeleteMe.AdType.Banner(request.adm ?: "no adm")
                }
            }
        )
        return Throwable().asFailure()

        return when (request.format) {
            AdFormat.INTERSTITIAL -> loadInterstitialAd(
                context,
                request,
                partnerAdListener
            )

            AdFormat.REWARDED -> loadRewardedAd(
                context,
                request,
                partnerAdListener
            )

            AdFormat.BANNER -> loadBannerAd(
                context,
                request,
                partnerAdListener
            )

            else -> {
                if (request.format.key == "rewarded_interstitial") {
                    loadRewardedInterstitialAd(
                        context,
                        request,
                        partnerAdListener
                    )
                } else {
                    PartnerLogController.log(LOAD_FAILED)
                    Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_UNSUPPORTED_AD_FORMAT))
                }
            }
        }
    }

    /**
     * Attempt to show the currently loaded Google Bidding ad.
     *
     * @param context The current [Context]
     * @param partnerAd The [PartnerAd] object containing the Google Bidding ad to be shown.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(Exception) otherwise.
     */
    override suspend fun show(context: Context, partnerAd: PartnerAd): Result<PartnerAd> {
        PartnerLogController.log(SHOW_STARTED)
        val listener = listeners.remove(partnerAd.request.identifier)

        return when (partnerAd.request.format) {
            // Banner ads do not have a separate "show" mechanism.
            AdFormat.BANNER -> {
                PartnerLogController.log(SHOW_SUCCEEDED)
                Result.success(partnerAd)
            }

            AdFormat.INTERSTITIAL -> showInterstitialAd(context, partnerAd, listener)
            AdFormat.REWARDED -> showRewardedAd(context, partnerAd, listener)
            else -> {
                if (partnerAd.request.format.key == "rewarded_interstitial") {
                    showRewardedInterstitialAd(context, partnerAd, listener)
                } else {
                    PartnerLogController.log(SHOW_FAILED)
                    Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_SHOW_FAILURE_UNSUPPORTED_AD_FORMAT))
                }
            }
        }
    }

    /**
     * Discard unnecessary Google Bidding ad objects and release resources.
     *
     * @param partnerAd The [PartnerAd] object containing the Google Bidding ad to be discarded.
     *
     * @return Result.success(PartnerAd) if the ad was successfully discarded, Result.failure(Exception) otherwise.
     */
    override suspend fun invalidate(partnerAd: PartnerAd): Result<PartnerAd> {
        PartnerLogController.log(INVALIDATE_STARTED)
        listeners.remove(partnerAd.request.identifier)

        // Only invalidate banners as there are no explicit methods to invalidate the other formats.
        return when (partnerAd.request.format) {
            AdFormat.BANNER -> destroyBannerAd(partnerAd)
            else -> {
                PartnerLogController.log(INVALIDATE_SUCCEEDED)
                Result.success(partnerAd)
            }
        }
    }

    /**
     * Get a [Result] containing the initialization result of the Google Mobile Ads SDK.
     *
     * @param status The initialization status of the Google Mobile Ads SDK.
     *
     * @return A [Result] object containing details about the initialization result.
     */
    private fun getInitResult(status: AdapterStatus?): Result<Unit> {
        return status?.let { it ->
            if (it.initializationState == AdapterStatus.State.READY) {
                Result.success(PartnerLogController.log(SETUP_SUCCEEDED))
            } else {
                PartnerLogController.log(
                    SETUP_FAILED,
                    "Initialization state: ${it.initializationState}. Description: ${it.description}"
                )
                Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_INITIALIZATION_FAILURE_UNKNOWN))
            }
        } ?: run {
            PartnerLogController.log(SETUP_FAILED, "Initialization status is null.")
            Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_INITIALIZATION_FAILURE_UNKNOWN))
        }
    }

    /**
     * Attempt to load a Google Bidding banner on the main thread.
     *
     * @param context The current [Context].
     * @param request An [PartnerAdLoadRequest] instance containing relevant data for the current ad load call.
     * @param listener A [PartnerAdListener] to notify Chartboost Mediation of ad events.
     */
    private suspend fun loadBannerAd(
        context: Context,
        request: PartnerAdLoadRequest,
        listener: PartnerAdListener
    ): Result<PartnerAd> {
        return suspendCoroutine { continuation ->
            CoroutineScope(Main).launch {
                val adm = request.adm ?: run {
                    PartnerLogController.log(
                        LOAD_FAILED,
                        ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_MARKUP.cause
                    )
                    continuation.resumeWith(
                        Result.failure(
                            ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_MARKUP)
                        )
                    )
                    return@launch
                }

                val bannerAd = AdView(context)
                val partnerAd = PartnerAd(
                    ad = bannerAd,
                    details = emptyMap(),
                    request = request,
                )

                bannerAd.setAdSize(getGoogleBiddingAdSize(request.size))
                bannerAd.adUnitId = request.partnerPlacement
                bannerAd.loadAd(buildRequest(adm))
                bannerAd.adListener = object : AdListener() {
                    override fun onAdImpression() {
                        PartnerLogController.log(DID_TRACK_IMPRESSION)
                        listener.onPartnerAdImpression(partnerAd)
                    }

                    override fun onAdLoaded() {
                        PartnerLogController.log(LOAD_SUCCEEDED)
                        continuation.resume(Result.success(partnerAd))
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        PartnerLogController.log(LOAD_FAILED, adError.message)
                        continuation.resume(
                            Result.failure(
                                ChartboostMediationAdException(
                                    getChartboostMediationError(adError.code)
                                )
                            )
                        )
                    }

                    override fun onAdOpened() {
                        // NO-OP
                    }

                    override fun onAdClicked() {
                        PartnerLogController.log(DID_CLICK)
                        listener.onPartnerAdClicked(partnerAd)
                    }

                    override fun onAdClosed() {
                        // NO-OP. Ignore banner closes to help avoid auto-refresh issues.
                    }
                }
            }
        }
    }

    /**
     * Find the most appropriate Google Bidding ad size for the given screen area based on height.
     *
     * @param size The [Size] to parse for conversion.
     *
     * @return The Google Bidding ad size that best matches the given [Size].
     */
    private fun getGoogleBiddingAdSize(size: Size?): AdSize {
        return size?.height?.let {
            when {
                it in 50 until 90 -> AdSize.BANNER
                it in 90 until 250 -> AdSize.LEADERBOARD
                it >= 250 -> AdSize.MEDIUM_RECTANGLE
                else -> AdSize.BANNER
            }
        } ?: AdSize.BANNER
    }

    /**
     * Attempt to load a Google Bidding interstitial on the main thread.
     *
     * @param context The current [Context].
     * @param request An [PartnerAdLoadRequest] instance containing data to load the ad with.
     * @param listener A [PartnerAdListener] to notify Chartboost Mediation of ad events.
     *
     * @return Result.success(PartnerAd) if the ad was successfully loaded, Result.failure(Exception) otherwise.
     */
    private suspend fun loadInterstitialAd(
        context: Context,
        request: PartnerAdLoadRequest,
        listener: PartnerAdListener
    ): Result<PartnerAd> {
        // Save the listener for later use.
        listeners[request.identifier] = listener

        return suspendCoroutine { continuation ->
            CoroutineScope(Main).launch {
                val adm = request.adm ?: run {
                    PartnerLogController.log(
                        LOAD_FAILED,
                        ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_MARKUP.cause
                    )
                    continuation.resumeWith(
                        Result.failure(
                            ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_MARKUP)
                        )
                    )
                    return@launch
                }

                InterstitialAd.load(
                    context,
                    request.partnerPlacement,
                    buildRequest(adm),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            PartnerLogController.log(LOAD_SUCCEEDED)
                            continuation.resume(
                                Result.success(
                                    PartnerAd(
                                        ad = interstitialAd,
                                        details = emptyMap(),
                                        request = request
                                    )
                                )
                            )
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            PartnerLogController.log(LOAD_FAILED, loadAdError.message)
                            continuation.resume(
                                Result.failure(
                                    ChartboostMediationAdException(
                                        getChartboostMediationError(loadAdError.code)
                                    )
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    /**
     * Attempt to load a Google Bidding rewarded ad on the main thread.
     *
     * @param context The current [Context].
     * @param request The [PartnerAdLoadRequest] containing relevant data for the current ad load call.
     * @param listener A [PartnerAdListener] to notify Chartboost Mediation of ad events.
     *
     * @return Result.success(PartnerAd) if the ad was successfully loaded, Result.failure(Exception) otherwise.
     */
    private suspend fun loadRewardedAd(
        context: Context,
        request: PartnerAdLoadRequest,
        listener: PartnerAdListener
    ): Result<PartnerAd> {
        // Save the listener for later use.
        listeners[request.identifier] = listener

        return suspendCoroutine { continuation ->
            CoroutineScope(Main).launch {
                val adm = request.adm ?: run {
                    PartnerLogController.log(
                        LOAD_FAILED,
                        ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_MARKUP.cause
                    )
                    continuation.resumeWith(
                        Result.failure(
                            ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_MARKUP)
                        )
                    )
                    return@launch
                }

                RewardedAd.load(
                    context,
                    request.partnerPlacement,
                    buildRequest(adm),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(rewardedAd: RewardedAd) {
                            PartnerLogController.log(LOAD_SUCCEEDED)
                            continuation.resume(
                                Result.success(
                                    PartnerAd(
                                        ad = rewardedAd,
                                        details = emptyMap(),
                                        request = request
                                    )
                                )
                            )
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            PartnerLogController.log(LOAD_FAILED, loadAdError.message)
                            continuation.resume(
                                Result.failure(
                                    ChartboostMediationAdException(
                                        getChartboostMediationError(
                                            loadAdError.code
                                        )
                                    )
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    /**
     * Attempt to load a Google Bidding rewarded interstitial ad.
     *
     * @param context The current [Context].
     * @param request The [PartnerAdLoadRequest] containing relevant data for the current ad load call.
     * @param listener A [PartnerAdListener] to notify Chartboost Mediation of ad events.
     *
     * @return Result.success(PartnerAd) if the ad was successfully loaded, Result.failure(Exception) otherwise.
     */
    private suspend fun loadRewardedInterstitialAd(
        context: Context,
        request: PartnerAdLoadRequest,
        listener: PartnerAdListener
    ): Result<PartnerAd> {
        // Save the listener for later use.
        logInfo("ChartBoo", "Chartboost bidding adapter setup success")

        listeners[request.identifier] = listener

        return suspendCoroutine { continuation ->
            CoroutineScope(Main).launch {
                val adm = request.adm ?: run {
                    PartnerLogController.log(
                        LOAD_FAILED,
                        ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_MARKUP.cause
                    )
                    continuation.resumeWith(
                        Result.failure(
                            ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_MARKUP)
                        )
                    )
                    return@launch
                }

                RewardedInterstitialAd.load(
                    context,
                    request.partnerPlacement,
                    buildRequest(adm),
                    object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(rewardedInterstitialAd: RewardedInterstitialAd) {
                            PartnerLogController.log(LOAD_SUCCEEDED)
                            continuation.resume(
                                Result.success(
                                    PartnerAd(
                                        ad = rewardedInterstitialAd,
                                        details = emptyMap(),
                                        request = request
                                    )
                                )
                            )
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            PartnerLogController.log(LOAD_FAILED, loadAdError.message)
                            continuation.resume(
                                Result.failure(
                                    ChartboostMediationAdException(
                                        getChartboostMediationError(
                                            loadAdError.code
                                        )
                                    )
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    /**
     * Attempt to show a Google Bidding interstitial ad on the main thread.
     *
     * @param context The current [Context].
     * @param partnerAd The [PartnerAd] object containing the Google Bidding ad to be shown.
     * @param listener The [PartnerAdListener] to be notified of ad events.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(Exception) otherwise.
     */
    private suspend fun showInterstitialAd(
        context: Context,
        partnerAd: PartnerAd,
        listener: PartnerAdListener?
    ): Result<PartnerAd> {
        if (context !is Activity) {
            PartnerLogController.log(SHOW_FAILED, "Context is not an Activity.")
            return Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_SHOW_FAILURE_ACTIVITY_NOT_FOUND))
        }

        return suspendCoroutine { continuation ->
            partnerAd.ad?.let { ad ->
                CoroutineScope(Main).launch {
                    val interstitialAd = ad as InterstitialAd
                    interstitialAd.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdImpression() {
                                PartnerLogController.log(DID_TRACK_IMPRESSION)
                                listener?.onPartnerAdImpression(partnerAd)
                                    ?: PartnerLogController.log(
                                        CUSTOM,
                                        "Unable to fire onPartnerAdImpression for Google Bidding adapter."
                                    )
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                PartnerLogController.log(SHOW_FAILED, adError.message)
                                continuation.resume(
                                    Result.failure(
                                        ChartboostMediationAdException(
                                            getChartboostMediationError(adError.code)
                                        )
                                    )
                                )
                            }

                            override fun onAdShowedFullScreenContent() {
                                PartnerLogController.log(SHOW_SUCCEEDED)
                                continuation.resume(Result.success(partnerAd))
                            }

                            override fun onAdClicked() {
                                PartnerLogController.log(DID_CLICK)
                                listener?.onPartnerAdClicked(partnerAd)
                                    ?: PartnerLogController.log(
                                        CUSTOM,
                                        "Unable to fire onPartnerAdClicked for Google Bidding adapter."
                                    )
                            }

                            override fun onAdDismissedFullScreenContent() {
                                PartnerLogController.log(DID_DISMISS)
                                listener?.onPartnerAdDismissed(partnerAd, null)
                                    ?: PartnerLogController.log(
                                        CUSTOM,
                                        "Unable to fire onPartnerAdDismissed for Google Bidding adapter."
                                    )
                            }
                        }
                    interstitialAd.show(context)
                }
            } ?: run {
                PartnerLogController.log(SHOW_FAILED, "Ad is null.")
                continuation.resume(
                    Result.failure(
                        ChartboostMediationAdException(
                            ChartboostMediationError.CM_SHOW_FAILURE_AD_NOT_FOUND
                        )
                    )
                )
            }
        }
    }

    /**
     * Attempt to show a Google Bidding rewarded ad on the main thread.
     *
     * @param context The current [Context].
     * @param partnerAd The [PartnerAd] object containing the Google Bidding ad to be shown.
     * @param listener A [PartnerAdListener] to notify Chartboost Mediation of ad events.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(Exception) otherwise.
     */
    private suspend fun showRewardedAd(
        context: Context,
        partnerAd: PartnerAd,
        listener: PartnerAdListener?
    ): Result<PartnerAd> {
        if (context !is Activity) {
            PartnerLogController.log(SHOW_FAILED, "Context is not an Activity.")
            return Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_SHOW_FAILURE_ACTIVITY_NOT_FOUND))
        }

        return suspendCoroutine { continuation ->
            partnerAd.ad?.let { ad ->
                CoroutineScope(Main).launch {
                    val rewardedAd = ad as RewardedAd
                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdImpression() {
                            PartnerLogController.log(DID_TRACK_IMPRESSION)
                            listener?.onPartnerAdImpression(partnerAd) ?: PartnerLogController.log(
                                CUSTOM,
                                "Unable to fire onPartnerAdImpression for Google Bidding adapter."
                            )
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            PartnerLogController.log(SHOW_FAILED, adError.message)
                            continuation.resume(
                                Result.failure(
                                    ChartboostMediationAdException(
                                        getChartboostMediationError(adError.code)
                                    )
                                )
                            )
                        }

                        override fun onAdShowedFullScreenContent() {
                            PartnerLogController.log(SHOW_SUCCEEDED)
                            continuation.resume(Result.success(partnerAd))
                        }

                        override fun onAdClicked() {
                            PartnerLogController.log(DID_CLICK)
                            listener?.onPartnerAdClicked(partnerAd)
                                ?: PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to fire onPartnerAdClicked for Google Bidding adapter."
                                )
                        }

                        override fun onAdDismissedFullScreenContent() {
                            PartnerLogController.log(DID_DISMISS)
                            listener?.onPartnerAdDismissed(partnerAd, null)
                                ?: PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to fire onPartnerAdDismissed for Google Bidding adapter."
                                )
                        }
                    }

                    rewardedAd.show(context) {
                        PartnerLogController.log(DID_REWARD)
                        listener?.onPartnerAdRewarded(partnerAd)
                            ?: PartnerLogController.log(
                                CUSTOM,
                                "Unable to fire onPartnerAdRewarded for Google Bidding adapter."
                            )
                    }
                }
            } ?: run {
                PartnerLogController.log(SHOW_FAILED, "Ad is null.")
                continuation.resume(
                    Result.failure(
                        ChartboostMediationAdException(
                            ChartboostMediationError.CM_SHOW_FAILURE_AD_NOT_FOUND
                        )
                    )
                )
            }
        }
    }

    /**
     * Attempt to show a Google Bidding rewarded interstitial ad on the main thread.
     *
     * @param context The current [Context].
     * @param partnerAd The [PartnerAd] object containing the Google Bidding ad to be shown.
     * @param listener A [PartnerAdListener] to notify Chartboost Mediation of ad events.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(Exception) otherwise.
     */
    private suspend fun showRewardedInterstitialAd(
        context: Context,
        partnerAd: PartnerAd,
        listener: PartnerAdListener?
    ): Result<PartnerAd> {
        if (context !is Activity) {
            PartnerLogController.log(SHOW_FAILED, "Context is not an Activity.")
            return Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_SHOW_FAILURE_ACTIVITY_NOT_FOUND))
        }

        return suspendCoroutine { continuation ->
            partnerAd.ad?.let { ad ->
                CoroutineScope(Main).launch {
                    val rewardedInterstitialAd = ad as RewardedInterstitialAd
                    rewardedInterstitialAd.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdImpression() {
                                PartnerLogController.log(DID_TRACK_IMPRESSION)
                                listener?.onPartnerAdImpression(partnerAd)
                                    ?: PartnerLogController.log(
                                        CUSTOM,
                                        "Unable to fire onPartnerAdImpression for Google Bidding adapter."
                                    )
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                PartnerLogController.log(SHOW_FAILED, adError.message)
                                continuation.resume(
                                    Result.failure(
                                        ChartboostMediationAdException(
                                            getChartboostMediationError(adError.code)
                                        )
                                    )
                                )
                            }

                            override fun onAdShowedFullScreenContent() {
                                PartnerLogController.log(SHOW_SUCCEEDED)
                                continuation.resume(Result.success(partnerAd))
                            }

                            override fun onAdClicked() {
                                PartnerLogController.log(DID_CLICK)
                                listener?.onPartnerAdClicked(partnerAd)
                                    ?: PartnerLogController.log(
                                        CUSTOM,
                                        "Unable to fire onPartnerAdClicked for Google Bidding adapter."
                                    )
                            }

                            override fun onAdDismissedFullScreenContent() {
                                PartnerLogController.log(DID_DISMISS)
                                listener?.onPartnerAdDismissed(partnerAd, null)
                                    ?: PartnerLogController.log(
                                        CUSTOM,
                                        "Unable to fire onPartnerAdDismissed for Google Bidding adapter."
                                    )
                            }
                        }

                    rewardedInterstitialAd.show(context) {
                        PartnerLogController.log(DID_REWARD)
                        listener?.onPartnerAdRewarded(partnerAd)
                            ?: PartnerLogController.log(
                                CUSTOM,
                                "Unable to fire onPartnerAdRewarded for Google Bidding adapter."
                            )
                    }
                }
            }
        }
    }

    /**
     * Destroy the current Google Bidding banner ad.
     *
     * @param partnerAd The [PartnerAd] object containing the Google Bidding ad to be destroyed.
     *
     * @return Result.success(PartnerAd) if the ad was successfully destroyed, Result.failure(Exception) otherwise.
     */
    private fun destroyBannerAd(partnerAd: PartnerAd): Result<PartnerAd> {
        return partnerAd.ad?.let {
            if (it is AdView) {
                it.visibility = GONE
                it.destroy()

                PartnerLogController.log(INVALIDATE_SUCCEEDED)
                Result.success(partnerAd)
            } else {
                PartnerLogController.log(INVALIDATE_FAILED, "Ad is not an AdView.")
                Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_INVALIDATE_FAILURE_WRONG_RESOURCE_TYPE))
            }
        } ?: run {
            PartnerLogController.log(INVALIDATE_FAILED, "Ad is null.")
            Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_INVALIDATE_FAILURE_AD_NOT_FOUND))
        }
    }

    /**
     * Get the equivalent Google Bidding ad format for a given Chartboost Mediation [AdFormat].
     *
     * @param format The Chartboost Mediation [AdFormat] to convert.
     *
     * @return The equivalent Google Bidding ad format.
     */
    private fun getGoogleBiddingAdFormat(format: AdFormat) = when (format) {
        AdFormat.BANNER -> com.google.android.gms.ads.AdFormat.BANNER
        AdFormat.INTERSTITIAL -> com.google.android.gms.ads.AdFormat.INTERSTITIAL
        AdFormat.REWARDED -> com.google.android.gms.ads.AdFormat.REWARDED
        else -> com.google.android.gms.ads.AdFormat.BANNER
    }

    /**
     * Build a Google Bidding ad request.
     *
     * @param adm The ad string to be used in the ad request.
     *
     * @return A Google Bidding [AdRequest] object.
     */
    private fun buildRequest(adm: String) =
        AdRequest.Builder()
            .setRequestAgent("Chartboost")
            .addNetworkExtrasBundle(AdMobAdapter::class.java, buildPrivacyConsents())
            .setAdString(adm)
            .build()

    /**
     * Build a [Bundle] containing privacy settings for the current ad request for Google Bidding.
     *
     * @return A [Bundle] containing privacy settings for the current ad request for Google Bidding.
     */
    private fun buildPrivacyConsents(): Bundle {
        return Bundle().apply {
            if (gdprApplies == true && !allowPersonalizedAds) {
                putString("npa", "1")
            }

            if (!TextUtils.isEmpty(ccpaPrivacyString)) {
                putString("IABUSPrivacy_String", ccpaPrivacyString)
            }
        }
    }

    /**
     * Convert a given Google Bidding error code into a [ChartboostMediationError].
     *
     * @param error The Google Bidding error code as an [Int].
     *
     * @return The corresponding [ChartboostMediationError].
     */
    private fun getChartboostMediationError(error: Int) = when (error) {
        AdRequest.ERROR_CODE_APP_ID_MISSING -> ChartboostMediationError.CM_LOAD_FAILURE_PARTNER_NOT_INITIALIZED
        AdRequest.ERROR_CODE_INTERNAL_ERROR -> ChartboostMediationError.CM_INTERNAL_ERROR
        AdRequest.ERROR_CODE_INVALID_AD_STRING -> ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_MARKUP
        AdRequest.ERROR_CODE_INVALID_REQUEST, AdRequest.ERROR_CODE_REQUEST_ID_MISMATCH -> ChartboostMediationError.CM_LOAD_FAILURE_INVALID_AD_REQUEST
        AdRequest.ERROR_CODE_NETWORK_ERROR -> ChartboostMediationError.CM_NO_CONNECTIVITY
        AdRequest.ERROR_CODE_NO_FILL -> ChartboostMediationError.CM_LOAD_FAILURE_NO_FILL
        else -> ChartboostMediationError.CM_PARTNER_ERROR
    }
}