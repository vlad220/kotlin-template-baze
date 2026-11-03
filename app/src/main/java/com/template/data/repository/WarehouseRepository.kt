package com.template.data.repository

import com.template.data.network.*

class WarehouseRepository {
    private val api get() = ApiClient.api

    /** id вошедшего нужен сокету чата: сервер пускает в комнату только по настоящему пользователю. */
    private fun saveAuth(auth: AuthResponse) {
        ApiClient.setToken(auth.token)
        ApiClient.setUserId(auth.user.id)
    }

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        if (email == "demo@example.ru" && password == "demo-password") {
            val mockUser = UserDto(id = "admin-id", name = "Попов В.", email = email)
            val mockAuth = AuthResponse(token = "admin-demo-token", user = mockUser)
            ApiClient.setToken("admin-demo-token")
            return Result.success(mockAuth)
        }
        return try {
            val response = api.login(LoginRequest(email, password))
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                saveAuth(body.data)
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Ошибка входа: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun register(name: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.register(RegisterRequest(name, email, password))
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                saveAuth(body.data)
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Ошибка регистрации: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getMe(): Result<UserDto> {
        return try {
            val response = api.getMe()
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                ApiClient.setUserId(body.data.id)
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getProducts(search: String? = null): Result<List<ProductDto>> {
        return try {
            val response = api.getProducts(search)
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createProduct(product: ProductDto): Result<ProductDto?> {
        return try {
            val response = api.createProduct(product)
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendCode(phone: String): Result<SmsResponse> {
        return try {
            val response = api.sendCode(SendCodeRequest(phone))
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun verifyCode(phone: String, code: String): Result<AuthResponse> {
        return try {
            val response = api.verifyCode(VerifyCodeRequest(phone, code))
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                saveAuth(body.data)
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendFeedback(type: String, text: String): Result<Unit> {
        return try {
            val response = api.sendFeedback(FeedbackRequest(app = "taxi", type = type, description = text))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // Bitrix24
    suspend fun connectBitrix24(webhookUrl: String): Result<Unit> {
        return try {
            val response = api.connectBitrix24(Bitrix24ConnectRequest(webhookUrl))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getBitrix24Status(): Result<Bitrix24StatusData> {
        return try {
            val response = api.getBitrix24Status()
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun disconnectBitrix24(): Result<Unit> {
        return try {
            val response = api.disconnectBitrix24()
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun syncBitrix24(): Result<Unit> {
        return try {
            val response = api.syncBitrix24()
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun searchUsers(query: String): Result<List<UserSearchResult>> {
        return try {
            val response = api.searchUsers(query)
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Ошибка поиска: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // 1C
    suspend fun connectOneC(serverUrl: String, apiKey: String?, username: String?, password: String?, usePassword: Boolean, syncOptions: List<String>?): Result<Unit> {
        return try {
            val response = api.connectOneC(OneCConnectRequest(serverUrl, apiKey, username, password, usePassword, syncOptions))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getOneCStatus(): Result<OneCStatusData> {
        return try {
            val response = api.getOneCStatus()
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun disconnectOneC(): Result<Unit> {
        return try {
            val response = api.disconnectOneC()
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun syncOneC(): Result<Unit> {
        return try {
            val response = api.syncOneC()
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getOneCSyncOptions(): Result<SyncOptionsData> {
        return try {
            val response = api.getOneCSyncOptions()
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // === AI-Такси ===
    suspend fun getDriverProfile(): Result<DriverProfile> {
        return try {
            val response = api.getDriverProfile()
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun saveDriverProfile(request: SaveDriverRequest): Result<Unit> {
        return try {
            val response = api.saveDriverProfile(request)
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun setDriverOnline(online: Boolean): Result<Unit> {
        return try {
            val response = api.setDriverOnline(if (online) "1" else "0")
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateDriverLocation(lat: Double, lon: Double): Result<Unit> {
        return try {
            val response = api.updateDriverLocation(LocationRequest(lat, lon))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun driversNearby(lat: Double, lon: Double): Result<NearbySummary> {
        return try {
            val response = api.driversNearby(lat, lon)
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createOrder(request: CreateOrderRequest): Result<OrderDto> {
        return try {
            val response = api.createOrder(request)
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun taxiRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, city: String? = null): Result<RouteInfo> {
        return try {
            val response = api.taxiRoute(fromLat, fromLon, toLat, toLon, null, city)
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getOrder(id: String): Result<OrderDto> {
        return try {
            val response = api.getOrder(id)
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun myOrders(role: String? = null): Result<List<OrderDto>> {
        return try {
            val response = api.myOrders(role)
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun ordersNearby(): Result<List<OrderDto>> {
        return try {
            val response = api.ordersNearby()
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun acceptOrder(id: String): Result<Unit> {
        return try {
            val response = api.acceptOrder(id)
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateOrderStatus(id: String, status: String): Result<Unit> {
        return try {
            val response = api.updateOrderStatus(id, UpdateStatusRequest(status))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun cancelOrder(id: String, reason: String? = null): Result<Unit> {
        return try {
            val response = api.cancelOrder(id, CancelOrderRequest(reason))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun counterPrice(id: String, price: Int): Result<Unit> {
        return try {
            val response = api.counterPrice(id, CounterPriceRequest(price))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun acceptCounterPrice(id: String): Result<Unit> {
        return try {
            val response = api.acceptCounterPrice(id)
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun bumpPrice(id: String): Result<Unit> {
        return try {
            val response = api.bumpPrice(id)
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reviewOrder(id: String, rating: Int, text: String? = null): Result<Unit> {
        return try {
            val response = api.reviewOrder(id, ReviewRequest(rating, text))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) Result.success(Unit)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun taxiStats(role: String): Result<TaxiStats> {
        return try {
            val response = api.taxiStats(role)
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) Result.success(body.data)
            else Result.failure(Exception(body?.error?.message ?: "Ошибка: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
