package com.template.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.template.data.network.CreateOrderRequest
import com.template.data.network.OrderDto
import com.template.data.network.RouteInfo
import com.template.data.repository.WarehouseRepository
import com.template.util.Notifier
import com.template.util.Schedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ClientState(
    val fromLat: Double = 57.1535,
    val fromLon: Double = 65.5343,
    val fromLabel: String? = null,
    val toLabel: String = "",
    val toLat: Double? = null,
    val toLon: Double? = null,
    val route: RouteInfo? = null,
    val price: Int = 0,
    // Цену назвал клиент (ползунком или фразой) — подсказка сервера её не перекрывает
    val namedPrice: Int? = null,
    val loading: Boolean = false,
    val order: OrderDto? = null,
    // Миллисекунды эпохи: когда подать машину. null — «ехать сейчас».
    val scheduledMillis: Long? = null,
    val error: String? = null,
    val message: String? = null
)

class ClientViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = WarehouseRepository()

    private val _state = MutableStateFlow(ClientState())
    val state: StateFlow<ClientState> = _state

    private var pollJob: kotlinx.coroutines.Job? = null

    init {
        loadActiveOrder()
    }

    /** Возвращаем активную заявку после перезапуска: иначе клиент не увидит цену водителя. */
    private fun loadActiveOrder() {
        viewModelScope.launch {
            repository.myOrders(role = "client").onSuccess { list ->
                val current = list.firstOrNull { it.status == null || it.status in ACTIVE }
                val id = current?.id
                if (id != null && _state.value.order?.id != id) {
                    _state.value = _state.value.copy(order = current)
                    startPolling(id)
                }
            }
        }
    }

    fun setFrom(lat: Double, lon: Double, label: String?) {
        _state.value = _state.value.copy(
            fromLat = lat, fromLon = lon, fromLabel = label, route = null, error = null
        )
    }

    fun setTo(label: String, lat: Double?, lon: Double?) {
        _state.value = _state.value.copy(
            toLabel = label, toLat = lat, toLon = lon, route = null, error = null
        )
    }

    fun setPrice(price: Int) {
        _state.value = _state.value.copy(price = price, namedPrice = price)
    }

    /** null — поездка сейчас; иначе миллисекунды эпохи, к которым нужна подача. */
    fun setSchedule(epochMillis: Long?) {
        _state.value = _state.value.copy(scheduledMillis = epochMillis, error = null)
    }

    fun calculateRoute() {
        val s = _state.value
        val toLat = s.toLat
        val toLon = s.toLon
        if (toLat == null || toLon == null) {
            _state.value = s.copy(error = "Сначала укажите, куда ехать")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repository.taxiRoute(s.fromLat, s.fromLon, toLat, toLon)
                .onSuccess { route ->
                    // Клиент сам называет цену, подсказка сервера только задаёт середину ползунка
                    val start = _state.value.namedPrice ?: route.suggested_price ?: DEFAULT_PRICE
                    _state.value = _state.value.copy(
                        loading = false,
                        route = route,
                        price = start,
                        error = if (route.suggested_price == null)
                            "Рядом нет водителей с тарифом — цену назначаете сами"
                        else null
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(loading = false, error = e.message)
                }
        }
    }

    fun createOrder(comment: String? = null) {
        val s = _state.value
        val toLat = s.toLat
        val toLon = s.toLon
        if (toLat == null || toLon == null) {
            _state.value = s.copy(error = "Укажите точку назначения")
            return
        }
        if (s.price <= 0) {
            _state.value = s.copy(error = "Назначьте цену")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, message = null)
            repository.createOrder(
                CreateOrderRequest(
                    from_address = s.fromLabel,
                    from_lat = s.fromLat,
                    from_lon = s.fromLon,
                    to_address = s.toLabel.ifBlank { null },
                    to_lat = toLat,
                    to_lon = toLon,
                    distance_m = s.route?.distance_m,
                    duration_min = s.route?.duration_min,
                    price = s.price,
                    price_source = "client",
                    comment = comment?.ifBlank { null },
                    scheduled_at = s.scheduledMillis?.let { Schedule.toRfc3339(it) }
                )
            )
                .onSuccess { order ->
                    _state.value = _state.value.copy(
                        loading = false,
                        order = order,
                        // Расписание одноразовое: следующий заказ по умолчанию «сейчас»
                        scheduledMillis = null,
                        message = s.scheduledMillis
                            ?.let { "Машина будет ${Schedule.pickupShort(it)}" }
                            ?: "Ищем машину рядом"
                    )
                    startPolling(order.id ?: return@onSuccess)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(loading = false, error = e.message)
                }
        }
    }

    /** Статус заказа обновляем раз в 5 секунд, пока поездка не закончится или её не отменят. */
    private fun startPolling(id: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_MS)
                repository.getOrder(id).onSuccess { order ->
                    val previous = _state.value.order?.status
                    _state.value = _state.value.copy(order = order)
                    notifyTransition(previous, order)
                    when (order.status) {
                        "taken", "arrived", "started" -> _state.value = _state.value.copy(
                            message = when (order.status) {
                                // Отложенный заказ водитель берёт заранее: до подачи ещё часы,
                                // и «едет к вам» было бы неправдой.
                                "taken" -> if (order.isUpcomingPickup())
                                    "Водитель принял заказ: ${Schedule.pickupShort(order.scheduled_at_ms)}"
                                else "Водитель в пути к вам"
                                "arrived" -> "Водитель приехал"
                                else -> "Поездка началась"
                            }
                        )
                        "finished" -> {
                            _state.value = _state.value.copy(message = "Поездка завершена")
                            return@launch
                        }
                        "cancelled" -> {
                            _state.value = _state.value.copy(message = "Заказ отменён")
                            return@launch
                        }
                    }
                }
            }
        }
    }

    /** О смене статуса сообщаем и когда экрана нет на переднем плане. */
    private fun notifyTransition(previous: String?, order: OrderDto) {
        if (order.status == previous) return
        val car = listOfNotNull(order.driver_name, order.plate).joinToString(" · ")
        val app = getApplication<Application>()
        when (order.status) {
            "taken" -> if (order.isUpcomingPickup()) {
                Notifier.show(app, "Заказ принят", "${Schedule.pickupShort(order.scheduled_at_ms)} · $car")
            } else {
                Notifier.show(app, "Машина нашлась", car.ifBlank { "Водитель принял заказ" })
            }
            "arrived" -> Notifier.show(app, "Водитель приехал", car.ifBlank { "Ждите у точки подачи" })
            "started" -> Notifier.show(app, "Поехали", order.to_address ?: "")
            "cancelled" -> Notifier.show(app, "Заказ отменён", "Отменили не с вашей стороны")
        }
    }

    /** Отложенный заказ, до подачи которого ещё больше четверти часа. */
    private fun OrderDto.isUpcomingPickup(): Boolean =
        scheduled_at_ms != null && scheduled_at_ms > System.currentTimeMillis() + QUARTER_HOUR_MS

    fun cancelOrder() {
        val id = _state.value.order?.id ?: return
        viewModelScope.launch {
            repository.cancelOrder(id, "клиент передумал")
                .onSuccess {
                    pollJob?.cancel()
                    _state.value = _state.value.copy(order = null, message = "Поиск остановлен")
                }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    /** Клиент принимает цену водителя: сервер закрепляет заказ за тем, кто её предложил. */
    fun acceptCounter() {
        val order = _state.value.order ?: return
        val counter = order.counter_price ?: return
        viewModelScope.launch {
            repository.acceptCounterPrice(order.id ?: return@launch)
                .onSuccess { refreshOrder("Договорились за $counter ₽") }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    /** Никто не берёт заказ: цена поднимается на 10%, прежнее предложение водителя сгорает. */
    fun bumpPrice() {
        val order = _state.value.order ?: return
        viewModelScope.launch {
            repository.bumpPrice(order.id ?: return@launch)
                .onSuccess { refreshOrder("Цену подняли — ищем дальше") }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    private suspend fun refreshOrder(message: String) {
        val id = _state.value.order?.id ?: return
        repository.getOrder(id).onSuccess { order ->
            _state.value = _state.value.copy(order = order, message = message)
        }
    }

    fun rateRide(rating: Int, text: String?) {
        val id = _state.value.order?.id ?: return
        viewModelScope.launch {
            repository.reviewOrder(id, rating, text?.ifBlank { null })
                .onSuccess {
                    _state.value = _state.value.copy(
                        order = _state.value.order?.copy(status = "rated"),
                        message = "Спасибо за оценку"
                    )
                }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, message = null)
    }

    companion object {
        private const val DEFAULT_PRICE = 150
        private const val POLL_MS = 5_000L
        private const val QUARTER_HOUR_MS = 15 * 60_000L
        private val ACTIVE = setOf("new", "taken", "arrived", "started")
    }
}
