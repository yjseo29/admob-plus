import GoogleMobileAds

class AMBInterstitial: AMBAdBase, FullScreenContentDelegate {
    var mAd: InterstitialAd?

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
                let ad = try await InterstitialAd.load(with: self.adUnitId, request: self.adRequest)
                self.mAd = ad
                ad.fullScreenContentDelegate = self

                self.emit(AMBEvents.adLoad)
                ctx.resolve()
            } catch {
                self.emit(AMBEvents.adLoadFail, error)
                ctx.reject(error)
            }
        }
    }

    override func show(_ ctx: AMBContext) {
        mAd?.present(from: plugin.viewController)
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
