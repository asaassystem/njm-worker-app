package com.njm.worker.data.repository

import com.njm.worker.data.api.ApiClient
import com.njm.worker.data.model.*

class WorkerRepository {
    private val api = ApiClient.apiService

    suspend fun login(pin: String): Result<LoginResponse> {
        return try {
            val resp = api.loginWithPin(LoginRequest(pin = pin))
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!)
            else Result.failure(Exception(resp.body()?.message ?: "Login failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getWorkerInfo(): Result<WorkerInfoResponse> {
        return try {
            val resp = api.getWorkerInfo()
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!)
            else Result.failure(Exception("Info failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun searchCar(plate: String): Result<SearchResponse> {
        return try {
            val resp = api.searchCar(plate)
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!)
            else Result.failure(Exception(resp.body()?.message ?: "Search failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun recordWash(carId: Int, isPaid: Int = 1, notes: String = ""): Result<WashResponse> {
        return try {
            val resp = api.recordWash(RecordWashRequest(carId = carId, isPaid = isPaid, notes = notes))
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!)
            else Result.failure(Exception(resp.body()?.message ?: "Record failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getTodayWashes(): Result<TodayWashesResponse> {
        return try {
            val resp = api.getTodayWashes()
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!)
            else Result.failure(Exception("Washes failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getSettings(): Result<SettingsResponse> {
        return try {
            val resp = api.getSettings()
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!)
            else Result.failure(Exception("Settings failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun logout(): Result<Boolean> {
        return try {
            api.logout()
            Result.success(true)
        } catch (e: Exception) { Result.success(true) }
    }
}