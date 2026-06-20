package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class FirebaseAuthRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "returnSecureToken") val returnSecureToken: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FirebaseAuthResponse(
    @Json(name = "idToken") val idToken: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "expiresIn") val expiresIn: String? = null,
    @Json(name = "localId") val localId: String? = null
)

@JsonClass(generateAdapter = true)
data class FirebasePasswordResetRequest(
    @Json(name = "requestType") val requestType: String = "PASSWORD_RESET",
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class FirebasePasswordResetResponse(
    @Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class FirebaseUpdateProfileRequest(
    @Json(name = "idToken") val idToken: String,
    @Json(name = "password") val password: String? = null,
    @Json(name = "returnSecureToken") val returnSecureToken: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FirebaseUpdateProfileResponse(
    @Json(name = "localId") val localId: String? = null,
    @Json(name = "idToken") val idToken: String? = null
)

interface FirebaseAuthService {
    @POST("v1/accounts:signUp")
    suspend fun signUp(
        @Query("key") apiKey: String,
        @Body request: FirebaseAuthRequest
    ): FirebaseAuthResponse

    @POST("v1/accounts:signInWithPassword")
    suspend fun signIn(
        @Query("key") apiKey: String,
        @Body request: FirebaseAuthRequest
    ): FirebaseAuthResponse

    @POST("v1/accounts:sendOobCode")
    suspend fun sendPasswordResetEmail(
        @Query("key") apiKey: String,
        @Body request: FirebasePasswordResetRequest
    ): FirebasePasswordResetResponse

    @POST("v1/accounts:update")
    suspend fun updateProfile(
        @Query("key") apiKey: String,
        @Body request: FirebaseUpdateProfileRequest
    ): FirebaseUpdateProfileResponse
}

object FirebaseAuthClient {
    private const val BASE_URL = "https://identitytoolkit.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: FirebaseAuthService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FirebaseAuthService::class.java)
    }
}
