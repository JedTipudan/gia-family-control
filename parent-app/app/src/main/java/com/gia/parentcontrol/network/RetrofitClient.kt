package com.gia.parentcontrol.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.gia.parentcontrol.model.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type
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

    @GET("api/schedules")
    suspend fun getSchedules(): Response<List<ScheduledLock>>

    @POST("api/schedules")
    suspend fun createSchedule(@Body body: ScheduledLock): Response<ScheduledLock>

    @PUT("api/schedules/{id}")
    suspend fun updateSchedule(@Path("id") id: Long, @Body body: ScheduledLock): Response<ScheduledLock>

    @DELETE("api/schedules/{id}")
    suspend fun deleteSchedule(@Path("id") id: Long): Response<Void>
}

object RetrofitClient {

    private const val BASE_URL = "https://gia-family-control-backend.onrender.com/"

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            object : TypeToken<List<DeviceResponse>>() {}.type,
            object : JsonDeserializer<List<DeviceResponse>> {
                override fun deserialize(json: JsonElement, t: Type, ctx: JsonDeserializationContext) =
                    json.asJsonArray.map { ctx.deserialize<DeviceResponse>(it, DeviceResponse::class.java) }
            }
        )
        .registerTypeAdapter(
            object : TypeToken<List<AppControlResponse>>() {}.type,
            object : JsonDeserializer<List<AppControlResponse>> {
                override fun deserialize(json: JsonElement, t: Type, ctx: JsonDeserializationContext) =
                    json.asJsonArray.map { ctx.deserialize<AppControlResponse>(it, AppControlResponse::class.java) }
            }
        )
        .registerTypeAdapter(
            object : TypeToken<List<AppResponse>>() {}.type,
            object : JsonDeserializer<List<AppResponse>> {
                override fun deserialize(json: JsonElement, t: Type, ctx: JsonDeserializationContext) =
                    json.asJsonArray.map { ctx.deserialize<AppResponse>(it, AppResponse::class.java) }
            }
        )
        .create()

    fun create(context: Context): ApiService {
        val appContext = context.applicationContext

        val authInterceptor = Interceptor { chain ->
            val token = appContext
                .getSharedPreferences("parent_prefs", Context.MODE_PRIVATE)
                .getString("jwt_token_plain", null)
            val request = if (token != null)
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            else chain.request()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
