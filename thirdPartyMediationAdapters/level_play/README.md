# Level Play Adapter for Bidon SDK

**Adapter Data:**

- **Network name:** Bidon
- **Network key:** `15c0a270d`

---
## Update Consent Value

To set **GDPR**, **CCPA**, and **COPPA** consent status for the Bidon network adapter, use the following keys:

##### Bidon network key:

`BIDON_CA_NETWORK_KEY = "15c0a270d"`

##### Consent Keys:
- `BIDON_GDPR_KEY = "BidonCA_GDPR"`
- `BIDON_CCPA_KEY = "BidonCA_CCPA"`
- `BIDON_COPPA_KEY = "BidonCA_COPPA"`

Then put consent `Boolean` value to json with `Bidon network key` and call:
```
IronSource.setNetworkData(BIDON_CA_NETWORK_KEY, networkData)
```

##### Example:
```kotlin
val networkData = JSONObject()
networkData.put(BIDON_GDPR_KEY, isUserHasGdprConsent)
networkData.put(BIDON_CCPA_KEY, isUserHasCcpaConsent)
networkData.put(BIDON_COPPA_KEY, isUserAgeRestricted)

IronSource.setNetworkData(BIDON_CA_NETWORK_KEY, networkData)
```
where:
- `isUserHasGdprConsent` — Boolean: user’s GDPR consent;
- `isUserHasCcpaConsent` — Boolean: user’s CCPA consent;
- `isUserAgeRestricted` — Boolean: user’s COPPA (age restriction) status.