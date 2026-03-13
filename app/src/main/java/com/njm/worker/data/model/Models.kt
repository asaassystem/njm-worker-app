package com.njm.worker.data.model

import com.google.gson.annotations.SerializedName

// ---- Request Models ----
data class LoginRequest(val pin: String, val lang: String = "ar")

data class RecordWashRequest(
    @SerializedName("car_id") val carId: Int,
    @SerializedName("is_paid") val isPaid: Int = 1,
    val notes: String = ""
)

data class UpdatePaymentRequest(
    @SerializedName("wash_id") val washId: Int,
    @SerializedName("is_paid") val isPaid: Int
)

// ---- Login / Session ----
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

// ---- Car Search ----
data class CarDetail(
    val id: Int,
    @SerializedName("plate_number") val plateNumber: String,
    @SerializedName("car_type") val carType: String?,
    @SerializedName("car_type_label") val carTypeLabel: String?,
    val color: String?,
    @SerializedName("org_name") val orgName: String?,
    @SerializedName("wash_price") val washPrice: Double?,
    @SerializedName("organization_id") val orgId: Int?
)

data class SearchResponse(
    val success: Boolean,
    val car: CarDetail?,
    val cars: List<CarDetail>?,
    val message: String?
)

// ---- Wash Record ----
data class WashResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("wash_id") val washId: Int?,
    val plate: String?,
    val cost: Double?
)

data class WashRecord(
    val id: Int,
    @SerializedName("plate_number") val plateNumber: String?,
    @SerializedName("car_type") val carType: String?,
    @SerializedName("org_name") val orgName: String?,
    val cost: Double?,
    @SerializedName("is_paid") val isPaid: Int?,
    @SerializedName("wash_date") val washDate: String?,
    @SerializedName("wash_time") val washTime: String?
)

data class TodayWashesResponse(
    val success: Boolean,
    val washes: List<WashRecord>?,
    val message: String?
)

data class MonthWashesResponse(
    val success: Boolean,
    val washes: List<WashRecord>?,
    val message: String?
)

data class UpdatePaymentResponse(
    val success: Boolean,
    val message: String?
)

// ---- Settings / Org ----
data class OrgInfo(
    val id: Int?,
    val name: String?
)

data class SettingsResponse(
    val success: Boolean,
    val org: OrgInfo?
)

// ---- Logout ----
data class LogoutResponse(
    val success: Boolean,
    val message: String?
)
