# [AdMob Plus Cordova](https://admob-plus.github.io)

[![NPM version][b1]][p0]
[![GitHub last commit][b2]][b2-l]
![Dependency status][b3]
[![Package Health][b4]][b4-l]
![Vulnerabilities][b5]
[![NPM license][b6]][b6-l]

AdMob Plus Cordova is the successor of [cordova-plugin-admob-free](https://github.com/ratson/cordova-plugin-admob-free), which provides a cleaner API and build with modern tools.

## Documentation

You can find the documentation [on the website](https://admob-plus.github.io/docs/cordova).

## Install this GitHub prerelease

The packaged prerelease includes the generated JavaScript files and the native
Android and iOS source files. It does not require pnpm or a local build.

Both application IDs are required. The SDK version variables below match this
prerelease's default versions and can be changed when a compatible SDK version
is required:

```shell
cordova plugin add "https://github.com/yjseo29/admob-plus/releases/download/admob-plus-cordova-v3.0.0-alpha.0/admob-plus-cordova-3.0.0-alpha.0.tgz" --variable APP_ID_ANDROID="ca-app-pub-xxx~yyy" --variable APP_ID_IOS="ca-app-pub-xxx~yyy" --variable GMA_NEXT_GEN_VERSION="1.2.1" --variable GMA_IOS_VERSION="13.6.0"
```

The tarball is only the download format. Cordova extracts it and installs the
normal plugin directory, including `plugin.xml`, `src`, `www`, `lib`, and `esm`.

## Features

- App Open Ads
- Banner Ads
- Interstitial Ads
- Rewarded Ads
- Rewarded Interstitial Ads
- Native Ads ([admob-plus-cordova-native](https://www.npmjs.com/package/admob-plus-cordova-native))
- WebView Ads ([admob-plus-cordova-webview-ad](https://www.npmjs.com/package/admob-plus-cordova-webview-ad))
- User Consent ([cordova-plugin-consent](https://www.npmjs.com/package/cordova-plugin-consent))

## Android GMA Next-Gen prerelease

This branch uses [GMA Next-Gen SDK 1.2.1][migration-guide] for Android.
The Cordova JavaScript bridge and the iOS implementation remain in place. An
intermediate update to Google Mobile Ads SDK (Legacy) 25.4.0 is not required;
Android applications can migrate directly from the previous 24.2.0 dependency.

Android requirements:

- Cordova Android 13.0.0 or newer
- Android `minSdk` 24 or newer
- Android `compileSdk` 34 or newer
- Kotlin 1.9 or newer

The Android SDK version preference was renamed from `PLAY_SERVICES_VERSION` to
`GMA_NEXT_GEN_VERSION` and defaults to `1.2.1`:

```xml
<preference name="GMA_NEXT_GEN_VERSION" value="1.2.1" />
```

Next-Gen initialization is performed programmatically with
`InitializationConfig` on a background worker thread. The plugin reads
`APP_ID_ANDROID` from manifest metadata, so the existing Cordova application ID
preference is still required.

### Removed APIs

The following deprecated APIs are no longer supported by this prerelease:

- Replace `AdSizeType.SMART_BANNER` with `{ adaptive: "anchored" }`.
- Replace `tagForChildDirectedTreatment` with `ageRestrictedTreatment` set to
  `"child"` or `"unspecified"`.
- Replace `tagForUnderAgeOfConsent` with `ageRestrictedTreatment` set to
  `"teen"` or `"unspecified"`.
- Replace `sameAppKey` with `publisherFirstPartyIDEnabled`.

The default banner is now a large anchored adaptive banner. Custom banner
`width`, `height`, and `maxHeight` values are density-independent pixels (dp);
they must not be pre-converted to physical pixels.

Mediation adapters that transitively depend on the Legacy SDK can otherwise
package both SDKs. The plugin excludes `play-services-ads` and
`play-services-ads-lite` globally, as required by the Next-Gen migration guide.
Verify every mediation adapter used by the application against the current
Next-Gen support matrix before releasing.

## iOS Google Mobile Ads SDK 13 prerelease

This branch uses [Google Mobile Ads SDK 13.6.0][ios-release-notes] on iOS while
preserving the existing Cordova JavaScript bridge.

iOS requirements:

- Cordova iOS 6.0.0 or newer
- iOS 13.0 or newer
- Xcode 26.2 or newer
- CocoaPods 1.16 or newer

The CocoaPods version can be selected when installing the plugin and defaults
to the exact tested version, `13.6.0`:

```shell
cordova plugin add admob-plus-cordova \
  --variable GMA_IOS_VERSION=13.6.0
```

The Swift bridge uses the SDK 13 async loading APIs and the Swift names
introduced in SDK 12. Anchored adaptive banners now use the SDK 13 large
anchored adaptive size APIs. Their height can be larger than the previous
anchored adaptive banners because the supported range increased from 50-90 to
50-150 points.

The deprecated child-directed and under-age request flags are no longer used.
Set `ageRestrictedTreatment` to `"child"`, `"teen"`, or `"unspecified"`.
The deprecated native `willLeaveApplication` callback is also no longer
implemented.

If Meta Audience Network mediation is used, install a current
`GoogleMobileAdsMediationFacebook` adapter that supports Google Mobile Ads SDK
13 and verify all requested formats on a physical device. Meta does not support
anchored or inline adaptive banner sizes, so its banner demand requires a
supported fixed banner size.

## Compare to other projects

|              Project              |  No Ad-Sharing  |    Fully Open Sourced     |        No Remote Control        |
| --------------------------------- | --------------- | ------------------------- | ------------------------------- |
| [admob-plus-cordova][p0]          | ✅               | ✅                         | ✅                               |
| [admob][p1]                       | Not Sure        | [❌][p2-bin1] [❌][p2-bin2] | Not Sure                        |
| [cordova-admob][p2]               | [❌][p2-android] | ✅                         | ✅                               |
| [cordova-plugin-ad-admob][p3]     | [❌][p3-android] | ✅                         | ✅                               |
| [cordova-plugin-admob-simple][p4] | [❌][p4-android] | ✅                         | [❌][p4-remote1]                 |
| [cordova-plugin-admobpro][p5]     | [❌][p5-share]   | [❌][p5-bin1] [❌][p5-bin2] | [❌][p5-remote1] [❌][p5-remote2] |
| [cordova-plugin-ads][p6]          | [❌][p6-share]   | ✅                         | ✅                               |

Click ❌ to see the detail.

[p0]: https://www.npmjs.com/package/admob-plus-cordova
[p1]: https://www.npmjs.com/package/admob
[p2]: https://www.npmjs.com/package/cordova-admob
[p2-android]: https://github.com/appfeel/admob-google-cordova/blob/3f122f278a323a4bc9e580f400182a7bd690a346/src/android/AdMobAds.java#L569
[p2-bin1]: https://github.com/admob-google/admob-cordova/blob/master/src/android/libs/admobadplugin.jar
[p2-bin2]: https://github.com/admob-google/admob-cordova/blob/master/src/ios/AdmobAPI.framework/AdmobAPI
[p3]: https://www.npmjs.com/package/cordova-plugin-ad-admob
[p3-android]: https://github.com/cranberrygame/cordova-plugin-ad-admob/blob/7aaa397b19ab63579d6aa68fbf20ffdf795a15fc/src/android/AdMobPlugin.java#L330
[p4]: https://github.com/sunnycupertino/cordova-plugin-admob-simple
[p4-android]: https://github.com/sunnycupertino/cordova-plugin-admob-simple/blob/a58846c1ea14188a4aef44381ccd28ffdcae3bfa/src/android/AdMob.java#L207
[p4-remote1]: https://github.com/sunnycupertino/cordova-plugin-admob-simple/blob/f7cc64e9e018f2146b2735b5ae8d3b780fa24f72/src/android/AdMob.java#L728
[p5]: https://www.npmjs.com/package/cordova-plugin-admobpro
[p5-share]: https://github.com/floatinghotpot/cordova-admob-pro/wiki/License-Agreement#2-win-win-partnership
[p5-bin1]: https://unpkg.com/browse/cordova-plugin-extension@1.6.0/src/android/CordovaAd.jar
[p5-bin2]: https://unpkg.com/browse/cordova-plugin-extension@1.6.0/src/ios/libCordovaAd.a
[p5-remote1]: https://github.com/floatinghotpot/cordova-admob-pro/pull/658
[p5-remote2]: https://github.com/ratson/cordova-plugin-admob-free/issues/354
[p6]: https://www.npmjs.com/package/cordova-plugin-ads
[p6-share]: https://github.com/cozycodegh/cordova-plugin-ads/blob/3d8f14ac02a8a7bad0ab4b472e6b776640f88c15/www/ads.js#L32


## Contributing

- Star this repository
- Open issue for feature requests
- [Sponsor this project](https://admob-plus.github.io/funding)

## Disclaimer

This is third party software, not a product of Google Inc.

Google AdMob is a trademark of Google, Inc.

This plugin utilizes Google AdMob SDKs to provide support for Cordova applications.

## License

AdMob Plus Cordova is [MIT licensed][b6-l].

[b1]: https://img.shields.io/npm/v/admob-plus-cordova.svg
[b2]: https://img.shields.io/github/last-commit/admob-plus/admob-plus
[b2-l]: https://github.com/admob-plus/admob-plus
[b3]: https://img.shields.io/librariesio/release/npm/admob-plus-cordova
[b4]: https://snyk.io/advisor/npm-package/admob-plus-cordova/badge.svg
[b4-l]: https://snyk.io/advisor/npm-package/admob-plus-cordova
[b5]: https://img.shields.io/snyk/vulnerabilities/npm/admob-plus-cordova
[b6]: https://img.shields.io/npm/l/admob-plus-cordova
[b6-l]: https://github.com/admob-plus/admob-plus/blob/master/LICENSE
[migration-guide]: https://developers.google.com/admob/android/next-gen/migration
[ios-release-notes]: https://developers.google.com/admob/ios/rel-notes
