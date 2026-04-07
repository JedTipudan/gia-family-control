package com.gia.parentcontrol.network

import android.content.Context
import com.gia.parentcontrol.model.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @PUT("api/device/status")
    suspend fun updateDeviceStatus(@Body update: DeviceStatusUpdate): Response<Void>

    @GET("api/location/{deviceId}/latest")
    suspend fun getLatestLocation(@Path("deviceId") deviceId: Long): Response<LocationResponse>

    @POST("api/send-command")
    suspend fun sendCommand(@Body request: SendCommandRequest): Response<CommandResponse>

    @POST("api/unpair-device/{deviceId}")
    suspend fun unpairDevice(@Path("deviceId") deviceId: Long): Response<Void>

    @GET("api/apps/controls/{deviceId}")
    suspend fun getAppControls(@Path("deviceId") deviceId: Long): Response<List<AppControlResponse>>

    @POST("api/apps/control")
    suspend fun setAppControl(@Body request: AppControlRequest): Response<AppControlResponse>

    @DELETE("api/apps/control/{deviceId}/{packageName}")
    suspend fun removeAppControl(
        @Path("deviceId") deviceId: Long,
        @Path("packageName") packageName: String
    ): Response<Void>

    @GET("api/apps/installed/{deviceId}")
    suspend fun getInstalledApps(@Path("deviceId") deviceId: Long): Response<List<AppResponse>>

    @GET("api/users/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: Long): Response<UserProfileResponse>

    @GET("api/child-devices")
    suspend fun getChildDevices(): Response<List<DeviceResponse>>
}

object RetrofitClient {

    private const val BASE_URL = "https://gia-family-control-production.up.railway.app/"

    fun create(context: Context): ApiService {
        val appContext = context.applicationContext

        val authInterceptor = Interceptor { chain ->
            val token = appContext
                .getSharedPreferences("parent_prefs", Context.MODE_PRIVATE)
                .getString("jwt_token_plain", null)
            val request = if (token != null)
                chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
            else chain.request()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
