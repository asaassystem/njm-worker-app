package com.njm.worker.data.api

import com.njm.worker.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface NjmApiService {

    // ---- Auth ----
    @POST("api/worker-pin-login")
    suspend fun loginWithPin(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/worker/logout")
    suspend fun logout(): Response<LogoutResponse>

    // ---- Worker Info ----
    @GET("api/worker/info")
    suspend fun getWorkerInfo(): Response<WorkerInfoResponse>

    // ---- Car Search ----
    @GET("api/worker/search-car")
    suspend fun searchCar(@Query("plate") plate: String): Response<SearchResponse>

    // ---- Wash Operations ----
    @POST("api/worker/record-wash")
    suspend fun recordWash(@Body request: RecordWashRequest): Response<WashResponse>

    @GET("api/worker/today-washes")
    suspend fun getTodayWashes(): Response<TodayWashesResponse>

    @GET("api/worker/month-washes")
    suspend fun getMonthWashes(): Response<MonthWashesResponse>

    @POST("api/worker/update-payment")
    suspend fun updatePayment(@Body request: UpdatePaymentRequest): Response<UpdatePaymentResponse>

    // ---- Invoice Operations ----
    @GET("api/worker/invoices")
    suspend fun getInvoices(): Response<InvoiceListResponse>

    @POST("api/worker/create-invoice")
    suspend fun createInvoice(@Body request: CreateInvoiceRequest): Response<CreateInvoiceResponse>

    // ---- Settings ----
    @GET("api/worker/settings")
    suspend fun getSettings(): Response<SettingsResponse>

    @POST("api/worker/update-lang")
    suspend fun updateLanguage(@Body request: UpdateLangRequest): Response<SimpleResponse>
}
