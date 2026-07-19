package admob.plus.cordova.ads

import admob.plus.cordova.Events
import admob.plus.cordova.ExecuteContext
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback

class RewardedInterstitial(ctx: ExecuteContext) : AdBase(ctx) {
    @Volatile
    private var mAd: RewardedInterstitialAd? = null
    override fun onDestroy() {
        clear()
        super.onDestroy()
    }

    override fun load(ctx: ExecuteContext) {
        clear()
        RewardedInterstitialAd.load(
            adRequest,
            object : AdLoadCallback<RewardedInterstitialAd> {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mAd = null
                    emit(Events.AD_LOAD_FAIL, loadAdError)
                    ctx.reject(loadAdError.toString())
                }

                override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                    mAd = rewardedAd
                    val ssv = buildServerSideVerificationOptions(initOpts)
                    if (ssv != null) {
                        mAd!!.setServerSideVerificationOptions(ssv)
                    }
                    mAd!!.adEventCallback = object : RewardedInterstitialAdEventCallback {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd.destroy()
                            emit(Events.AD_DISMISS)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                            mAd = null
                            rewardedAd.destroy()
                            emit(Events.AD_SHOW_FAIL, adError)
                        }

                        override fun onAdShowedFullScreenContent() {
                            mAd = null
                            emit(Events.AD_SHOW)
                        }

                        override fun onAdImpression() {
                            emit(Events.AD_IMPRESSION)
                        }

                        override fun onAdClicked() {
                            emit(Events.AD_CLICK)
                        }
                    }
                    emit(Events.AD_LOAD)
                    ctx.resolve()
                }
            })
    }

    override val isLoaded: Boolean
        get() = mAd != null

    override fun show(ctx: ExecuteContext) {
        if (this.isLoaded) {
            mAd!!.show(plugin.activity) { rewardItem: RewardItem? ->
                emit(Events.AD_REWARD, rewardItem!!)
            }
            ctx.resolve()
        } else {
            ctx.reject("Ad is not loaded")
        }
    }

    private fun clear() {
        mAd?.destroy()
        mAd = null
    }
}
