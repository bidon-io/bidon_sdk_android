# Release 0.4.29.2 (2024.04.10)
## Features:
- [APDM-195](https://appodeal.atlassian.net/browse/APDM-195) Update Amazon to 9.9.3
- [APDM-195](https://appodeal.atlassian.net/browse/APDM-195) Update Applovin to 12.4.2
- [APDM-195](https://appodeal.atlassian.net/browse/APDM-195) Update Bidmachine to 2.6.0
- [APDM-195](https://appodeal.atlassian.net/browse/APDM-195) Update BigoAds to 4.6.1
- [APDM-195](https://appodeal.atlassian.net/browse/APDM-195) Update Meta to 6.17.0
- [APDM-195](https://appodeal.atlassian.net/browse/APDM-195) Update Mintegral to 16.6.71
- [APDM-195](https://appodeal.atlassian.net/browse/APDM-195) Update Inmobi to 7.3.1
- [APDM-195](https://appodeal.atlassian.net/browse/APDM-195) Update UnityAds to 4.10.0
- 
# Release 0.4.29.1 (2024.03.27)
## Features:
- [BDN-616](https://appodeal.atlassian.net/browse/BDN-616) Update Applovin to 12.3.1
- [BDN-617](https://appodeal.atlassian.net/browse/BDN-617) Update Admob to 23.0.0
- [BDN-618](https://appodeal.atlassian.net/browse/BDN-618) Update Bidmachine to 2.5.2
- [BDN-619](https://appodeal.atlassian.net/browse/BDN-619) Update DTExchange to 8.2.6
- [BDN-620](https://appodeal.atlassian.net/browse/BDN-620) Update Inmobi to 10.6.7
- [BDN-621](https://appodeal.atlassian.net/browse/BDN-621) Update UnityAds to 4.9.3

# Release 0.4.29 (2024.02.14)
## Features:
- [BDN-615](https://appodeal.atlassian.net/browse/BDN-615) Update BigoAds to 4.5.1

# Release 0.4.28 (2024.01.15)
## Features:
- [BDN-612](https://appodeal.atlassian.net/browse/BDN-612) Move impression tracking to onAdClosed for UnityAds Interstitial

## Bugfixes:
- [BDN-598](https://appodeal.atlassian.net/browse/BDN-598) Lost request to BM with notification of winnings

# Release 0.4.27 (2024.01.05)
## Features:
- [BDN-609](https://appodeal.atlassian.net/browse/BDN-609) Remove Admob dependency

# Release 0.4.26 (2023.12.04)
## Features:
- [BDN-555](https://appodeal.atlassian.net/browse/BDN-555) Concurrent ad network initalization optimisation Android
- [BDN-511](https://appodeal.atlassian.net/browse/BDN-511) Upgrade Vungle adapter
- [BDN-481](https://appodeal.atlassian.net/browse/BDN-481) Integrate Google Ad Manager
- [BDN-578](https://appodeal.atlassian.net/browse/BDN-578) Add AdObject.Format to Stats.Result
- [BDN-580](https://appodeal.atlassian.net/browse/BDN-580) Pass Empty Mediation Config To Bidmachine When It Works In CPM Model. Android

# Release 0.4.24 (2023.11.23)
## Features:
- [BDN-550](https://appodeal.atlassian.net/browse/BDN-550) Skip some checks from VisibilityTracker

# Release 0.4.23 (2023.11.09)
## Features:
- [BDN-510](https://appodeal.atlassian.net/browse/BDN-510) Upgrade adapter versions: Mintegral SDK - 16.5.41, Inmobi SDK - 10.6.1, UnityAds SDK - 4.9.2.
- [BDN-513](https://appodeal.atlassian.net/browse/BDN-513) Remove tracker on Detach. Fixed Adaptive banner logic.

# Release 0.4.21 (2023.10.20)
## Features:
- [BDN-446](https://appodeal.atlassian.net/browse/BDN-446) Implement Amazon Rewarded
- [BDN-417](https://appodeal.atlassian.net/browse/BDN-417) Implement Amazon Adapters: Interstitial+Banner

## Bugfixes:
- [BDN-453](https://appodeal.atlassian.net/browse/BDN-453) Applying Regulations GDPR/CCPA/COPPA
- [BDN-443](https://appodeal.atlassian.net/browse/BDN-434) Improving Ad process
- [BDN-433](https://appodeal.atlassian.net/browse/BDN-433) Memory Leak

# Release 0.4.20 (2023.09.21)
## New Features:
- [BDN-380](https://appodeal.atlassian.net/browse/BDN-380) Add Round Number To Bidon Analytics
- [BDN-379](https://appodeal.atlassian.net/browse/BDN-379) Separate Bidding And Non-Bidding Data In Bidon Analytics
- [BDN-390](https://appodeal.atlassian.net/browse/BDN-390) Replace Publicly Exposed Integer Auto-incremental Identifiers With Secure IDs
- [BDN-92](https://appodeal.atlassian.net/browse/BDN-92) Add InMobi Adapter

## Bugfixes:
- [BDN-382](https://appodeal.atlassian.net/browse/BDN-382) Fix stats of empty bidding rounds
- [BDN-413](https://appodeal.atlassian.net/browse/BDN-413) IabConsent fix

# Release 0.3.3 (2023.09.13)
## Bugfixes:
- [BDN-391](https://appodeal.atlassian.net/browse/BDN-391) Sync Adapter version between Appodeal vs Bidon


# Release 0.4.0 (2023.09.08)
## New Features:
- [BDN-377](https://appodeal.atlassian.net/browse/BDN-377) Add meditation_service to Meta Audience Adapter
- [BDN-191](https://appodeal.atlassian.net/browse/BDN-191) BannerManager + BannerPosition

## Bugfixes:
- [BDN-413](https://appodeal.atlassian.net/browse/BDN-413) Change IABTCF_gdprApplies type to Int

# Release 0.3.2 (2023.09.04)
## New Features:
- [BDN-362](https://appodeal.atlassian.net/browse/BDN-362) Turned off MobileAds.disableMediationAdapterInitialization() for AdMob

## Bugfixes:
- [BDN-355](https://appodeal.atlassian.net/browse/BDN-355) Double WIN-status stat with multi-rounds
- [BDN-354](https://appodeal.atlassian.net/browse/BDN-354) Fixed NO_FILL status

