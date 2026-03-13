package com.njm.worker.data.repository

import com.njm.worker.data.api.ApiClient
import com.njm.worker.data.model.*

class WorkerRepository {
    private val api = ApiClient.apiService

    suspend fun login(pin: String): Result<LoginResponse> {
        return try {
            val r = api.loginWithPin(LoginRequest(pin = pin))
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
            else Result.failure(Exception(r.body()?.message ?: "Login failed: " + r.code()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCar(plate: String): Result<SearchResponse> {
        return try {
            val r = api.searchCar(plate)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
            else Result.failure(Exception(r.body()?.message ?: "Search failed: " + r.code()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordWash(carId: Int): Result<WashResponse> {
        return try {
            val r = api.recordWash(RecordWashRequest(carId = carId))
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
            else Result.failure(Exception(r.body()?.message ?: "Record failed: " + r.code()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTodayWashes(): Result<TodayWashesResponse> {
        return try {
            val r = api.getTodayWashes()
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
            else Result.failure(Exception("Failed: " + r.code()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Boolean> {
        return try {
            api.logout()
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }
}