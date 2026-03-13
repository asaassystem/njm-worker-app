package com.njm.worker.data.repository

import com.njm.worker.data.api.ApiClient
import com.njm.worker.data.api.NjmApiService
import com.njm.worker.data.model.*
import com.njm.worker.utils.SessionManager

class WorkerRepository {
    private val api: NjmApiService = ApiClient.retrofit.create(NjmApiService::class.java)

    suspend fun login(pin: String): Result<Boolean> = runCatching {
        val response = api.pinLogin(PinLoginRequest(pin))
        if (response.success) {
            try {
                val info = api.getWorkerInfo()
                SessionManager.isLoggedIn = true
                SessionManager.workerName = info.name
                SessionManager.orgId = info.org_id ?: 0
            } catch (e: Exception) {
                SessionManager.isLoggedIn = true
            }
            true
        } else {
            throw Exception(response.message ?: "PIN غير صحيح")
        }
    }

    suspend fun searchCar(plate: String): Result<List<Car>> = runCatching {
        val response = api.searchCar(plate)
        response.cars ?: emptyList()
    }

    suspend fun recordWash(carId: Int, isPaid: Int = 1, notes: String = ""): Result<RecordWashResponse> = runCatching {
        api.recordWash(RecordWashRequest(carId, isPaid, notes))
    }

    suspend fun getTodayWashes(): Result<TodayWashesResponse> = runCatching {
        api.getTodayWashes()
    }

    suspend fun logout(): Result<Unit> = runCatching {
        try { api.logout() } catch (e: Exception) { /* ignore */ }
        SessionManager.clear()
        ApiClient.clearSession()
    }
}