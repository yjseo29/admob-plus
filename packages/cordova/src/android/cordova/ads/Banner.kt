package admob.plus.cordova.ads

import admob.plus.cordova.Events
import admob.plus.cordova.ExecuteContext
import admob.plus.core.applyAdRequestOptions
import admob.plus.core.buildAdSize
import admob.plus.core.pxToDp
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Insets
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.RoundedCorner
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import org.json.JSONObject

enum class AdSizeType {
    BANNER, LARGE_BANNER, MEDIUM_RECTANGLE, FULL_BANNER, LEADERBOARD;

    companion object {
        fun getAdSize(adSize: Int): AdSize? {
            return when (entries.getOrNull(adSize)) {
                BANNER -> AdSize.BANNER
                LARGE_BANNER -> AdSize.LARGE_BANNER
                MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
                FULL_BANNER -> AdSize.FULL_BANNER
                LEADERBOARD -> AdSize.LEADERBOARD
                else -> null
            }
        }
    }
}

fun buildGravity(opts: JSONObject): Int {
    return if ("top" == opts.optString("position")) Gravity.TOP else Gravity.BOTTOM
}

fun buildOffset(opts: JSONObject): Int? {
    return if (opts.has("offset")) {
        opts.optInt("offset")
    } else null
}

class Banner(ctx: ExecuteContext) : AdBase(ctx) {
    private val adSize: AdSize
    private val gravity: Int
    private val offset: Int?
    private var mAdView: AdView? = null
    private var mRelativeLayout: RelativeLayout? = null
    private var mAdViewOld: AdView? = null

    /**
     * True when show() was requested before the ad finished loading; the view is
     * then attached in onAdLoaded. "Last call wins": hide() (and onDestroy) clears
     * the flag, so a screen that hides the banner while a load is still in flight
     * never gets a late pop-up.
     */
    private var pendingShow = false

    /**
     * True while a bottom banner is temporarily hidden because the soft keyboard
     * covers its area (see [applyWrapperInsets]). Distinct from a user hide():
     * the banner restores itself when the keyboard goes away, and the JS-visible
     * state (show/hide semantics) is not touched. Reset by show()/hide()/onDestroy
     * so a user action always wins over the automatic toggle.
     */
    private var imeAutoHidden = false

    override val isLoaded: Boolean
        get() = mAdView?.getBannerAd() != null

    init {
        adSize = buildAdSize(initOpts, ctx.activity)
        gravity = buildGravity(initOpts)
        offset = buildOffset(initOpts)
    }

    override fun load(ctx: ExecuteContext) {
        if (mAdView == null) {
            mAdView = createBannerView()
        }
        loadBannerView(mAdView!!)
        ctx.resolve()
    }

    private fun createBannerView(): AdView {
        return AdView(plugin.activity)
    }

