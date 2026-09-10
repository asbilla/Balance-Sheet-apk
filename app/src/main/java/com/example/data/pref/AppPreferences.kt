package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _webAppUrlFlow = MutableStateFlow(getWebAppUrl())
    val webAppUrlFlow: StateFlow<String> = _webAppUrlFlow.asStateFlow()

    private val _businessProfileFlow = MutableStateFlow(getBusinessProfile())
    val businessProfileFlow: StateFlow<BusinessProfile> = _businessProfileFlow.asStateFlow()

    fun getWebAppUrl(): String {
        return prefs.getString(KEY_WEB_APP_URL, "")?.trim() ?: ""
    }

    fun setWebAppUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString(KEY_WEB_APP_URL, trimmed).apply()
        _webAppUrlFlow.value = trimmed
    }

    fun getBusinessProfile(): BusinessProfile {
        return BusinessProfile(
            businessName = prefs.getString(KEY_BUSINESS_NAME, "") ?: "",
            abnAcn = prefs.getString(KEY_ABN_ACN, "") ?: "",
            businessAddress = prefs.getString(KEY_BUSINESS_ADDRESS, "") ?: "",
            phoneMobile = prefs.getString(KEY_PHONE_MOBILE, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: ""
        )
    }

    fun setBusinessProfile(profile: BusinessProfile) {
        prefs.edit()
            .putString(KEY_BUSINESS_NAME, profile.businessName.trim())
            .putString(KEY_ABN_ACN, profile.abnAcn.trim())
            .putString(KEY_BUSINESS_ADDRESS, profile.businessAddress.trim())
            .putString(KEY_PHONE_MOBILE, profile.phoneMobile.trim())
            .putString(KEY_EMAIL, profile.email.trim())
            .apply()
        _businessProfileFlow.value = profile
    }

    fun isConfigured(): Boolean {
        return getWebAppUrl().isNotEmpty()
    }

    fun clearUrl() {
        prefs.edit().remove(KEY_WEB_APP_URL).apply()
        _webAppUrlFlow.value = ""
    }

    fun getCachedProducts(): List<com.example.data.model.ProductItem> {
        val rawJson = prefs.getString(KEY_CACHED_PRODUCTS, "") ?: ""
        if (rawJson.isBlank()) {
            return getDefaultProducts()
        }
        return try {
            val array = org.json.JSONArray(rawJson)
            val list = mutableListOf<com.example.data.model.ProductItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.data.model.ProductItem(
                        name = obj.optString("name", ""),
                        price = obj.optDouble("price", 0.0),
                        category = obj.optString("category", "")
                    )
                )
            }
            if (list.isEmpty()) getDefaultProducts() else list
        } catch (_: Exception) {
            getDefaultProducts()
        }
    }

    fun setCachedProducts(products: List<com.example.data.model.ProductItem>) {
        try {
            val array = org.json.JSONArray()
            for (p in products) {
                val obj = org.json.JSONObject().apply {
                    put("name", p.name)
                    put("price", p.price)
                    put("category", p.category)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_CACHED_PRODUCTS, array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun getDefaultProducts(): List<com.example.data.model.ProductItem> {
        return listOf(
            com.example.data.model.ProductItem("Eyebrows", 10.00, "Threading"),
            com.example.data.model.ProductItem("Uper Lips", 5.00, "Threading"),
            com.example.data.model.ProductItem("Chin", 5.00, "Threading"),
            com.example.data.model.ProductItem("Forehead", 5.00, "Threading"),
            com.example.data.model.ProductItem("Sideburns", 12.00, "Threading"),
            com.example.data.model.ProductItem("Neck", 5.00, "Threading"),
            com.example.data.model.ProductItem("Full Face", 35.00, "Threading"),
            com.example.data.model.ProductItem("Underarms", 15.00, "Body Waxing"),
            com.example.data.model.ProductItem("Full Arms", 30.00, "Body Waxing"),
            com.example.data.model.ProductItem("1/2 Arms", 20.00, "Body Waxing"),
            com.example.data.model.ProductItem("3/4 Arms", 25.00, "Body Waxing"),
            com.example.data.model.ProductItem("Full Legs", 45.00, "Body Waxing"),
            com.example.data.model.ProductItem("1/2 Legs w Knee", 25.00, "Body Waxing"),
            com.example.data.model.ProductItem("1/2 Legs Below Knee", 20.00, "Body Waxing"),
            com.example.data.model.ProductItem("Back", 25.00, "Body Waxing"),
            com.example.data.model.ProductItem("1/2 Back", 20.00, "Body Waxing"),
            com.example.data.model.ProductItem("Stomach", 20.00, "Body Waxing"),
            com.example.data.model.ProductItem("Back of Nick", 15.00, "Body Waxing"),
            com.example.data.model.ProductItem("Sideburns", 15.00, "Facial Waxing"),
            com.example.data.model.ProductItem("Chun", 7.00, "Facial Waxing"),
            com.example.data.model.ProductItem("Upper Lips", 7.00, "Facial Waxing"),
            com.example.data.model.ProductItem("Neck", 7.00, "Facial Waxing"),
            com.example.data.model.ProductItem("Eyebrow", 12.00, "Tinting"),
            com.example.data.model.ProductItem("Eyelash", 20.00, "Tinting"),
            com.example.data.model.ProductItem("EBT+ELT", 30.00, "Tinting"),
            com.example.data.model.ProductItem("Henna (Herbal Henna + Oil)", 30.00, "Tinting"),
            com.example.data.model.ProductItem("Henna (Herbal Henna + Oil)", 25.00, "Tinting"),
            com.example.data.model.ProductItem("20min Clean Up (Cleanse, Scrub, Face Pack)", 20.00, "Herbal Facial (BYO)"),
            com.example.data.model.ProductItem("Hair Oil Massage (10 min)", 15.00, "Hair Care (BYO)"),
            com.example.data.model.ProductItem("Hand Henna", 10.00, "Hair Care (BYO)")
        )
    }

    companion object {
        private const val PREF_NAME = "business_reporting_prefs"
        private const val KEY_WEB_APP_URL = "google_apps_script_url"
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_ABN_ACN = "abn_acn"
        private const val KEY_BUSINESS_ADDRESS = "business_address"
        private const val KEY_PHONE_MOBILE = "phone_mobile"
        private const val KEY_EMAIL = "email_address"
        private const val KEY_CACHED_PRODUCTS = "cached_products_json"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
