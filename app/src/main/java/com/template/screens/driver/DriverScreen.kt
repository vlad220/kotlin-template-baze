package com.template.screens.driver

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.template.data.network.OrderDto
import com.template.screens.client.statusLabel
import com.template.presentation.components.PhraseInput
import com.template.presentation.theme.TaxiEmerald
import com.template.presentation.theme.TaxiPause
import com.template.presentation.theme.TaxiStar
import com.template.presentation.viewmodel.DriverViewModel
import com.template.util.GeocoderUtils
import com.template.util.LocationTracker
import com.template.util.Notifier
import com.template.util.Schedule
import com.template.util.hasLocationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOCATION_PUSH_MS = 15_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(navController: NavController, viewModel: DriverViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val state by viewModel.state.collectAsState()
    val tripId = state.activeOrder?.id

    // Автоотправка точки: только пока на линии и приложение на переднем плане
    val context = LocalContext.current
    var locationAllowed by remember { mutableStateOf(hasLocationPermission(context)) }
    val askLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { locationAllowed = hasLocationPermission(context) }
    val tracker = remember { LocationTracker(context) }
    val askPermissions: () -> Unit = {
        askLocation.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(state.isOnline, locationAllowed) {
        if (state.isOnline && locationAllowed) {
            tracker.start()
            while (isActive) {
                tracker.current()?.let { (lat, lon) -> viewModel.setLocation(lat, lon, silent = true) }
                delay(LOCATION_PUSH_MS)
            }
        } else {
            tracker.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { tracker.stop() }
    }

    // Взятый заказ — главная штука на экране, поэтому сразу показываем вкладку поездки
    LaunchedEffect(tripId) {
        if (tripId != null) viewModel.setNav("trip")
    }

    // Карта передаёт точку через savedStateHandle экрана, от которого открывались
    DisposableEffect(navController) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        val observer = androidx.lifecycle.Observer<Double?> { lat ->
            val lon = handle?.get<Double?>("picked_lon")
            if (lat != null && lon != null) {
                viewModel.setLocation(lat, lon)
                handle["picked_lat"] = null
                handle["picked_lon"] = null
            }
        }
        handle?.getLiveData<Double?>("picked_lat")?.observeForever(observer)
        onDispose { handle?.getLiveData<Double?>("picked_lat")?.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI-Такси — Водитель") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = state.bottomNav == "dashboard", onClick = { viewModel.setNav("dashboard") },
                    icon = { Icon(Icons.Filled.CarRental, null) }, label = { Text("Линия") })
                if (state.activeOrder != null) {
                    NavigationBarItem(selected = state.bottomNav == "trip", onClick = { viewModel.setNav("trip") },
                        icon = { Icon(Icons.Filled.Route, null) }, label = { Text("Поездка") })
                }
                NavigationBarItem(
                    selected = state.bottomNav == "requests",
                    onClick = { viewModel.setNav("requests") },
                    icon = {
                        val count = state.orders.size
                        var prevCount by remember { mutableStateOf(count) }
                        var justChanged by remember { mutableStateOf(false) }
                        LaunchedEffect(count) {
                            if (count != prevCount) {
                                justChanged = true
                                prevCount = count
                                delay(200)
                                justChanged = false
                            }
                        }
                        val scale = if (justChanged) 1.25f else 1f
                        BadgedBox(badge = {
                            if (count > 0) {
                                Badge(
                                    containerColor = if (justChanged) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.error
                                ) {
                                    Text("$count")
                                }
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                null,
                                modifier = Modifier.scale(scale)
                            )
                        }
                    },
                    label = { Text("Заявки") }
                )
                NavigationBarItem(selected = state.bottomNav == "earnings", onClick = { viewModel.setNav("earnings") },
                    icon = { Icon(Icons.Filled.MonetizationOn, null) }, label = { Text("₽") })
                NavigationBarItem(selected = state.bottomNav == "profile", onClick = { viewModel.setNav("profile") },
                    icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Я") })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AnimatedContent(
                targetState = state.bottomNav,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180)) + slideInHorizontally { it / 20 })
                        .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutHorizontally { -it / 20 })
                },
                label = "driverTabs"
            ) { tab ->
                when (tab) {
                    "requests" -> RequestsTab(viewModel)
                    "earnings" -> EarningsTab(state)
                    "profile" -> DriverProfileScreen(onBack = {}, viewModel = viewModel)
                    "trip" -> state.activeOrder?.let { TripTab(it, viewModel, navController) }
                        ?: DashboardTab(state, viewModel, navController, locationAllowed, askPermissions)
                    else -> DashboardTab(state, viewModel, navController, locationAllowed, askPermissions)
                }
            }
        }
    }
}