    private fun loadBannerView(adView: AdView) {
        val requestBuilder = BannerAdRequest.Builder(adUnitId, adSize)
        applyAdRequestOptions(requestBuilder, initOpts)
        adView.loadAd(requestBuilder.build(), object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                ad.adEventCallback = object : BannerAdEventCallback {
                    override fun onAdClicked() {
                        emit(Events.AD_CLICK)
                    }

                    override fun onAdDismissedFullScreenContent() {
                        emit(Events.AD_DISMISS)
                    }

                    override fun onAdImpression() {
                        emit(Events.AD_IMPRESSION)
                    }

                    override fun onAdShowedFullScreenContent() {
                        emit(Events.AD_SHOW)
                    }

                    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                        emit(Events.AD_SHOW_FAIL, fullScreenContentError)
                    }
                }
                // Next-Gen delivers callbacks on a background thread; view mutations stay on the UI thread.
                plugin.activity.runOnUiThread {
                    if (adView !== mAdView) {
                        removeBannerView(adView)
                        return@runOnUiThread
                    }
                    if (mAdViewOld != null) {
                        removeBannerView(mAdViewOld!!)
                        mAdViewOld = null
                    }
                    if (pendingShow) {
                        // A show() arrived while this load was in flight and no hide()
                        // cancelled it since — attach now. Restore visibility too:
                        // a show→hide→show sequence during the load leaves the view GONE.
                        pendingShow = false
                        adView.visibility = View.VISIBLE
                        addBannerView()
                        if (offset == null) applyWrapperInsets()
                    }
                    runJustBeforeBeingDrawn(adView) {
                        emit(Events.BANNER_SIZE, computeAdSize())
                    }
                    emit(Events.AD_LOAD, computeAdSize())
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                emit(Events.AD_LOAD_FAIL, adError)
            }
        })
    }

    private fun computeAdSize(): Map<String, Any> {
        val width = mAdView!!.width
        val height = mAdView!!.height
        return mapOf(
            "size" to mapOf(
                "width" to pxToDp(width),
                "height" to pxToDp(height),
                "widthInPixels" to width,
                "heightInPixels" to height,
            )
        )
    }

    override fun show(ctx: ExecuteContext) {
        pendingShow = false // showing right now — no deferred attach needed
        imeAutoHidden = false // fresh user intent; the insets listener re-evaluates the IME state
        // Always restore visibility first: hide() may have set GONE before the view
        // was ever attached (hide while the first load was in flight). The
        // parent==null branch below never touched visibility, which used to attach
        // a permanently invisible banner in that sequence.
        mAdView!!.visibility = View.VISIBLE
        if (mAdView!!.parent == null) {
            addBannerView()
        } else {
            val wvParentView = getParentView(webView)
            if (rootLinearLayout !== wvParentView) {
                removeFromParentView(rootLinearLayout)
                addBannerView()
            }
        }
        if (offset == null) applyWrapperInsets()
        ctx.resolve()
    }

    override fun showNotLoaded(ctx: ExecuteContext) {
        // Remember the intent; onAdLoaded attaches the view unless a hide()
        // cancels it first. Still resolves false so callers keep treating the
        // banner as "not visible yet" (the size event reports the real moment).
        pendingShow = true
        ctx.resolve(false)
    }

    override fun hide(ctx: ExecuteContext) {
        pendingShow = false // cancel a deferred show — "last call wins"
        imeAutoHidden = false // user hide overrides the automatic IME toggle
        if (mAdView != null) {
            mAdView!!.visibility = View.GONE
        }
        // The wrapper stays around while hidden; drop the system-bar padding so the
        // WebView goes back to drawing edge-to-edge behind the bars.
        if (offset == null) clearWrapperInsets()
        ctx.resolve()
    }

    /**
     * In an edge-to-edge window (cordova-android `AndroidEdgeToEdge=true`) the
     * wrapper LinearLayout spans the whole screen, so a bottom banner is laid out
     * behind the transparent navigation bar (and a top banner behind the status
     * bar). Pad the banner edge of the wrapper by the system-bar inset so the
     * AdView clears the bar; the wrapper background (see [config]) fills the
     * padded strip. No-op when the app is not edge-to-edge — the window already
     * avoids the bars there, and padding would double the offset.
     *
     * The same listener also hides a bottom banner while the soft keyboard is
     * visible: the keyboard draws over the banner area anyway, and if the banner
     * kept its layout slot the WebView would shrink by keyboard + banner height
     * (double subtraction). The banner (and its system-bar padding) comes back
     * when the keyboard fully hides — see [imeAutoHidden]. Handling this here
     * keeps it in the same insets dispatch cordova-android uses to resize the
     * WebView, so the relayout happens in one pass with no JS round-trip.
     * Top banners are unaffected — the keyboard never covers them.
     */
    private fun applyWrapperInsets() {
        val wrapper = rootLinearLayout ?: return
        if (!plugin.isEdgeToEdge) return
        wrapper.setOnApplyWindowInsetsListener { v, insets ->
            val top: Int
            val bottom: Int
            val ime: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                top = bars.top
                bottom = bars.bottom
                ime = insets.getInsets(WindowInsets.Type.ime()).bottom
            } else {
                // Stable insets = bar sizes independent of the IME; the IME inflates
                // the system window insets, so the difference is the keyboard height.
                @Suppress("DEPRECATION")
                top = insets.stableInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.stableInsetBottom
                @Suppress("DEPRECATION")
                ime = (insets.systemWindowInsetBottom - bottom).coerceAtLeast(0)
            }
            if (isPositionTop) {
                v.setPadding(0, top, 0, v.paddingBottom)
                // The wrapper now covers the top edge (padding + banner) — consume it
                // so inset-aware children do not pad for it again.
                return@setOnApplyWindowInsetsListener consumeBannerEdge(insets)
            }
            if (ime > 0) {
                // Keyboard covers the banner area — release the banner's layout slot
                // so the WebView only shrinks by the keyboard height (cordova applies
                // that as a WebView margin in the same dispatch).
                if (mAdView?.visibility == View.VISIBLE) {
                    imeAutoHidden = true
                    mAdView?.visibility = View.GONE
                }
                if (imeAutoHidden) {
                    v.setPadding(0, v.paddingTop, 0, 0)
                }
                // Nothing consumed: the wrapper is not covering the bottom edge right
                // now, and children must still see the IME inset.
                return@setOnApplyWindowInsetsListener insets
            }
            if (imeAutoHidden) {
                imeAutoHidden = false
                mAdView?.visibility = View.VISIBLE
            }
            v.setPadding(0, v.paddingTop, 0, bottom)
            consumeBannerEdge(insets)
        }
        wrapper.requestApplyInsets()
    }

    /**
     * Returns [insets] with the banner edge zeroed for the types a child would use
     * for edge padding (status/navigation bars + display cutout). While a banner is
     * shown, the wrapper already covers that edge with its own padding plus the
     * AdView, so inset-aware children must not pad for it again — e.g. the
     * `@totalpave/cordova-plugin-insets` listener sits on the WebView (a child of
     * the wrapper) and would otherwise report the navigation-bar inset to JS even
     * though the WebView no longer reaches that edge. Standard Android insets
     * etiquette: consume what you handled before dispatching to children.
     */
    private fun consumeBannerEdge(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT >= 30) {
            // Copy-based builder: everything about the *other* edges must stay intact.
            // Consumers may mix rounded-corner radii into their edge math for BOTH
            // edges (@totalpave/cordova-plugin-insets takes max(inset, corner radius)
            // for top and bottom) — an earlier from-scratch rebuild dropped the TOP
            // corner info too, which changed the reported top inset whenever a bottom
            // banner toggled and made the whole page shift vertically on devices
            // where the corner radius exceeds the status-bar inset (Galaxy S22).
            // Only the banner edge is neutralized: its bar/cutout insets are zeroed
            // and, on API 31+, its rounded corners are cleared as well (a radius left
            // in place would resurface as a phantom inset on the consumed edge).
            val builder = WindowInsets.Builder(insets)
            for (type in intArrayOf(
                    WindowInsets.Type.statusBars(),
                    WindowInsets.Type.navigationBars(),
                    WindowInsets.Type.displayCutout()
            )) {
                val i = insets.getInsets(type)
                builder.setInsets(
                    type,
                    if (isPositionTop) Insets.of(i.left, 0, i.right, i.bottom)
                    else Insets.of(i.left, i.top, i.right, 0)
                )
            }
            if (Build.VERSION.SDK_INT >= 31) {
                val positions =
                    if (isPositionTop) intArrayOf(
                        RoundedCorner.POSITION_TOP_LEFT, RoundedCorner.POSITION_TOP_RIGHT
                    )
                    else intArrayOf(
                        RoundedCorner.POSITION_BOTTOM_LEFT, RoundedCorner.POSITION_BOTTOM_RIGHT
                    )
                for (position in positions) {
                    // Only clear corners that exist: the copy constructor leaves the
                    // internal corner container null when the source has none, and
                    // setRoundedCorner would NPE on it (checked against AOSP 12..16).
                    if (insets.getRoundedCorner(position) != null) {
                        builder.setRoundedCorner(position, null)
                    }
                }
            }
            return builder.build()
        }
        // Pre-R only the merged system window insets can be replaced. Display cutout
        // objects are immutable there, so a bottom-cutout device on API 28..29 may
        // still report a residual cutout inset — acceptable for that rare combination.
        @Suppress("DEPRECATION")
        return insets.replaceSystemWindowInsets(
            insets.systemWindowInsetLeft,
            if (isPositionTop) 0 else insets.systemWindowInsetTop,
            insets.systemWindowInsetRight,
            if (isPositionTop) insets.systemWindowInsetBottom else 0
        )
    }

    /** Removes the inset listener and padding added by [applyWrapperInsets]. */
    private fun clearWrapperInsets() {
        val wrapper = rootLinearLayout ?: return
        wrapper.setOnApplyWindowInsetsListener(null)
        wrapper.setPadding(0, 0, 0, 0)
        // Re-dispatch so children see the un-consumed insets again — without this,
        // an inset-aware child (e.g. the insets plugin on the WebView) would keep
        // the values from when the banner was still consuming the edge.
        wrapper.requestApplyInsets()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val w = plugin.activity.resources.displayMetrics.widthPixels
        if (w != screenWidth) {
            screenWidth = w
            plugin.activity.runOnUiThread { reloadBannerView() }
        }
    }

    private fun reloadBannerView() {
        // GONE normally means "hidden by the user" — skip the reload. But a banner
        // auto-hidden by the IME (see imeAutoHidden) is still logically visible and
        // must pick up the new width, e.g. rotating while the keyboard is open.
        if (mAdView == null || (mAdView!!.visibility == View.GONE && !imeAutoHidden)) return
        if (mAdViewOld != null) removeBannerView(mAdViewOld!!)
        mAdViewOld = mAdView
        mAdView = createBannerView()
        loadBannerView(mAdView!!)
        addBannerView()
        // The fresh AdView starts VISIBLE; re-evaluate the insets so a banner that
        // was auto-hidden for the keyboard goes straight back to hidden until the
        // keyboard closes (the listener alone only fires on the next insets change).
        if (offset == null) {
            imeAutoHidden = false
            applyWrapperInsets()
        }
    }

    override fun onDestroy() {
        pendingShow = false
        // The wrapper (with the WebView inside) outlives the banner; make sure the
        // system-bar padding does not linger once the banner is gone.
        if (offset == null) clearWrapperInsets()
        if (mAdView != null) {
            removeBannerView(mAdView!!)
            mAdView = null
        }
        if (mAdViewOld != null) {
            removeBannerView(mAdViewOld!!)
            mAdViewOld = null
        }
        if (mRelativeLayout != null) {
            removeFromParentView(mRelativeLayout)
            mRelativeLayout = null
        }
        super.onDestroy()
    }

    private fun removeBannerView(adView: AdView) {
        removeFromParentView(adView)
        adView.removeAllViews()
        adView.destroy()
    }

    private fun addBannerView() {
        if (mAdView == null) return
        if (offset == null) {
            if (getParentView(mAdView) === rootLinearLayout && rootLinearLayout != null) return
            addBannerViewWithLinearLayout()
        } else {
            if (getParentView(mAdView) === mRelativeLayout && mRelativeLayout != null) return
            addBannerViewWithRelativeLayout()
        }
        plugin.contentView?.let {
            it.bringToFront()
            it.requestLayout()
            it.requestFocus()
        }
    }

    private fun addBannerViewWithLinearLayout() {
        val wvParentView = getParentView(webView)
        if (rootLinearLayout == null) {
            rootLinearLayout = LinearLayout(plugin.activity)
        }
        // Keep the wrapper color in sync with BannerAd.config(), including when the
        // wrapper is (re)created after the config call.
        backgroundColor?.let { rootLinearLayout!!.setBackgroundColor(it) }
        if (wvParentView != null && wvParentView !== rootLinearLayout) {
            wvParentView.removeView(webView)
            val content = rootLinearLayout as LinearLayout?
            content!!.orientation = LinearLayout.VERTICAL
            rootLinearLayout!!.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.0f
            )
            webView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1.0f
            )
            rootLinearLayout!!.addView(webView)
            val view = getParentView(rootLinearLayout)
            if (view !== wvParentView) {
                removeFromParentView(rootLinearLayout)
                wvParentView.addView(rootLinearLayout)
            }
        }
        removeFromParentView(mAdView)
        if (isPositionTop) {
            rootLinearLayout!!.addView(mAdView, 0)
        } else {
            rootLinearLayout!!.addView(mAdView)
        }
        plugin.contentView?.let {
            for (i in 0 until it.childCount) {
                val view = it.getChildAt(i)
                (view as? RelativeLayout)?.bringToFront()
            }
        }
    }

    private fun addBannerViewWithRelativeLayout() {
        val paramsContent = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        paramsContent.addRule(if (isPositionTop) RelativeLayout.ALIGN_PARENT_TOP else RelativeLayout.ALIGN_PARENT_BOTTOM)
        if (mRelativeLayout == null) {
            mRelativeLayout = RelativeLayout(plugin.activity)
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            if (isPositionTop) {
                params.setMargins(0, offset!!, 0, 0)
            } else {
                params.setMargins(0, 0, 0, offset!!)
            }
            plugin.contentView?.addView(mRelativeLayout, params)
                ?: Log.e(TAG, "Unable to find content view")
        }
        removeFromParentView(mAdView)
        mRelativeLayout!!.addView(mAdView, paramsContent)
        mRelativeLayout!!.bringToFront()
    }

    private val isPositionTop: Boolean
        get() = gravity == Gravity.TOP

    companion object {
        private const val TAG = "AdMobPlus.Banner"

        @SuppressLint("StaticFieldLeak")
        private var rootLinearLayout: ViewGroup? = null
        private var screenWidth = 0

        /** Background color for the banner wrapper layout, set via BannerAd.config(). */
        private var backgroundColor: Int? = null

        /**
         * Handles the `bannerConfig` action (BannerAd.config() in JS).
         *
         * Android counterpart of AMBBanner.config on iOS, limited to `backgroundColor`.
         * The color is applied to the wrapper LinearLayout that hosts the WebView and
         * the banner. In edge-to-edge apps the wrapper extends behind the transparent
         * navigation bar while the AdView sits above it, so without a color the
         * window/theme background bleeds through that strip (e.g. a light band in
         * dark mode). Must run on the UI thread.
         */
        fun config(ctx: ExecuteContext) {
            ctx.optBackgroundColor()?.let { color ->
                backgroundColor = color
                // Apply immediately when a banner is already attached.
                rootLinearLayout?.setBackgroundColor(color)
            }
            ctx.resolve()
        }

        fun destroyParentView() {
            try {
                val vg = getParentView(rootLinearLayout)
                vg?.removeAllViews()
            } finally {
                rootLinearLayout = null
            }
        }

        private fun runJustBeforeBeingDrawn(view: View, runnable: Runnable) {
            val preDrawListener: ViewTreeObserver.OnPreDrawListener =
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        view.viewTreeObserver.removeOnPreDrawListener(this)
                        runnable.run()
                        return true
                    }
                }
            view.viewTreeObserver.addOnPreDrawListener(preDrawListener)
        }
    }
}
