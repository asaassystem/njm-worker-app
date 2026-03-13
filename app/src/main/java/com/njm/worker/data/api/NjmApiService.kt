package com.njm.worker.data.api

import com.njm.worker.data.model.*
import retrofit2.http.*

interface NjmApiService {

    @POST("api/worker-pin-login")
    suspend fun pinLogin(@Body request: PinLoginRequest): PinLoginResponse

    @GET("api/worker/info")
    suspend fun getWorkerInfo(): WorkerInfoResponse

    @GET("api/worker/search-car")
    suspend fun searchCar(@Query("plate") plate: String): SearchCarResponse

    @POST("api/worker/record-wash")
    suspend fun recordWash(@Body request: RecordWashRequest): RecordWashResponse

    @GET("api/worker/today-washes")
    suspend fun getTodayWashes(): TodayWashesResponse

    @POST("api/worker/logout")
    suspend fun logout(): BaseResponse
}