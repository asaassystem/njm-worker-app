package com.njm.worker.data.api

import com.njm.worker.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface NjmApiService {

    @POST("api/worker-pin-login")
    @FormUrlEncoded
    suspend fun loginWithPin(
        @Field("pin") pin: String
    ): Response<LoginResponse>

    @GET("api/worker/info")
    suspend fun getWorkerInfo(): Response<WorkerInfo>

    @GET("api/worker/search-car")
    suspend fun searchCar(
        @Query("plate") plate: String
    ): Response<SearchResponse>

    @POST("api/worker/record-wash")
    @FormUrlEncoded
    suspend fun recordWash(
        @Field("car_id") carId: Int
    ): Response<WashResponse>

    @GET("api/worker/today-washes")
    suspend fun getTodayWashes(): Response<TodayWashesResponse>

    @POST("api/worker/logout")
    suspend fun logout(): Response<LoginResponse>
}