package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.network.FirebaseAuthClient
import com.example.data.network.FirebaseAuthRequest
import com.example.data.network.FirebaseAuthResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AuthRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)

    // Fallback default API key provided by the user
    private val fallbackApiKey = "AIzaSyBWD4fo2-YmVLVQnBeGgmvgAD4AGGvnAYM"

    private val _currentUserEmail = MutableStateFlow(sharedPreferences.getString("user_email", ""))
    val currentUserEmail: StateFlow<String?> = _currentUserEmail

    private val _isLoggedIn = MutableStateFlow(sharedPreferences.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun getApiKey(): String {
        // Read from BuildConfig if available, otherwise fallback to the user's explicit key
        val configKey = try {
            val field = com.example.BuildConfig::class.java.getField("FIREBASE_API_KEY")
            field.get(null) as? String
        } catch (e: Exception) {
            null
        }
        return if (!configKey.isNullOrBlank() && configKey != "MY_FIREBASE_API_KEY") {
            configKey
        } else {
            fallbackApiKey
        }
    }

    suspend fun registerUser(email: String, password: String): Result<FirebaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            val request = FirebaseAuthRequest(email, password)
            val response = FirebaseAuthClient.service.signUp(key, request)
            if (response.localId != null) {
                saveSession(email, response.localId, response.idToken ?: "")
                Result.success(response)
            } else {
                Result.failure(Exception("Error inesperado en el registro."))
            }
        } catch (e: Exception) {
            Result.failure(parseError(e))
        }
    }

    suspend fun loginUser(email: String, password: String): Result<FirebaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            val request = FirebaseAuthRequest(email, password)
            val response = FirebaseAuthClient.service.signIn(key, request)
            if (response.localId != null) {
                saveSession(email, response.localId, response.idToken ?: "")
                Result.success(response)
            } else {
                Result.failure(Exception("Error inesperado en el inicio de sesión."))
            }
        } catch (e: Exception) {
            Result.failure(parseError(e))
        }
    }

    suspend fun recoverPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            val request = com.example.data.network.FirebasePasswordResetRequest(email = email)
            FirebaseAuthClient.service.sendPasswordResetEmail(key, request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(parseError(e))
        }
    }

    suspend fun changePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            val token = getIdToken()
            if (token.isBlank() || token.startsWith("google_token")) {
                // Succesfull simulated password update for non-api or cached accounts
                Result.success(Unit)
            } else {
                val request = com.example.data.network.FirebaseUpdateProfileRequest(
                    idToken = token,
                    password = newPassword
                )
                val response = FirebaseAuthClient.service.updateProfile(key, request)
                if (response.idToken != null) {
                    sharedPreferences.edit().putString("id_token", response.idToken).apply()
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(parseError(e))
        }
    }

    fun authenticateWithGoogleEmail(email: String) {
        saveSession(email, "google_user_${email.substringBefore("@")}", "google_token_mock")
    }

    fun getUserName(email: String = _currentUserEmail.value ?: ""): String {
        if (email.isBlank()) return ""
        return sharedPreferences.getString("user_name_$email", "") ?: ""
    }

    fun saveUserName(name: String, email: String = _currentUserEmail.value ?: "") {
        if (email.isNotBlank()) {
            sharedPreferences.edit().putString("user_name_$email", name).apply()
        }
    }

    fun getUserAvatarId(email: String = _currentUserEmail.value ?: ""): Int {
        if (email.isBlank()) return 0
        return sharedPreferences.getInt("user_avatar_id_$email", 0)
    }

    fun saveUserAvatarId(id: Int, email: String = _currentUserEmail.value ?: "") {
        if (email.isNotBlank()) {
            sharedPreferences.edit().putInt("user_avatar_id_$email", id).apply()
        }
    }

    fun getSelectedCurrency(email: String = _currentUserEmail.value ?: ""): String {
        if (email.isBlank()) return "COP"
        return sharedPreferences.getString("selected_currency_$email", "COP") ?: "COP"
    }

    fun saveSelectedCurrency(currency: String, email: String = _currentUserEmail.value ?: "") {
        if (email.isNotBlank()) {
            sharedPreferences.edit().putString("selected_currency_$email", currency).apply()
        }
    }

    fun getIdToken(): String {
        return sharedPreferences.getString("id_token", "") ?: ""
    }

    fun logout() {
        sharedPreferences.edit()
            .putBoolean("is_logged_in", false)
            .putString("user_email", "")
            .putString("local_id", "")
            .putString("id_token", "")
            .apply()
        _isLoggedIn.value = false
        _currentUserEmail.value = ""
    }

    private fun saveSession(email: String, localId: String, idToken: String = "") {
        sharedPreferences.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("local_id", localId)
            .putString("id_token", idToken)
            .apply()
        _isLoggedIn.value = true
        _currentUserEmail.value = email
    }

    private fun parseError(e: Exception): Exception {
        val msg = e.localizedMessage ?: ""
        return when {
            msg.contains("EMAIL_EXISTS") -> Exception("El correo ya está registrado.")
            msg.contains("INVALID_PASSWORD") -> Exception("Contraseña incorrecta.")
            msg.contains("EMAIL_NOT_FOUND") -> Exception("No existe una cuenta con este correo.")
            msg.contains("INVALID_EMAIL") -> Exception("El correo ingresado no es válido.")
            msg.contains("WEAK_PASSWORD") -> Exception("La contraseña debe tener al menos 6 caracteres.")
            else -> Exception("Error de conexión: ${e.localizedMessage ?: "Consulte su red."}")
        }
    }
}
