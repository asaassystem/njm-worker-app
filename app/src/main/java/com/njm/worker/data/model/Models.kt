package com.njm.worker.data.model

import com.google.gson.annotations.SerializedName

// ---- Requests ----
data class LoginRequest(val pin: String, val lang: String = "ar")

data class RecordWashRequest(
    @SerializedName("car_id") val carId: Int,
    @SerializedName("is_paid") val isPaid: Int = 1,
    val notes: String = "",
    val lang: String = "ar"
)

data class UpdatePaymentRequest(
    @SerializedName("wash_id") val washId: Int,
    @SerializedName("is_paid") val isPaid: Int
)

data class UpdateLangRequest(val lang: String)

data class CreateInvoiceRequest(
    @SerializedName("period_start") val periodStart: String,
    @SerializedName("period_end") val periodEnd: String
)

// ---- Login ----
data class LoginResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("worker_id") val workerId: Int?,
    @SerializedName("worker_name") val workerName: String?,
    @SerializedName("org_id") val orgId: Int?,
    @SerializedName("org_name") val orgName: String?,
    val lang: String?
)

data class WorkerInfoResponse(
    val success: Boolean,
    val name: String?,
    @SerializedName("org_id") val orgId: Int?,
    val lang: String?
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
    @SerializedName("organization_id") val orgId: Int?,
    @SerializedName("owner_name") val ownerName: String?,
    @SerializedName("owner_phone") val ownerPhone: String?,
    @SerializedName("car_brand") val carBrand: String?,
    @SerializedName("car_model") val carModel: String?,
    @SerializedName("model_year") val modelYear: String?
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
    val cost: Double?,
    @SerializedName("invoice_number") val invoiceNumber: String?,
        @SerializedName("is_paid") val isPaid: Int?
)

data class WashRecord(
    val id: Int,
    @SerializedName("plate_number") val plateNumber: String?,
    @SerializedName("car_type") val carType: String?,
    @SerializedName("org_name") val orgName: String?,
    val cost: Double?,
    @SerializedName("is_paid") val isPaid: Int?,
    @SerializedName("wash_date") val washDate: String?,
    @SerializedName("wash_time") val washTime: String?,
    @SerializedName("owner_name") val ownerName: String?,
    @SerializedName("owner_phone") val ownerPhone: String?
)

data class TodayWashesResponse(
    val success: Boolean,
    val washes: List<WashRecord>?,
    val message: String?,
    val total: Double?,
    val paid: Double?,
    val unpaid: Double?,
    val count: Int?
)

data class MonthWashesResponse(
    val success: Boolean,
    val washes: List<WashRecord>?,
    val message: String?,
    val total: Double?,
    val paid: Double?,
    val unpaid: Double?,
    val count: Int?
)

data class UpdatePaymentResponse(val success: Boolean, val message: String?)

// ---- Invoice Models ----
data class Invoice(
    val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("period_start") val periodStart: String?,
    @SerializedName("period_end") val periodEnd: String?,
    @SerializedName("total_washes") val totalWashes: Int?,
    @SerializedName("total_amount") val totalAmount: Double?,
    @SerializedName("vat_amount") val vatAmount: Double?,
    @SerializedName("grand_total") val grandTotal: Double?,
    val status: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("invoice_date") val invoiceDate: String?,
    @SerializedName("org_name") val orgName: String?
)

data class InvoiceListResponse(
    val success: Boolean,
    val invoices: List<Invoice>?,
    val message: String?
)

data class CreateInvoiceResponse(
    val success: Boolean,
    val message: String?,
    val invoice: Invoice?
)

// ---- Settings ----
data class OrgInfo(val id: Int?, val name: String?, val vat: String?, val address: String?, val phone: String?)

data class SettingsResponse(val success: Boolean, val org: OrgInfo?)

// ---- General ----
data class LogoutResponse(val success: Boolean, val message: String?)
data class SimpleResponse(val success: Boolean, val message: String?)
