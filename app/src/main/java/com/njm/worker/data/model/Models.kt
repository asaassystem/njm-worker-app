package com.njm.worker.data.model

import com.google.gson.annotations.SerializedName

// Request models
data class LoginRequest(val pin: String, val lang: String = "ar")

data class RecordWashRequest(
    @SerializedName("car_id") val carId: Int,
    @SerializedName("is_paid") val isPaid: Int = 1,
    val notes: String = ""
)

// Response models
data class LoginResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("worker_id") val workerId: Int?,
    @SerializedName("worker_name") val workerName: String?,
    @SerializedName("org_id") val orgId: Int?,
    @SerializedName("org_name") val orgName: String?
)

data class WorkerInfoResponse(
    val success: Boolean,
    val name: String?,
    @SerializedName("org_id") val orgId: Int?
)

data class Car(
    val id: Int,
    @SerializedName("plate_number") val plateNumber: String,
    @SerializedName("car_type") val carType: String?,
    @SerializedName("organization_id") val orgId: Int?,
    @SerializedName("org_name") val orgName: String?,
    @SerializedName("wash_price") val washPrice: Double?,
    @SerializedName("wash_price_small") val washPriceSmall: Double?,
    @SerializedName("wash_price_large") val washPriceLarge: Double?
)

data class SearchResponse(
    val success: Boolean,
    val cars: List<Car>?,
    val message: String?
)

data class WashRecord(
    val id: Int,
    @SerializedName("plate_number") val plateNumber: String?,
    @SerializedName("car_type") val carType: String?,
    @SerializedName("org_name") val orgName: String?,
    @SerializedName("wash_time_fmt") val washTime: String?,
    @SerializedName("is_paid") val isPaid: Int?,
    val amount: Double?
)

data class WashResponse(
    val success: Boolean,
    val message: String?,
    val amount: Double?
)

data class TodayWashesResponse(
    val success: Boolean,
    val washes: List<WashRecord>?,
    val total: Int?,
    val message: String?
)

data class LogoutResponse(
    val success: Boolean,
    val message: String?
)