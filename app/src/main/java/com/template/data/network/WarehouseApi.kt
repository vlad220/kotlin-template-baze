package com.template.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface WarehouseApi {
    // Auth
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): Response<ApiResponse<SmsResponse>>

    @POST("auth/verify-code")
    suspend fun verifyCode(@Body request: VerifyCodeRequest): Response<ApiResponse<AuthResponse>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    // Товары
    @GET("warehouse/products")
    suspend fun getProducts(@Query("search") search: String? = null): Response<ApiResponse<List<ProductDto>>>

    @POST("warehouse/products")
    suspend fun createProduct(@Body product: ProductDto): Response<ApiResponse<ProductDto?>>

    // Обратная связь
    @POST("feedback/send")
    suspend fun sendFeedback(@Body request: FeedbackRequest): Response<ApiResponse<FeedbackResponse>>

    // Битрикс24
    @POST("integrations/bitrix24/connect")
    suspend fun connectBitrix24(@Body request: Bitrix24ConnectRequest): Response<ApiResponse<Unit>>

    @GET("integrations/bitrix24/status")
    suspend fun getBitrix24Status(): Response<ApiResponse<Bitrix24StatusData>>

    @POST("integrations/bitrix24/disconnect")
    suspend fun disconnectBitrix24(): Response<ApiResponse<Unit>>

    @POST("integrations/bitrix24/sync")
    suspend fun syncBitrix24(): Response<ApiResponse<Unit>>

    // 1С
    @POST("integrations/1c/connect")
    suspend fun connectOneC(@Body request: OneCConnectRequest): Response<ApiResponse<Unit>>

    @GET("integrations/1c/status")
    suspend fun getOneCStatus(): Response<ApiResponse<OneCStatusData>>

    @POST("integrations/1c/disconnect")
    suspend fun disconnectOneC(): Response<ApiResponse<Unit>>

    @POST("integrations/1c/sync")
    suspend fun syncOneC(): Response<ApiResponse<Unit>>

    @GET("integrations/1c/sync-options")
    suspend fun getOneCSyncOptions(): Response<ApiResponse<SyncOptionsData>>

    // Пользователи
    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<ApiResponse<List<UserSearchResult>>>

    // === AI-Такси ===
    @GET("taxi/driver/profile")
    suspend fun getDriverProfile(): Response<ApiResponse<DriverProfile>>

    @PUT("taxi/driver/profile")
    suspend fun saveDriverProfile(@Body request: SaveDriverRequest): Response<ApiResponse<Any>>

    @PUT("taxi/driver/online/{on}")
    suspend fun setDriverOnline(@Path("on") on: String): Response<ApiResponse<Any>>

    @POST("taxi/driver/location")
    suspend fun updateDriverLocation(@Body request: LocationRequest): Response<ApiResponse<Any>>

    @GET("taxi/drivers/nearby")
    suspend fun driversNearby(@Query("lat") lat: Double, @Query("lon") lon: Double, @Query("radius_km") radiusKm: Double? = null): Response<ApiResponse<NearbySummary>>

    @POST("taxi/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<ApiResponse<OrderDto>>

    @GET("taxi/route")
    suspend fun taxiRoute(
        @Query("from_lat") fromLat: Double,
        @Query("from_lon") fromLon: Double,
        @Query("to_lat") toLat: Double,
        @Query("to_lon") toLon: Double,
        @Query("distance_m") distanceM: Int? = null,
        @Query("city") city: String? = null
    ): Response<ApiResponse<RouteInfo>>

    @GET("taxi/orders")
    suspend fun myOrders(@Query("role") role: String? = null): Response<ApiResponse<List<OrderDto>>>

    @GET("taxi/orders/nearby")
    suspend fun ordersNearby(): Response<ApiResponse<List<OrderDto>>>

    @GET("taxi/orders/{id}")
    suspend fun getOrder(@Path("id") id: String): Response<ApiResponse<OrderDto>>

    @POST("taxi/orders/{id}/accept")
    suspend fun acceptOrder(@Path("id") id: String): Response<ApiResponse<Any>>

    @POST("taxi/orders/{id}/status")
    suspend fun updateOrderStatus(@Path("id") id: String, @Body request: UpdateStatusRequest): Response<ApiResponse<Any>>

    @POST("taxi/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: String, @Body request: CancelOrderRequest): Response<ApiResponse<Any>>

    @POST("taxi/orders/{id}/counter")
    suspend fun counterPrice(@Path("id") id: String, @Body request: CounterPriceRequest): Response<ApiResponse<Any>>

    @POST("taxi/orders/{id}/counter/accept")
    suspend fun acceptCounterPrice(@Path("id") id: String): Response<ApiResponse<Any>>

    @POST("taxi/orders/{id}/bump")
    suspend fun bumpPrice(@Path("id") id: String): Response<ApiResponse<Any>>

    @POST("taxi/orders/{id}/review")
    suspend fun reviewOrder(@Path("id") id: String, @Body request: ReviewRequest): Response<ApiResponse<Any>>

    @GET("taxi/stats")
    suspend fun taxiStats(@Query("role") role: String): Response<ApiResponse<TaxiStats>>
}