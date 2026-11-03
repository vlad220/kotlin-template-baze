package com.template.data.network

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiErrorData?
)

data class ApiErrorData(
    val status: Int,
    val message: String
)

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class SendCodeRequest(val phone: String)
data class VerifyCodeRequest(val phone: String, val code: String)
data class SmsResponse(val message: String, val token: String? = null, val code: String? = null)
data class AuthResponse(val token: String, val user: UserDto)
data class UserDto(val id: String, val name: String, val email: String)
data class FeedbackRequest(val app: String, val type: String, val title: String = "", val description: String)
data class FeedbackResponse(val success: Boolean)
data class ProductDto(
    val id: String? = null,
    val name: String,
    val barcode: String? = null,
    val price: Double? = null,
    val quantity: Double? = null
)

// --- Битрикс24 ---
data class Bitrix24ConnectRequest(val webhook_url: String)
data class Bitrix24StatusData(
    val is_active: Boolean?,
    val portal_domain: String?,
    val connected_at: String?
)

// --- 1С ---
data class OneCConnectRequest(
    val server_url: String,
    val api_key: String? = null,
    val username: String? = null,
    val password: String? = null,
    val use_password: Boolean = false,
    val sync_options: List<String>? = null
)
data class OneCStatusData(
    val is_active: Boolean?,
    val server_url: String?,
    val last_sync_at: String?
)
data class SyncOption(val id: String, val label: String)
data class SyncOptionsData(val options: List<SyncOption>?)

// --- Пользователи ---
data class UserSearchResult(
    val id: String,
    val name: String,
    val phone: String?,
    val avatarUrl: String?
)

// --- AI-Такси ---
data class DriverProfile(
    val user_id: String? = null,
    val city: String? = null,
    val car_model: String? = null,
    val car_color: String? = null,
    val plate: String? = null,
    val photo_url: String? = null,
    val rate_per_km: Int? = null,
    val search_radius_km: Int? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val is_online: Boolean? = null,
    val is_active: Boolean? = null,
    val show_rating: Boolean? = null,
    val cancel_count: Int? = null,
    val rating: Double? = null,
    val review_count: Int? = null
)

data class SaveDriverRequest(
    val city: String? = null,
    val car_model: String? = null,
    val car_color: String? = null,
    val plate: String? = null,
    val photo_url: String? = null,
    val rate_per_km: Int? = null,
    val search_radius_km: Int? = null,
    val show_rating: Boolean? = null
)

data class LocationRequest(val lat: Double, val lon: Double, val speed: Float? = null, val heading: Float? = null)

// Ответ GET /taxi/route: длину и время считает сервер, цену — только подсказка
data class RouteInfo(
    val distance_m: Int = 0,
    val duration_min: Int = 0,
    val straight_m: Long = 0,
    val distance_source: String? = null,
    val approx: Boolean = false,
    val median_rate_per_km: Double = 0.0,
    val drivers_nearby: Int = 0,
    val suggested_price: Int? = null
)

// Сервер до приёма заказа отдаёт только агрегаты — без имён, номеров и координат водителей
data class NearbySummary(
    val drivers_online: Int,
    val median_rate_per_km: Double,
    val nearest_m: Double? = null
)

data class CreateOrderRequest(
    val to_address: String? = null,
    val to_lat: Double? = null,
    val to_lon: Double? = null,
    val from_address: String? = null,
    val from_lat: Double? = null,
    val from_lon: Double? = null,
    val distance_m: Int? = null,
    val duration_min: Int? = null,
    val price: Int,
    val price_source: String? = null,
    val comment: String? = null,
    val raw_text: String? = null,
    // RFC 3339 со смещением: «подать к 06:30 11 октября». Нет значения — ехать сейчас.
    val scheduled_at: String? = null
)

data class OrderDto(
    val id: String? = null,
    val from_address: String? = null,
    val to_address: String? = null,
    val from_lat: Double? = null,
    val from_lon: Double? = null,
    val to_lat: Double? = null,
    val to_lon: Double? = null,
    val distance_m: Int? = null,
    val duration_min: Int? = null,
    val price: Int? = null,
    // Цена, которую предложил водитель; сервер чистит её, когда клиент соглашается
    val counter_price: Int? = null,
    val status: String? = null,
    val created_at: String? = null,
    // Миллисекунды эпохи: сервер отдаёт время подачи, назначенное клиентом
    val scheduled_at_ms: Long? = null,
    val driver_name: String? = null,
    val plate: String? = null,
    val car_model: String? = null,
    val car_color: String? = null,
    val price_per_km: Double? = null,
    // Где машина водителя: сервер отдаёт их, только когда заказ кем-то взят
    val driver_lat: Double? = null,
    val driver_lon: Double? = null
)

data class CounterPriceRequest(val price: Int)
data class UpdateStatusRequest(val status: String)
data class CancelOrderRequest(val reason: String? = null)
data class ReviewRequest(val rating: Int, val text: String? = null)

data class TaxiStats(
    val role: String? = null,
    val rides: Long = 0,
    val total_rub: Long = 0,
    val total_km: Long = 0,
    val avg_duration_min: Double = 0.0,
    val period: String? = null,
    val rating: Any? = null
)