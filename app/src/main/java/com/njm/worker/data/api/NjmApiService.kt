package com.njm.worker.data.api

import com.njm.worker.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface NjmApiService {

    // Login
    @POST("api/worker-pin-login")
    suspend fun loginWithPin(@Body request: LoginRequest): Response<LoginResponse>

    // Worker info (uses session)
    @GET("api/worker/info")
    suspend fun getWorkerInfo(): Response<WorkerInfoResponse>

    // Search car by plate
    @GET("api/worker/search-car")
    suspend fun searchCar(@Query("plate") plate: String): Response<SearchResponse>

    // Record wash
    @POST("api/worker/record-wash")
    suspend fun recordWash(@Body request: RecordWashRequest): Response<WashResponse>

    // Today's washes
    @GET("api/worker/today-washes")
    suspend fun getTodayWashes(): Response<TodayWashesResponse>

    // Month's washes
    @GET("api/worker/month-washes")
    suspend fun getMonthWashes(): Response<MonthWashesResponse>

    // Update payment status
    @POST("api/worker/update-payment")
    suspend fun updatePayment(@Body request: UpdatePaymentRequest): Response<UpdatePaymentResponse>

    // Settings / org info
    @GET("api/worker/settings")
    suspend fun getSettings(): Response<SettingsResponse>

    // Logout
    @POST("api/worker/logout")
    suspend fun logout(): Response<LogoutResponse>
}
