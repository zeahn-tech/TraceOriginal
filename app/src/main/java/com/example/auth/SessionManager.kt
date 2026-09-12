package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SessionManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private const val PREFS_NAME = "session_prefs"
    private const val KEY_USER_JSON = "user_json"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val userJson = prefs?.getString(KEY_USER_JSON, null)
        if (userJson != null) {
            try {
                val json = JSONObject(userJson)
                val user = User(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    email = json.getString("email"),
                    role = UserRole.valueOf(json.getString("role")),
                    profileImageUrl = if (json.has("profileImageUrl") && !json.isNull("profileImageUrl")) json.getString("profileImageUrl") else null,
                    isApproved = json.optBoolean("isApproved", true),
                    badgeNumber = if (json.has("badgeNumber") && !json.isNull("badgeNumber")) json.getString("badgeNumber") else null,
                    contact = if (json.has("contact") && !json.isNull("contact")) json.getString("contact") else null,
                    address = if (json.has("address") && !json.isNull("address")) json.getString("address") else null,
                    idCardUrl = if (json.has("idCardUrl") && !json.isNull("idCardUrl")) json.getString("idCardUrl") else null,
                    status = json.optString("status", "ACTIVE")
                )
                _currentUser.value = user
            } catch (e: Exception) {
                e.printStackTrace()
                // Clear corrupted session
                prefs?.edit()?.remove(KEY_USER_JSON)?.apply()
                _currentUser.value = null
            }
        }
    }

    fun login(user: User) {
        _currentUser.value = user
        saveUserToPrefs(user)
    }

    private fun saveUserToPrefs(user: User) {
        try {
            val json = JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("email", user.email)
                put("role", user.role.name)
                user.profileImageUrl?.let { put("profileImageUrl", it) }
                put("isApproved", user.isApproved)
                user.badgeNumber?.let { put("badgeNumber", it) }
                user.contact?.let { put("contact", it) }
                user.address?.let { put("address", it) }
                user.idCardUrl?.let { put("idCardUrl", it) }
                put("status", user.status)
            }
            prefs?.edit()?.putString(KEY_USER_JSON, json.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSessionUser(user: User) {
        if (_currentUser.value?.id == user.id) {
            _currentUser.value = user
            saveUserToPrefs(user)
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs?.edit()?.remove(KEY_USER_JSON)?.apply()
    }

    fun getGlobalPasswordExpiry(): Int {
        return prefs?.getInt("global_password_expiry", 90) ?: 90
    }

    fun setGlobalPasswordExpiry(days: Int) {
        prefs?.edit()?.putInt("global_password_expiry", days)?.apply()
    }

    fun getGlobalTwoFactorEnabled(): Boolean {
        return prefs?.getBoolean("global_two_factor_enabled", true) ?: true
    }

    fun setGlobalTwoFactorEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("global_two_factor_enabled", enabled)?.apply()
    }

    fun getPasswordLastChanged(userId: String): Long {
        val key = "password_last_changed_$userId"
        val lastChanged = prefs?.getLong(key, 0L) ?: 0L
        if (lastChanged == 0L) {
            val now = System.currentTimeMillis()
            prefs?.edit()?.putLong(key, now)?.apply()
            return now
        }
        return lastChanged
    }

    fun setPasswordLastChanged(userId: String, timestamp: Long) {
        prefs?.edit()?.putLong("password_last_changed_$userId", timestamp)?.apply()
    }

    fun isCitizen(): Boolean = _currentUser.value?.role == UserRole.CITIZEN
    fun isLawEnforcer(): Boolean = _currentUser.value?.role == UserRole.LAW_ENFORCER
    fun isAdmin(): Boolean = _currentUser.value?.role == UserRole.ADMIN
    fun isApproved(): Boolean = _currentUser.value?.isApproved ?: false
}
