package com.template.data.network

import com.template.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://vsem72.ru/api/v1/"

    private var token: String? = null
    private var userId: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    fun getToken(): String? = token

    /** id вошедшего — нужен сокету чата: сервер пускает в комнату только по настоящему пользователю. */
    fun setUserId(id: String?) {
        userId = id
    }

    fun getUserId(): String? = userId

    /** ws://…/chat — тот же сервер, только по другому протоколу. */
    fun chatSocketUrl(): String? {
        val t = token ?: return null
        val u = userId ?: return null
        val httpBase = BASE_URL.removeSuffix("api/v1/").trimEnd('/')
        val wsBase = httpBase.replace("https://", "wss://").replace("http://", "ws://")
        return "$wsBase/chat?userId=$u&token=$t"
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        token?.let { request.addHeader("Authorization", "Bearer $it") }
        chain.proceed(request.build())
    }

    // BODY пишет в logcat JWT, телефоны и адреса поездок. В debug остаётся BASIC:
    // видно адрес и код ответа, содержимого запроса в логе нет.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // Без этого тик опроса водителя мог идти 3×30 сек и интервал растягивался
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: WarehouseApi = retrofit.create(WarehouseApi::class.java)
}