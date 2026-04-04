package com.gia.familycontrol.network

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val Context.dataStore by preferencesDataStore(name = "gia_prefs")
val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")

object RetrofitClient {

    private const val BASE_URL = "https://gia-family-control-production.up.railway.app/"

    fun create(context: Context): ApiService {
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking {
                context.dataStore.data.first()[JWT_TOKEN_KEY]
            }
            
            android.util.Log.d("RetrofitClient", "=== API REQUEST ===")
            android.util.Log.d("RetrofitClient", "URL: ${chain.request().url}")
            android.util.Log.d("RetrofitClient", "Method: ${chain.request().method}")
            android.util.Log.d("RetrofitClient", "Token: ${if (token != null) "${token.substring(0, minOf(30, token.length))}..." else "NULL"}")
            
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                android.util.Log.e("RetrofitClient", "NO TOKEN FOUND - Request will fail!")
                chain.request()
            }
            
            val response = chain.proceed(request)
            android.util.Log.d("RetrofitClient", "Response Code: ${response.code}")
            
            if (response.code == 403) {
                android.util.Log.e("RetrofitClient", "403 FORBIDDEN - Token invalid or expired!")
            }
            
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
