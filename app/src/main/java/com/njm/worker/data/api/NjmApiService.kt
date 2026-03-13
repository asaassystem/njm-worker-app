package com.njm.worker.data.api

import com.njm.worker.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface NjmApiService {

    @POST("api/worker-pin-login")
    suspend fun loginWithPin(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("api/worker/info")
    suspend fun getWorkerInfo(): Response<WorkerInfoResponse>

    @GET("api/worker/search-car")
    suspend fun searchCar(
        @Query("plate") plate: String
    ): Response<SearchResponse>

    @POST("api/worker/record-wash")
    suspend fun recordWash(
        @Body request: RecordWashRequest
    ): Response<WashResponse>

    @GET("api/worker/today-washes")
    suspend fun getTodayWashes(): Response<TodayWashesResponse>

    @POST("api/worker/logout")
    suspend fun logout(): Response<LogoutResponse>
}