---
title: Request User Consent
sidebar_label: Request Consent
slug: /cordova/consent
---

## Installation

```shell
cordova plugin add cordova-plugin-consent \
  --variable UMP_VERSION=4.0.0 \
  --variable UMP_IOS_VERSION=3.1.0
```

The version variables are optional. Android defaults to UMP 4.0.0 and iOS
defaults to UMP 3.1.0.

## Prerequisites

- Android API level 23 or newer
- Cordova iOS 7.0.0 or newer
- Xcode 16.0 or newer for iOS builds
- iOS 12.0 or newer

Create and publish the required messages under **Privacy & messaging** in the
AdMob UI before requesting consent from the app.

## Usage

```js
document.addEventListener('deviceready', async () => {
  try {
    // Request fresh consent information on every app launch.
    await consent.requestInfoUpdate()

    // This resolves immediately when no form is required.
    await consent.loadAndShowIfRequired()
  } catch (error) {
    console.warn('Unable to update or gather consent:', error)
  }

  // Check even after an error because consent from a previous session may apply.
  if (await consent.canRequestAds()) {
    // Request ads.
  }
}, false)
```

`requestInfoUpdate()` must run before reading consent status or using the other
UMP APIs. Call `loadAndShowIfRequired()` after the update without checking
`ConsentStatus`; the SDK performs the required check internally.

On iOS, the recommended approach is to configure an IDFA message in the AdMob
UI and let UMP present the explanation and App Tracking Transparency prompt in
the correct order. If the app does not use a UMP IDFA message, request ATT only
after the UMP consent flow:

```js
if (cordova.platformId === 'ios') {
  const status = await consent.trackingAuthorizationStatus()
  if (status === 0) {
    await consent.requestTrackingAuthorization()
  }
}
```

## Forward consent

If a user has consented to receive only non-personalized ads, pass `npa="1"`
when creating the ad, e.g.

```js {3}
new admob.BannerAd({
  adUnitId: 'ca-app-pub-xxx/yyy',
  npa: '1',
})
```

The `npa` parameter is applicable to all ad formats, e.g.
[`BannerAd`](./api/classes/BannerAd),
[`InterstitialAd`](./api/classes/InterstitialAd),
[`RewardedAd`](./api/classes/RewardedAd), and
[`RewardedInterstitialAd`](./api/classes/RewardedInterstitialAd).

## References

- [TrackingAuthorizationStatus](./api/enumerations/TrackingAuthorizationStatus.md)
- [UMP SDK for Android](https://developers.google.com/admob/android/privacy)
- [UMP SDK for iOS](https://developers.google.com/admob/ios/privacy)
- [AppTrackingTransparency Framework](https://developer.apple.com/documentation/apptrackingtransparency)
