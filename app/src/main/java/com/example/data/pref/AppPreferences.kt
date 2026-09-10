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
            com.example.data.model.ProductItem("Espresso / Coffee", 4.50, "Beverage"),
            com.example.data.model.ProductItem("Breakfast Combo", 12.50, "Food"),
            com.example.data.model.ProductItem("Lunch Special", 18.00, "Food"),
            com.example.data.model.ProductItem("Retail Goods", 25.00, "Goods"),
            com.example.data.model.ProductItem("Service / Labor", 60.00, "Service"),
            com.example.data.model.ProductItem("Wholesale Pack", 150.00, "Wholesale")
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
