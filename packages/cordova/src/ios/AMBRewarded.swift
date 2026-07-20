import GoogleMobileAds

class AMBRewarded: AMBAdBase, FullScreenContentDelegate {
    var mAd: RewardedAd?

    deinit {
        clear()
    }

    override func isLoaded() -> Bool {
        return self.mAd != nil
    }

    override func load(_ ctx: AMBContext) {
        clear()

        Task { @MainActor in
            do {
                let ad = try await RewardedAd.load(with: self.adUnitId, request: self.adRequest)
                self.mAd = ad
                ad.fullScreenContentDelegate = self
                ad.serverSideVerificationOptions = ctx.optServerSideVerificationOptions()

                self.emit(AMBEvents.adLoad)
                ctx.resolve()
            } catch {
                self.emit(AMBEvents.adLoadFail, error)
                ctx.reject(error)
            }
        }
    }

    override func show(_ ctx: AMBContext) {
        mAd?.present(from: plugin.viewController, userDidEarnRewardHandler: {
            let reward = self.mAd!.adReward
            self.emit(AMBEvents.adReward, reward)
        })
        ctx.resolve()
    }

    func adDidRecordImpression(_ ad: FullScreenPresentingAd) {
        self.emit(AMBEvents.adImpression)
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        clear()
        self.emit(AMBEvents.adShowFail, error)
    }

    func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        self.emit(AMBEvents.adShow)
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        clear()
        self.emit(AMBEvents.adDismiss)
    }

    private func clear() {
        mAd?.fullScreenContentDelegate = nil
        mAd = nil
    }
}
