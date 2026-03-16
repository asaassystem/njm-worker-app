package com.njm.worker.data.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * In-memory cookie jar for session cookies.
 * Cleared on logout to ensure clean session state.
 */
object AppCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies.clear()
        this.cookies.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.toList()

    fun clear() {
        cookies.clear()
    }
}

/**
 * ApiClient - NJM Worker App HTTP Client
 * v4.0: HTTPS enforced, BASIC logging only (no body - never logs PIN)
 * Developer: meshari.tech
 */
object ApiClient {

    private const val BASE_URL = "https://njm.company/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC // Never BODY - would log sensitive data
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(AppCookieJar)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: NjmApiService = retrofit.create(NjmApiService::class.java)
}
