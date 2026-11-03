package com.template.screens.client

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.template.data.local.AppDatabase
import com.template.data.local.entity.SavedAddressEntity
import com.template.data.network.ApiClient
import com.template.data.network.OrderDto
import com.template.presentation.components.PhraseInput
import com.template.presentation.theme.TaxiStar
import com.template.presentation.viewmodel.ClientViewModel
import com.template.screens.map.TripMapCard
import com.template.util.GeocoderUtils
import com.template.util.Notifier
import com.template.util.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

private const val MIN_PRICE = 50
private const val PRICE_STEP = 10
private val TRIP_STATUSES = setOf("taken", "arrived", "started")

/** Столько суток вперёд принимает сервер — дальше диалог выбора не пускает. */
private const val SCHEDULE_DAYS = 30L
private const val DAY_MS = 86_400_000L
/** На сколько минут вперёд ставить стрелки часов по умолчанию, если время не выбрано. */
private const val MIN_LEAD_MS = 15 * 60_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClientScreen(
    navController: NavController,
    viewModel: ClientViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var toText by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var geoError by remember { mutableStateOf<String?>(null) }
    var pickSchedule by remember { mutableStateOf(false) }
    val toLat = state.toLat
    val toLon = state.toLon

    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val db = remember { AppDatabase.getInstance(context) }
    val recentAddresses by db.addressDao().observeRecent().collectAsState(initial = emptyList())

    fun saveAddress(text: String, lat: Double, lon: Double) {
        scope.launch {
            db.addressDao().save(SavedAddressEntity(text, lat, lon, System.currentTimeMillis()))
        }
    }

    // Точка с карты возвращается через savedStateHandle этого экрана
    DisposableEffect(navController) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        val observer = androidx.lifecycle.Observer<Double?> { lat ->
            val lon = handle?.get<Double?>("picked_lon")
            if (lat != null && lon != null) {
                val label = coordsLabel(lat, lon)
                if (handle?.get<String>("picked_for") == "to") {
                    viewModel.setTo(label, lat, lon)
                    toText = label
                } else {
                    viewModel.setFrom(lat, lon, label)
                }
                handle?.set("picked_lat", null)
                handle?.set("picked_lon", null)
                handle?.set("picked_for", null)
            }
        }
        handle?.getLiveData<Double?>("picked_lat")?.observeForever(observer)
        onDispose { handle?.getLiveData<Double?>("picked_lat")?.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказать поездку") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("my_orders") }) {
                        Icon(Icons.Default.Receipt, contentDescription = "Мои заказы")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MapHero(
                fromLabel = state.fromLabel ?: coordsLabel(state.fromLat, state.fromLon),
                toLabel = state.toLabel.ifBlank { toText.ifBlank { "Куда ехать?" } },
                onFromMap = {
                    navController.navigate("map_picker/${state.fromLat}/${state.fromLon}/from")
                },
                onToMap = {
                    navController.navigate("map_picker/${toLat ?: state.fromLat}/${toLon ?: state.fromLon}/to")
                }
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset(y = (-18).dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp
            ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ApiClient.getToken() == null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Нужен вход", fontWeight = FontWeight.Bold)
                        Text("Без входа заказ не создастся — сервер не поймёт, кому платить водителю.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { navController.navigate("auth") }) {
                            Icon(Icons.Default.Person, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Войти")
                        }
                    }
                }
            }

            PhraseInput(placeholder = "Фраза: «из центра на вокзал за 250»") { parsed ->
                scope.launch {
                    parsed.price?.let { viewModel.setPrice(it) }
                    parsed.fromText?.let { label ->
                        val coords = withContext(Dispatchers.IO) {
                            GeocoderUtils.addressToCoordinates(context, label)
                        }
                        if (coords == null) {
                            geoError = "«$label» как точку подачи не нашёл — отметьте на карте"
                        } else {
                            viewModel.setFrom(coords.first, coords.second, label)
                        }
                    }
                    parsed.toText?.let { label ->
                        toText = label
                        val coords = withContext(Dispatchers.IO) {
                            GeocoderUtils.addressToCoordinates(context, label)
                        }
                        if (coords == null) {
                            geoError = "«$label» не нашёл — отметьте точку на карте"
                            viewModel.setTo(label, null, null)
                        } else {
                            geoError = null
                            viewModel.setTo(label, coords.first, coords.second)
                            saveAddress(label, coords.first, coords.second)
                        }
                    }
                    if (viewModel.state.value.toLat != null) viewModel.calculateRoute()
                }
            }

            PointRow(
                label = "Откуда",
                value = state.fromLabel ?: coordsLabel(state.fromLat, state.fromLon),
                onMap = {
                    navController.navigate("map_picker/${state.fromLat}/${state.fromLon}/from")
                }
            )

            OutlinedTextField(
                value = toText,
                onValueChange = {
                    toText = it
                    geoError = null
                },
                label = { Text("Куда ехать?") },
                supportingText = { Text(state.toLabel.ifBlank { "адрес или кнопка «на карте»" }) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val query = toText.trim()
                        if (query.isBlank()) {
                            geoError = "Введите адрес"
                            return@Button
                        }
                        scope.launch {
                            val coords = withContext(Dispatchers.IO) {
                                GeocoderUtils.addressToCoordinates(context, query)
                            }
                            if (coords == null) {
                                geoError = "Адрес не найден — отметьте точку на карте"
                                viewModel.setTo(query, null, null)
                            } else {
                                geoError = null
                                viewModel.setTo(query, coords.first, coords.second)
                                saveAddress(query, coords.first, coords.second)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Найти")
                }
                OutlinedButton(
                    onClick = { navController.navigate("map_picker/${toLat ?: state.fromLat}/${toLon ?: state.fromLon}/to") },
                    modifier = Modifier.weight(1f)
                ) { Text("На карте") }
            }
            geoError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (recentAddresses.isNotEmpty() && state.order == null) {
                Text(
                    "Раньше ездили туда",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentAddresses.forEach { saved ->
                        SuggestionChip(
                            onClick = {
                                toText = saved.text
                                geoError = null
                                viewModel.setTo(saved.text, saved.lat, saved.lon)
                                scope.launch {
                                    db.addressDao().save(saved.copy(usedAt = System.currentTimeMillis()))
                                }
                            },
                            label = { Text(saved.text.take(28)) }
                        )
                    }
                }
            }

            if (toLat != null && toLon != null) {
                Button(
                    onClick = { viewModel.calculateRoute() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Рассчитать дорогу")
                    }
                }
            }

            state.route?.let { route ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "%.1f км · около %d мин".format(route.distance_m / 1000.0, route.duration_min),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (route.approx) {
                            Text(
                                "Расчёт приблизительный: по прямой с запасом на повороты",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("Свободных машин рядом: ${route.drivers_nearby}")
                        if (route.median_rate_per_km > 0) {
                            Text("Ставка рядом: %.0f ₽ за км".format(route.median_rate_per_km))
                        }
                    }
                }

                val maxPrice = ((route.suggested_price ?: MIN_PRICE * 4) * 3)
                    .coerceAtLeast(MIN_PRICE + PRICE_STEP * 10)
                    .coerceAtLeast(state.price)
                Text(
                    "${state.price} ₽",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Ваша цена — водители видят именно её",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = state.price.toFloat().coerceIn(MIN_PRICE.toFloat(), maxPrice.toFloat()),
                    onValueChange = { viewModel.setPrice((it / PRICE_STEP).roundToInt() * PRICE_STEP) },
                    valueRange = MIN_PRICE.toFloat()..maxPrice.toFloat(),
                    steps = ((maxPrice - MIN_PRICE) / PRICE_STEP - 1).coerceAtLeast(0)
                )
                route.suggested_price?.let {
                    Text("Ориентир по ставкам рядом — $it ₽", style = MaterialTheme.typography.bodySmall)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Когда подавать машину",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.scheduledMillis == null,
                        onClick = { viewModel.setSchedule(null) },
                        label = { Text("Сейчас") }
                    )
                    FilterChip(
                        selected = state.scheduledMillis != null,
                        onClick = { pickSchedule = true },
                        label = {
                            Text(Schedule.pickupShort(state.scheduledMillis) ?: "На время")
                        }
                    )
                }
                if (state.scheduledMillis != null) {
                    Text(
                        "Водитель увидит, что поездка запланирована, и сможет взять её заранее",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий водителю") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (state.order == null) {
                val pickup = Schedule.pickupShort(state.scheduledMillis)
                Button(
                    onClick = { viewModel.createOrder(comment) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = toLat != null && toLon != null && state.price > 0 && !state.loading
                ) {
                    Icon(Icons.Filled.DirectionsCar, null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (pickup == null) "Вызвать машину за ${state.price} ₽"
                        else "Заказать на $pickup · ${state.price} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Без разрешения о том, что машина нашлась и приехала, клиент узнает только глядя в экран
                if (Notifier.needsConsent(context)) {
                    TextButton(onClick = {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text("Разрешить уведомления о машине") }
                }
            } else {
                state.order?.let { order ->
                    OrderCard(
                        order = order,
                        navController = navController,
                        onRate = { stars -> viewModel.rateRide(stars, comment) },
                        onCancel = { viewModel.cancelOrder() },
                        onAcceptCounter = { viewModel.acceptCounter() },
                        onBump = { viewModel.bumpPrice() }
                    )
                }
            }

            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = { navController.navigate("driver") }) {
                Text("Я водитель — переключиться")
            }

            if (pickSchedule) {
                PickupDialog(
                    initialMillis = state.scheduledMillis,
                    onDismiss = { pickSchedule = false },
                    onConfirm = {
                        viewModel.setSchedule(it)
                        pickSchedule = false
                    }
                )
            }
        }
            }
        }
    }
}

/**
 * Выбор времени подачи. Календарь отдаёт полночь по UTC, поэтому выбранную дату
 * переводим в локальный пояс и собираем момент вместе с выбранным часом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickupDialog(initialMillis: Long?, onDismiss: () -> Unit, onConfirm: (Long?) -> Unit) {
    val start = Instant.ofEpochMilli(initialMillis ?: System.currentTimeMillis() + MIN_LEAD_MS)
        .atZone(ZoneId.systemDefault())
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = start.toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = start.hour,
        initialMinute = start.minute,
        is24Hour = true
    )
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Когда подать машину") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatePicker(datePickerState)
                TimePicker(timePickerState)
                Text(
                    "Сервер не примет подачу раньше текущего момента и дальше $SCHEDULE_DAYS суток вперёд",
                    style = MaterialTheme.typography.bodySmall
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = utcDate(datePickerState.selectedDateMillis)
                if (date == null) {
                    error = "Выберите дату"
                    return@TextButton
                }
                val picked = Schedule.of(date, LocalTime.of(timePickerState.hour, timePickerState.minute))
                val now = System.currentTimeMillis()
                when {
                    picked < now -> error = "Это время уже прошло"
                    picked > now + SCHEDULE_DAYS * DAY_MS ->
                        error = "Планировать подачу можно максимум на $SCHEDULE_DAYS суток"
                    else -> onConfirm(picked)
                }
            }) { Text("Ок") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onConfirm(null) }) { Text("Сейчас") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}

private fun utcDate(utcTimeMillis: Long?): LocalDate? = utcTimeMillis
    ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }

@Composable
private fun MapHero(
    fromLabel: String,
    toLabel: String,
    onFromMap: () -> Unit,
    onToMap: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .background(
                Brush.verticalGradient(
                    listOf(colors.primaryContainer, colors.background)
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MapPinChip("А", "Откуда", fromLabel, onFromMap)
            MapPinChip("Б", "Куда", toLabel, onToMap)
        }
    }
}

@Composable
private fun MapPinChip(letter: String, label: String, value: String, onMap: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onMap,
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(letter, color = colors.onPrimary, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            Icon(Icons.Outlined.Place, contentDescription = "На карте", tint = colors.primary)
        }
    }
}

@Composable
private fun PointRow(label: String, value: String, onMap: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(value, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onMap) { Text("На карте") }
        }
    }
}

@Composable
private fun TripStatusRow(status: String?) {
    val steps = listOf(
        "new" to ("Поиск" to Icons.Outlined.Search),
        "taken" to ("Едет" to Icons.Filled.DirectionsCar),
        "arrived" to ("На месте" to Icons.Filled.Place),
        "started" to ("В пути" to Icons.Filled.Route)
    )
    val current = when (status) {
        "new", null -> 0
        "taken" -> 1
        "arrived" -> 2
        "started", "finished", "rated" -> 3
        else -> 0
    }
    val activePulse = rememberInfiniteTransition(label = "tripStepPulse")
    val pulseScale by activePulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tripStepScale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, (_, titleAndIcon) ->
            val (title, icon) = titleAndIcon
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                if (index == current) {
                    // пульсирующая подложка вокруг активного шага
                    Box(
                        modifier = Modifier
                            .size(40.dp * pulseScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        stepIcon(index, current, icon, title)
                    }
                } else {
                    stepIcon(index, current, icon, title)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (index <= current) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun stepIcon(index: Int, current: Int, icon: ImageVector, title: String) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (index < current) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else if (index == current) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (index <= current) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun OrderCard(
    order: OrderDto,
    navController: NavController,
    onRate: (Int) -> Unit,
    onCancel: () -> Unit,
    onAcceptCounter: () -> Unit,
    onBump: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(statusLabel(order.status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (order.status in listOf("new", "taken", "arrived", "started", null)) {
                TripStatusRow(order.status)
            }
            Schedule.pickupLabel(order.scheduled_at_ms)?.let {
                Text(it, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            }
            Text("Цена: ${order.price ?: 0} ₽")
            val waiting = order.status == null || order.status == "new"
            if (waiting) {
                order.counter_price?.let { counter ->
                    Text("Водитель предлагает $counter ₽", fontWeight = FontWeight.SemiBold)
                    Button(onClick = onAcceptCounter, modifier = Modifier.fillMaxWidth()) {
                        Text("Согласиться за $counter ₽")
                    }
                }
                val next = ((order.price ?: 0) * 1.1).roundToInt()
                OutlinedButton(onClick = onBump, modifier = Modifier.fillMaxWidth()) {
                    Text("Поднять свою цену до $next ₽")
                }
            }
            order.driver_name?.let { name ->
                val car = listOfNotNull(order.car_model, order.car_color, order.plate).joinToString(", ")
                Text("$name · $car")
            }
            if (order.status in TRIP_STATUSES && order.from_lat != null && order.to_lat != null) {
                TripMapCard(order)
            }
            // Переписка живёт в комнате заказа: id заказа = id комнаты на сервере.
            val canChat = !order.id.isNullOrBlank() && order.status in listOf("taken", "arrived", "started")
            if (order.status == null || order.status in listOf("new", "taken", "arrived", "started")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.status != "started") {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Отменить") }
                    }
                    if (canChat) {
                        Button(
                            onClick = { navController.navigate("chat_room/${order.id}") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Написать")
                        }
                    }
                }
            }
            if (order.status == "finished") {
                var rated by remember { mutableStateOf(0) }
                Text("Как поездка? Оцените водителя")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        IconButton(onClick = {
                            rated = star
                            onRate(star)
                        }) {
                            Icon(
                                imageVector = if (star <= rated) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Оценка $star",
                                tint = TaxiStar
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun coordsLabel(lat: Double, lon: Double): String = "%.5f, %.5f".format(lat, lon)

internal fun statusLabel(status: String?): String = when (status) {
    "new" -> "Ищем машину"
    "taken" -> "Водитель едет к вам"
    "arrived" -> "Водитель приехал"
    "started" -> "Едем"
    "finished" -> "Поездка завершена"
    "cancelled" -> "Заказ отменён"
    "rated" -> "Спасибо за оценку"
    else -> "Заказ создан"
}
