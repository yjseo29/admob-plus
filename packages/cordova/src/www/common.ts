export const CordovaService = "AdMob";

export type CordovaAction =
  | "adCreate"
  | "adHide"
  | "adIsLoaded"
  | "adLoad"
  | "adShow"
  | "bannerConfig"
  | "configure"
  | "ready"
  | "start"
  | "webviewGoto";

export enum Events {
  adClick = "admob.ad.click",
  adDismiss = "admob.ad.dismiss",
  adImpression = "admob.ad.impression",
  adLoad = "admob.ad.load",
  adLoadFail = "admob.ad.loadfail",
  adReward = "admob.ad.reward",
  adShow = "admob.ad.show",
  adShowFail = "admob.ad.showfail",
  bannerSize = "admob.banner.size",
  ready = "admob.ready",
}

// biome-ignore lint/suspicious/noConstEnum: ignore
export const enum Platform {
  android = "android",
  ios = "ios",
}

/**
 * An enum that represents the maximum ad content rating for an app or ad request.
 * @enum {string}
 */
type MaxAdContentRating =
  | /** Content suitable for general audiences, including families. */ "G"
  | /** Content suitable only for mature audiences. */ "MA"
  | /** Content suitable for most audiences with parental guidance. */ "PG"
  | /** Content suitable for teen and older audiences. */ "T"
  | /** Content suitability is unspecified. */ "";

export interface RequestConfig {
  /** Selects the Next-Gen age-restricted treatment for every Android ad request. */
  ageRestrictedTreatment?: "child" | "teen" | "unspecified";
  maxAdContentRating?: MaxAdContentRating;
  testDeviceIds?: string[];
}

export interface AdMobConfig extends RequestConfig {
  appMuted?: boolean;
  appVolume?: number;
  publisherFirstPartyIDEnabled?: boolean;
}

/** @internal */
export function execAsync<T>(action: CordovaAction, args?: unknown[]) {
  return new Promise<T>((resolve, reject) => {
    cordova.exec(resolve, reject, CordovaService, action, args);
  });
}
