package com.gia.familycontrol.network

import com.gia.familycontrol.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/pair-device")
    suspend fun pairDevice(@Body request: PairRequest): Response<DeviceResponse>

    @PUT("api/device/status")
    suspend fun updateDeviceStatus(@Body update: DeviceStatusUpdate): Response<Void>

    @POST("api/location/update")
    suspend fun updateLocation(@Body request: LocationUpdateRequest): Response<Void>

    @GET("api/location/{deviceId}/latest")
    suspend fun getLatestLocation(@Path("deviceId") deviceId: Long): Response<LocationResponse>

    @GET("api/location/{deviceId}/history")
    suspend fun getLocationHistory(
        @Path("deviceId") deviceId: Long,
        @Query("limit") limit: Int = 100
    ): Response<List<LocationResponse>>

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

    @POST("api/apps/sync")
    suspend fun syncApps(@Body apps: List<AppInfo>): Response<Void>

    @GET("api/users/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: Long): Response<UserProfileResponse>

    @GET("api/child-devices")
    suspend fun getChildDevices(): Response<List<DeviceResponse>>
}
