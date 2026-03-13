package com.njm.worker.data.repository

import com.njm.worker.data.api.ApiClient
import com.njm.worker.data.model.*

class WorkerRepository {
    private val api = ApiClient.apiService

    suspend fun loginWithPin(pin: String): Result<LoginResponse> {
        return try {
            val r = api.loginWithPin(pin)
            if (r.isSuccessful && r.body() != null) {
                val body = r.body()!!
                if (body.success) Result.success(body)
                else Result.failure(Exception(body.message ?: "Login failed"))
            } else {
                Result.failure(Exception("Network error: " + r.code()))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getWorkerInfo(): Result<WorkerInfo> {
        return try {
            val r = api.getWorkerInfo()
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
            else Result.failure(Exception("Failed to get info"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun searchCar(plate: String): Result<SearchResponse> {
        return try {
            val r = api.searchCar(plate)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
            else Result.failure(Exception("Search failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun recordWash(carId: Int): Result<WashResponse> {
        return try {
            val r = api.recordWash(carId)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
            else Result.failure(Exception("Failed to record wash"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getTodayWashes(): Result<TodayWashesResponse> {
        return try {
            val r = api.getTodayWashes()
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
            else Result.failure(Exception("Failed to get washes"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun logout(): Result<Unit> {
        return try { api.logout(); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }
}