@Composable
private fun DashboardTab(
    state: com.template.presentation.viewmodel.DriverState,
    viewModel: DriverViewModel,
    navController: NavController,
    locationAllowed: Boolean,
    onAskLocation: () -> Unit
) {
    val myLat = state.profile?.lat
    val myLon = state.profile?.lon
    // Сервер отдаёт 0 вместо NULL, пока точку не отметили
    val hasPoint = myLat != null && myLon != null && (myLat != 0.0 || myLon != 0.0)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phraseError by remember { mutableStateOf<String?>(null) }
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Карточка статуса
        Card(colors = CardDefaults.cardColors(
            containerColor = if (state.isOnline) TaxiEmerald else TaxiPause
        ), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Row(
                Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pulse = rememberInfiniteTransition(label = "driverPulse")
                val pulseScale by pulse.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "driverPulseScale"
                )
                Box(
                    modifier = Modifier.size(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isOnline) {
                        Box(
                            Modifier
                                .size(18.dp * pulseScale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.30f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (state.isOnline) "На линии — ищете заказы" else "Пауза — заказы не поступают",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Тариф: ${state.profile?.rate_per_km ?: 50} ₽/км${if (state.isOnline && state.orders.isNotEmpty()) " · заявок рядом: ${state.orders.size}" else ""}",
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Без разрешения на уведомления заявка придёт, только когда водитель сам глянет в экран
        if (Notifier.needsConsent(context)) {
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Уведомления о новых заявках выключены",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text("Разрешить") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Где водитель находится: без точки сервер считает заявки от (0,0) и ничего не находит
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Моя точка", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (hasPoint)
                            "%.5f, %.5f".format(myLat, myLon)
                        else
                            "Не отмечена — заявок рядом не будет",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = when {
                            locationAllowed && state.isOnline ->
                                "Автоотправка каждые ${LOCATION_PUSH_MS / 1000} сек"
                            locationAllowed -> "Разрешение дано, включится на линии"
                            else -> "Автоотправка выключена"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (locationAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    if (!locationAllowed) {
                        TextButton(onClick = onAskLocation) { Text("Разрешить геолокацию") }
                    }
                }
                OutlinedButton(onClick = {
                    navController.navigate(
                        "map_picker/${if (hasPoint) myLat else 57.1535}/${if (hasPoint) myLon else 65.5343}/driver"
                    )
                }) { Text("На карте") }
            }
        }

        Spacer(Modifier.height(16.dp))

        // «Еду на Широтную, 63» / «ищу клиентов на Широтной, 10» → та же точка, что и на карте
        PhraseInput(placeholder = "Фраза: «еду на Широтную, 63»") { parsed ->
            val place = parsed.toText ?: parsed.fromText
            if (place == null) {
                phraseError = "Адрес в фразе не найден — отметьте точку на карте"
            } else {
                scope.launch {
                    val coords = withContext(Dispatchers.IO) {
                        GeocoderUtils.addressToCoordinates(context, place)
                    }
                    if (coords == null) {
                        phraseError = "«$place» не нашёл — отметьте точку на карте"
                    } else {
                        phraseError = null
                        viewModel.setLocation(coords.first, coords.second)
                    }
                }
            }
        }
        phraseError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Профиль не заполнен → подсказка
        if (state.profile?.user_id == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Заполните профиль водителя", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Укажите машину и тариф, чтобы получать заказы. Сделайте это в разделе «Профиль» внизу.")
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Главный переключатель Свободен/Пауза
        Button(
            onClick = { viewModel.setOnline(!state.isOnline) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isOnline) MaterialTheme.colorScheme.error else TaxiEmerald
            )
        ) {
            Text(if (state.isOnline) "Перейти на паузу" else "Начать работу", style = MaterialTheme.typography.titleMedium)
        }

        state.message?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun RequestsTab(viewModel: DriverViewModel) {
    val ui by viewModel.state.collectAsState()
    val list = ui.orders
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.LocalShipping,
                    null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Пока заявок нет", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Включите «Начать работу», отметьте точку на карте и настройте уведомления",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(list, key = { it.id ?: "" }) { order ->
            RequestCard(order, viewModel)
        }
    }
}

@Composable
private fun RequestCard(order: OrderDto, viewModel: DriverViewModel) {
    var showCounter by remember { mutableStateOf(false) }
    var counterPrice by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${order.price ?: 0} ₽",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                // Цена за км — про выгодность водителю
                val perKm = order.distance_m?.takeIf { it > 0 }?.let { (order.price ?: 0) * 1000.0 / it }
                perKm?.let {
                    Text(
                        "≈ %.0f ₽/км".format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    order.distance_m?.let { "%.1f км".format(it / 1000.0) } ?: "≈",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(order.from_address ?: "Точка подачи", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(order.to_address ?: "→ Назначение", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            order.scheduled_at_ms?.let {
                Text(
                    "Заранее · ${Schedule.pickupShort(it)}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(4.dp))

            if (showCounter) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = counterPrice, onValueChange = { counterPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Ваша цена, ₽") }, modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val p = counterPrice.toIntOrNull() ?: return@Button
                        viewModel.counterOffer(order.id ?: "", p); showCounter = false
                    }) { Text("Отправить") }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.acceptOrder(order.id ?: "") }, modifier = Modifier.weight(1f)) { Text("Взять заказ") }
                OutlinedButton(onClick = { showCounter = !showCounter }, modifier = Modifier.weight(1f)) { Text("Моя цена") }
            }
        }
    }
}

@Composable
private fun TripTab(
    order: OrderDto,
    viewModel: DriverViewModel,
    navController: NavController
) {
    val ui by viewModel.state.collectAsState()
    var rating by remember { mutableStateOf(0) }
    var review by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text(statusLabel(order.status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                order.scheduled_at_ms?.let {
                    Text(
                        Schedule.pickupLabel(it) ?: "",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Откуда: ${order.from_address ?: "—"}")
                Text("Куда: ${order.to_address ?: "—"}")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(order.distance_m?.let { "%.1f км".format(it / 1000.0) } ?: "—", style = MaterialTheme.typography.bodySmall)
                    order.duration_min?.let { Text("около $it мин", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        Card {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Цена", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Её назвал клиент. Расчёт между собой — вне приложения, оплаты по QR ещё нет")
                }
                Text("${order.price ?: 0} ₽", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        when (order.status) {
            "taken" -> {
                Button(onClick = { viewModel.advanceOrder("arrived") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Я приехал на точку подачи")
                }
                OutlinedButton(onClick = { viewModel.declineOrder() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Отказаться от заказа")
                }
            }
            "arrived" -> {
                Button(onClick = { viewModel.advanceOrder("started") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Начать поездку")
                }
            }
            "started" -> {
                Button(onClick = { viewModel.advanceOrder("finished") },
                    colors = ButtonDefaults.buttonColors(containerColor = TaxiEmerald),
                    modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Завершить поездку")
                }
            }
            "finished" -> {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Оцените клиента", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                IconButton(onClick = { rating = star }) {
                                    Icon(
                                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = "Оценка $star",
                                        tint = TaxiStar
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = review,
                            onValueChange = { review = it },
                            label = { Text("Слово клиенту — необязательно") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.rateClient(rating, review) },
                            enabled = rating > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Отправить оценку") }
                    }
                }
            }
            "rated" -> Text("Поездка завершена, спасибо!")
        }

        if (!order.id.isNullOrBlank() && order.status in listOf("taken", "arrived", "started")) {
            OutlinedButton(
                onClick = { navController.navigate("chat_room/${order.id}") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, null)
                Spacer(Modifier.width(8.dp))
                Text("Написать клиенту")
            }
        }

        ui.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EarningsTab(state: com.template.presentation.viewmodel.DriverState) {
    val stats = state.stats
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Заработок", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Savings,
                    null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("${stats?.total_rub ?: 0} ₽", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("за последний год", style = MaterialTheme.typography.bodyMedium)
                if ((stats?.rides ?: 0L) == 0L) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Пока поездок не было — заработаете с первой заявки",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Поездок", (stats?.rides ?: 0L).toString())
            StatCard("Км", (stats?.total_km ?: 0L).toString())
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Средний чек", if ((stats?.rides ?: 0L) > 0) (stats!!.total_rub / stats.rides).toString() else "0", "₽")
            StatCard("Рейтинг", state.profile?.rating?.let { "%.2f".format(it) } ?: "—")
        }
    }
}

@Composable
private fun RowScope.StatCard(label: String, value: String, suffix: String = "") {
    Card(modifier = Modifier.weight(1f)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text("$value $suffix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}