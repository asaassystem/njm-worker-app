package com.njm.worker.data.model

import com.google.gson.annotations.SerializedName

data class PinLoginRequest(
    val pin: String,
    val lang: String = "ar"
)

data class PinLoginResponse(
    val success: Boolean,
    val message: String? = null
)

data class WorkerInfoResponse(
    val success: Boolean,
    val name: String = "",
    val org_id: Int? = null
)

data class Car(
    val id: Int,
    val plate_number: String,
    val car_type: String,
    val car_type_label: String,
    val color: String,
    val org_name: String,
    val price: Double
)

data class SearchCarResponse(
    val success: Boolean,
    val cars: List<Car>? = null,
    val message: String? = null
)

data class RecordWashRequest(
    val car_id: Int,
    val is_paid: Int = 1,
    val notes: String = ""
)

data class RecordWashResponse(
    val success: Boolean,
    val message: String = "",
    val cost: Double = 0.0,
    val wash_id: Int = 0
)

data class WashRecord(
    val id: Int,
    val plate_number: String,
    val car_type: String,
    val org_name: String,
    val cost: Double,
    val is_paid: Int,
    val wash_time_fmt: String
)

data class TodayWashesResponse(
    val success: Boolean,
    val washes: List<WashRecord>? = null,
    val total: Int = 0,
    val total_amount: Double = 0.0
)

data class BaseResponse(
    val success: Boolean,
    val message: String? = null
)