package com.njm.worker.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(val pin: String)

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("worker_id") val workerId: Int?,
    @SerializedName("worker_name") val workerName: String?,
    @SerializedName("org_id") val orgId: Int?
)

data class WorkerInfo(
    @SerializedName("worker_id") val workerId: Int,
    @SerializedName("worker_name") val workerName: String,
    @SerializedName("org_id") val orgId: Int,
    @SerializedName("org_name") val orgName: String?
)

data class Car(
    val id: Int,
    @SerializedName("plate_number") val plateNumber: String,
    @SerializedName("car_type") val carType: String,
    @SerializedName("organization_id") val orgId: Int,
    val price: Double?
)

data class SearchResponse(
    val success: Boolean,
    val cars: List<Car>?,
    val message: String?
)

data class WashRecord(
    val id: Int,
    @SerializedName("car_id") val carId: Int,
    @SerializedName("plate_number") val plateNumber: String?,
    @SerializedName("car_type") val carType: String?,
    @SerializedName("wash_date") val washDate: String?,
    val cost: Double?,
    @SerializedName("is_paid") val isPaid: Boolean?
)

data class WashResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("record_id") val recordId: Int?
)

data class TodayWashesResponse(
    val success: Boolean,
    val washes: List<WashRecord>?,
    val count: Int?
)