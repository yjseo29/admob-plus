[![NPM version](https://img.shields.io/npm/v/cordova-plugin-consent.svg)](https://npmjs.org/package/cordova-plugin-consent)
[![GitHub last commit](https://img.shields.io/github/last-commit/admob-plus/admob-plus)](https://github.com/admob-plus/admob-plus)
![Dependency status](https://img.shields.io/librariesio/release/npm/cordova-plugin-consent)
![Vulnerabilities](https://img.shields.io/snyk/vulnerabilities/npm/cordova-plugin-consent)
![NPM license](https://img.shields.io/npm/l/cordova-plugin-consent)

# cordova-plugin-consent

Google Consent SDK Cordova Plugin

## Platform requirements

- Android API level 23 or newer
- Cordova iOS 6.0.0 or newer
- Xcode 16.0 or newer for iOS builds
- iOS 12.0 or newer

This prerelease uses Google User Messaging Platform SDK 4.0.0 on Android and
3.1.0 on iOS. The Cordova JavaScript API remains unchanged.

The native SDK versions can be selected when installing the plugin:

```shell
cordova plugin add cordova-plugin-consent \
  --variable UMP_VERSION=4.0.0 \
  --variable UMP_IOS_VERSION=3.1.0
```

`UMP_VERSION` configures Android and is retained for backward compatibility.
`UMP_IOS_VERSION` configures the CocoaPods dependency on iOS.

## Documentation

You can find the documentation [on the website](https://admob-plus.github.io/docs/cordova/consent).
