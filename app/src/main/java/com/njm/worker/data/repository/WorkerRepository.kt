package com.njm.worker.data.repository

import com.njm.worker.data.api.ApiClient
import com.njm.worker.data.model.*
import okhttp3.ResponseBody

/**
 * WorkerRepository v6.1
 * FIX: error body read for non-2xx responses so real errors surface correctly.
 */
class WorkerRepository {
    private val api = ApiClient.apiService

    private fun errorMsg(errorBody: ResponseBody?, fallback: String): String {
        return try {
            val raw = errorBody?.string() ?: return fallback
            val match = Regex('\\u0022message\\u0022\\s*:\\s*\\u0022([^\\u0022]+)\\u0022').find(raw)
            match?.groupValues?.getOrNull(1) ?: fallback
        } catch (e: Exception) { fallback }
    }

    suspend fun login(pin: String): Result<LoginResponse> = try {
        val r = api.loginWithPin(LoginRequest(pin = pin))
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception(errorMsg(r.errorBody(), r.body()?.message ?: "Login failed")))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getWorkerInfo(): Result<WorkerInfoResponse> = try {
        val r = api.getWorkerInfo()
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception("Info failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun searchCar(plate: String): Result<SearchResponse> = try {
        val r = api.searchCar(plate)
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception(errorMsg(r.errorBody(), r.body()?.message ?: "Search failed")))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun recordWash(carId: Int, isPaid: Int = 1, notes: String = "", lang: String = "ar"): Result<WashResponse> = try {
        val r = api.recordWash(RecordWashRequest(carId = carId, isPaid = isPaid, notes = notes, lang = lang))
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception(errorMsg(r.errorBody(), r.body()?.message ?: "Record failed")))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getTodayWashes(): Result<TodayWashesResponse> = try {
        val r = api.getTodayWashes()
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception(errorMsg(r.errorBody(), "Washes failed")))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getMonthWashes(): Result<MonthWashesResponse> = try {
        val r = api.getMonthWashes()
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception(errorMsg(r.errorBody(), "Month washes failed")))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updatePayment(washId: Int, isPaid: Int): Result<UpdatePaymentResponse> = try {
        val r = api.updatePayment(UpdatePaymentRequest(washId = washId, isPaid = isPaid))
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception("Update payment failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getInvoices(): Result<InvoiceListResponse> = try {
        val r = api.getInvoices()
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception(errorMsg(r.errorBody(), "Invoices failed")))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun createInvoice(periodStart: String, periodEnd: String): Result<CreateInvoiceResponse> = try {
        val r = api.createInvoice(CreateInvoiceRequest(periodStart = periodStart, periodEnd = periodEnd))
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception(errorMsg(r.errorBody(), r.body()?.message ?: "Create invoice failed")))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getSettings(): Result<SettingsResponse> = try {
        val r = api.getSettings()
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception("Settings failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateLanguage(lang: String): Result<SimpleResponse> = try {
        val r = api.updateLanguage(UpdateLangRequest(lang = lang))
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.success(SimpleResponse(true, "ok"))
    } catch (e: Exception) { Result.success(SimpleResponse(true, "ok")) }

    suspend fun logout(): Result<Boolean> = try {
        api.logout()
        Result.success(true)
    } catch (e: Exception) { Result.success(true) }
}
