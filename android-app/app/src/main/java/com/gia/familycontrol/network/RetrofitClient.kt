package com.gia.familycontrol.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.gia.familycontrol.model.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://gia-family-control-production.up.railway.app/"

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            object : TypeToken<List<DeviceResponse>>() {}.type,
            com.google.gson.JsonDeserializer { json, _, ctx ->
                json.asJsonArray.map { ctx.deserialize<DeviceResponse>(it, DeviceResponse::class.java) }
            }
        )
        .registerTypeAdapter(
            object : TypeToken<List<AppControlResponse>>() {}.type,
            com.google.gson.JsonDeserializer { json, _, ctx ->
                json.asJsonArray.map { ctx.deserialize<AppControlResponse>(it, AppControlResponse::class.java) }
            }
        )
        .registerTypeAdapter(
            object : TypeToken<List<AppResponse>>() {}.type,
            com.google.gson.JsonDeserializer { json, _, ctx ->
                json.asJsonArray.map { ctx.deserialize<AppResponse>(it, AppResponse::class.java) }
            }
        )
        .registerTypeAdapter(
            object : TypeToken<List<LocationResponse>>() {}.type,
            com.google.gson.JsonDeserializer { json, _, ctx ->
                json.asJsonArray.map { ctx.deserialize<LocationResponse>(it, LocationResponse::class.java) }
            }
        )
        .create()

    fun create(context: Context): ApiService {
        val authInterceptor = Interceptor { chain ->
            val token = context.getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
                .getString("jwt_token", null)
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
