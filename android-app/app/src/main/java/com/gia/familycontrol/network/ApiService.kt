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
    suspend fun updateDeviceStatus(@Body update: DeviceStatusUpdate): Response<Unit>

    @POST("api/location/update")
    suspend fun updateLocation(@Body request: LocationUpdateRequest): Response<Unit>

    @GET("api/location/{deviceId}/latest")
    suspend fun getLatestLocation(@Path("deviceId") deviceId: Long): Response<LocationResponse>

    @GET("api/location/{deviceId}/history")
    suspend fun getLocationHistory(
        @Path("deviceId") deviceId: Long,
        @Query("limit") limit: Int = 100
    ): Response<List<LocationResponse>>

    @POST("api/send-command")
    suspend fun sendCommand(@Body request: SendCommandRequest): Response<CommandResponse>

    @GET("api/apps/controls/{deviceId}")
    suspend fun getAppControls(@Path("deviceId") deviceId: Long): Response<List<AppControlResponse>>

    @POST("api/apps/control")
    suspend fun setAppControl(@Body request: AppControlRequest): Response<AppControlResponse>

    @DELETE("api/apps/control/{deviceId}/{packageName}")
    suspend fun removeAppControl(
        @Path("deviceId") deviceId: Long,
        @Path("packageName") packageName: String
    ): Response<Unit>
}
