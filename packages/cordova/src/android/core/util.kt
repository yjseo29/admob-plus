package admob.plus.core

import admob.plus.cordova.ads.AdSizeType
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.os.Bundle
import android.provider.Settings
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
import kotlin.math.roundToInt

fun applyAdRequestOptions(builder: AdRequest.Builder, opts: JSONObject): AdRequest.Builder {
    optStringOrNull(opts, "contentUrl")?.let {
        builder.setContentUrl(it)
    }
    val extras = Bundle().apply {
        optStringOrNull(opts, "npa")?.let { npa ->
            putString("npa", npa)
        }
    }
    return builder.setGoogleExtrasBundle(extras)
}

fun applyAdRequestOptions(builder: BannerAdRequest.Builder, opts: JSONObject): BannerAdRequest.Builder {
    optStringOrNull(opts, "contentUrl")?.let {
        builder.setContentUrl(it)
    }
    return builder.setGoogleExtrasBundle(buildGoogleExtras(opts))
}

fun applyAdRequestOptions(builder: NativeAdRequest.Builder, opts: JSONObject): NativeAdRequest.Builder {
    optStringOrNull(opts, "contentUrl")?.let {
        builder.setContentUrl(it)
    }
    return builder.setGoogleExtrasBundle(buildGoogleExtras(opts))
}

private fun buildGoogleExtras(opts: JSONObject): Bundle {
    return Bundle().apply {
        optStringOrNull(opts, "npa")?.let { npa ->
            putString("npa", npa)
        }
    }
}

private fun optStringOrNull(opts: JSONObject, name: String): String? {
    return if (opts.has(name) && !opts.isNull(name)) opts.optString(name) else null
}

fun buildAdRequest(adUnitId: String, opts: JSONObject): AdRequest {
    val builder = AdRequest.Builder(adUnitId)
    applyAdRequestOptions(builder, opts)
    return builder.build()
}

fun buildAdSize(opts: JSONObject, activity: Activity): AdSize {
    val name = "size"
    if (!opts.has(name)) {
        return AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, availableWidthDp(activity))
    }
    val adSizeObj = opts.optJSONObject(name)
    if (adSizeObj == null) {
        return AdSizeType.getAdSize(opts.optInt(name))
            ?: AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, availableWidthDp(activity))
    }
    val adaptive = adSizeObj.optString("adaptive")
    // Cordova banner dimensions are expressed in dp. Only the physical display fallback needs conversion.
    val width = if (adSizeObj.has("width")) adSizeObj.optInt("width") else availableWidthDp(activity)
    if ("inline" == adaptive) {
        if (adSizeObj.has("maxHeight")) {
            return AdSize.getInlineAdaptiveBannerAdSize(width, adSizeObj.optInt("maxHeight"))
        }
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, width)
    }
    if ("anchored" == adaptive) {
        // The Large variants reserve a taller slot (better fill/revenue potential),
        // but smaller creatives are centered in it with blank space above/below.
        // `large: false` selects the classic anchored slot (50..90dp, same as the
        // previous SDK generation) for apps that prefer a slimmer banner.
        val large = adSizeObj.optBoolean("large", true)
        return when (adSizeObj.optString("orientation")) {
            "portrait" ->
                if (large) AdSize.getLargePortraitAnchoredAdaptiveBannerAdSize(activity, width)
                else AdSize.getPortraitAnchoredAdaptiveBannerAdSize(activity, width)

            "landscape" ->
                if (large) AdSize.getLargeLandscapeAnchoredAdaptiveBannerAdSize(activity, width)
                else AdSize.getLandscapeAnchoredAdaptiveBannerAdSize(activity, width)

            else ->
                if (large) AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, width)
                else AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, width)
        }
    }
    return AdSize(width, adSizeObj.optInt("height"))
}

private fun availableWidthDp(activity: Activity): Int {
    return (activity.resources.displayMetrics.widthPixels / activity.resources.displayMetrics.density).roundToInt()
}

fun optFloat(opts: JSONObject, name: String): Float? {
    if (!opts.has(name) || opts.isNull(name)) return null
    return opts.optDouble(name).toFloat()
}

fun buildRequestConfiguration(opts: JSONObject): RequestConfiguration {
    val builder = RequestConfiguration.Builder()
    when (opts.optString("maxAdContentRating", "").uppercase(Locale.US)) {
        "G" -> RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_G
        "PG" -> RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_PG
        "T" -> RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_T
        "MA" -> RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_MA
        else -> null
    }?.let {
        builder.setMaxAdContentRating(it)
    }
    when (opts.optString("ageRestrictedTreatment", "").lowercase(Locale.US)) {
        "child" -> AgeRestrictedTreatment.CHILD
        "teen" -> AgeRestrictedTreatment.TEEN
        "unspecified" -> AgeRestrictedTreatment.UNSPECIFIED
        else -> null
    }?.let {
        builder.setAgeRestrictedTreatment(it)
    }
    if (opts.has("testDeviceIds")) {
        builder.setTestDeviceIds(jsonArray2stringList(opts.optJSONArray("testDeviceIds")))
    }
    return builder.build()
}

fun configForTestLabIfNeeded(activity: Activity) {
    if (!isRunningInTestLab(activity)) {
        return
    }
    val config = MobileAds.getRequestConfiguration()
    val testDeviceIds = config.testDeviceIds.toMutableList()
    val deviceId = computeDeviceID(activity)
    if (deviceId in testDeviceIds) {
        return
    }
    testDeviceIds.add(deviceId)
    val builder = RequestConfiguration.Builder()
        .setAgeRestrictedTreatment(config.ageRestrictedTreatment)
        .setMaxAdContentRating(config.maxAdContentRating)
        .setPublisherPrivacyPersonalizationState(config.publisherPrivacyPersonalizationState)
        .setTestDeviceIds(testDeviceIds)
    MobileAds.setRequestConfiguration(builder.build())
}

fun computeDeviceID(activity: Activity): String {
    // This will request test ads on the emulator and device by passing this hashed device ID.
    @SuppressLint("HardwareIds") val androidID = Settings.Secure.getString(
        activity.contentResolver, Settings.Secure.ANDROID_ID
    )
    return md5(androidID).uppercase(Locale.getDefault())
}

fun isRunningInTestLab(activity: Activity): Boolean {
    val testLabSetting =
        Settings.System.getString(activity.contentResolver, "firebase.test.lab")
    return "true" == testLabSetting
}

fun dpToPx(dp: Double): Double {
    return dp * Resources.getSystem().displayMetrics.density
}

fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density).roundToInt()
}

fun jsonArray2stringList(a: JSONArray?): List<String> {
    val result: MutableList<String> = ArrayList()
    a?.let {
        for (i in 0 until it.length()) {
            it.optString(i)?.let { id ->
                result.add(id)
            }
        }
    }
    return result
}

fun md5(s: String): String {
    try {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(s.toByteArray())
        val bigInt = BigInteger(1, digest.digest())
        return String.format("%32s", bigInt.toString(16)).replace(' ', '0')
    } catch (ignore: NoSuchAlgorithmException) {
    }
    return ""
}
