package com.template.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.template.data.network.DriverProfile
import com.template.data.network.OrderDto
import com.template.data.network.SaveDriverRequest
import com.template.data.repository.WarehouseRepository
import com.template.util.Notifier
import com.template.util.Schedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DriverState(
    val profile: DriverProfile? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val isOnline: Boolean = false,
    val orders: List<OrderDto> = emptyList(),
    val activeOrder: OrderDto? = null,
    val stats: com.template.data.network.TaxiStats? = null,
    val error: String? = null,
    val message: String? = null,
    val bottomNav: String = "dashboard" // dashboard | requests | earnings
)

class DriverViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = WarehouseRepository()

    private val _state = MutableStateFlow(DriverState())
    val state: StateFlow<DriverState> = _state

    private var pollJob: kotlinx.coroutines.Job? = null
    private var counteredOrderId: String? = null

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repository.getDriverProfile()
                .onSuccess { profile ->
                    _state.value = _state.value.copy(
                        profile = profile,
                        isOnline = profile.is_online == true,
                        loading = false
                    )
                    if (profile.user_id != null) {
                        loadOrdersAndEarnings()
                        if (profile.is_online == true) startPolling()
                    }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(loading = false, error = e.message)
                }
        }
    }

    fun updateProfile(request: SaveDriverRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, message = null)
            repository.saveDriverProfile(request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false, message = "Профиль сохранён")
                    loadProfile()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(saving = false, error = e.message)
                }
        }
    }

    fun setOnline(online: Boolean) {
        viewModelScope.launch {
            repository.setDriverOnline(online)
                .onSuccess {
                    _state.value = _state.value.copy(isOnline = online, message = if (online) "Вы свободны" else "Пауза")
                    if (online) startPolling() else stopPolling()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(error = e.message)
                }
        }
    }

    fun setLocation(lat: Double, lon: Double, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.value = _state.value.copy(error = null, message = null)
            repository.updateDriverLocation(lat, lon)
                .onSuccess {
                    _state.value = _state.value.copy(
                        profile = _state.value.profile?.copy(lat = lat, lon = lon),
                        message = if (silent) _state.value.message else "Точка обновлена"
                    )
                    // Заявки рядом считаются от новой точки — пересчитать
                    if (!silent) refreshAll()
                }
                .onFailure { e ->
                    if (!silent) _state.value = _state.value.copy(error = e.message)
                }
        }
    }

    fun acceptOrder(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, message = null)
            repository.acceptOrder(id)
                .onSuccess {
                    counteredOrderId = null
                    _state.value = _state.value.copy(message = "Заказ взят!")
                    loadOrdersAndEarnings()
                }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun counterOffer(id: String, price: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, message = null)
            repository.counterPrice(id, price)
                .onSuccess {
                    counteredOrderId = id
                    _state.value = _state.value.copy(message = "Предложение отправлено: согласится клиент — заказ ваш")
                }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun loadOrdersAndEarnings() {
        viewModelScope.launch { refreshAll() }
    }

    private suspend fun refreshAll() {
        repository.ordersNearby()
            .onSuccess { orders ->
                val previous = _state.value.orders
                val known = previous.mapNotNull { it.id }.toSet()
                _state.value = _state.value.copy(orders = orders)
                if (known.isEmpty()) return@onSuccess
                // Водитель на линии и не смотрит в экран — пусть узнает о заявке из уведомления
                orders.firstOrNull { it.id != null && it.id !in known }?.let { fresh ->
                    Notifier.show(getApplication(), "Новая заявка рядом", describe(fresh))
                }
                orders.firstOrNull { order ->
                    val before = previous.firstOrNull { it.id == order.id }?.price
                    order.id != null && before != null && (order.price ?: 0) > before
                }?.let { raised ->
                    Notifier.show(getApplication(), "Клиент поднял цену", describe(raised))
                }
            }
            .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        repository.taxiStats(role = "driver")
            .onSuccess { stats -> _state.value = _state.value.copy(stats = stats) }
            .onFailure { }
        refreshActiveOrder()
    }

    // Водитель ведёт только один активный заказ: тот, что он взял, плюс последняя
    // завершённая поездка — пока клиента не оценён.
    private suspend fun refreshActiveOrder() {
        repository.myOrders(role = "driver").onSuccess { list ->
            val current = list.firstOrNull { it.status in TRIP_STATUSES }
            val old = _state.value.activeOrder
            val agreed = current?.id != null && current.id == counteredOrderId && current.status == "taken"
            _state.value = _state.value.copy(
                activeOrder = if (current?.id == old?.id && old?.status == "rated") old else current
            )
            if (agreed) {
                counteredOrderId = null
                Notifier.show(
                    getApplication(),
                    "Клиент принял вашу цену",
                    "${current?.price ?: 0} ₽ · ${current?.to_address ?: "куда — в карточке заказа"}"
                )
            }
        }
    }

    private fun describe(order: OrderDto): String = listOfNotNull(
        order.scheduled_at_ms?.let { "подача ${Schedule.pickupShort(it)}" },
        order.to_address?.let { "куда: $it" },
        order.distance_m?.let { "%.1f км".format(it / 1000.0) },
        order.price?.let { "$it ₽" }
    ).joinToString(" · ").ifBlank { "Откройте список заявок" }

    fun advanceOrder(status: String) {
        val id = _state.value.activeOrder?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, message = null)
            repository.updateOrderStatus(id, status)
                .onSuccess {
                    _state.value = _state.value.copy(
                        activeOrder = _state.value.activeOrder?.copy(status = status)
                    )
                }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun rateClient(rating: Int, text: String?) {
        val id = _state.value.activeOrder?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, message = null)
            repository.reviewOrder(id, rating, text?.ifBlank { null })
                .onSuccess {
                    _state.value = _state.value.copy(
                        activeOrder = _state.value.activeOrder?.copy(status = "rated"),
                        message = "Оценка клиенту отправлена"
                    )
                }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun declineOrder() {
        val id = _state.value.activeOrder?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, message = null)
            repository.cancelOrder(id, "водитель отказался")
                .onSuccess {
                    _state.value = _state.value.copy(activeOrder = null, message = "От заказа отказался")
                    refreshAll()
                }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_MS)
                refreshAll()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun setNav(tab: String) {
        _state.value = _state.value.copy(bottomNav = tab)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    companion object {
        private const val POLL_MS = 15_000L
        private val TRIP_STATUSES = setOf("taken", "arrived", "started", "finished")
    }
}