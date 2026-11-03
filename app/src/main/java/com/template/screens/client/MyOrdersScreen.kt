package com.template.screens.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.template.data.network.OrderDto
import com.template.data.network.TaxiStats
import com.template.data.repository.WarehouseRepository
import com.template.util.Schedule

private val ACTIVE = listOf("new", "taken", "arrived", "started")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(navController: NavController) {
    val repository = remember { WarehouseRepository() }
    var orders by remember { mutableStateOf<List<OrderDto>?>(null) }
    var stats by remember { mutableStateOf<TaxiStats?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf("Активные") }

    LaunchedEffect(Unit) {
        repository.myOrders(role = "client")
            .onSuccess { orders = it }
            .onFailure { e -> error = e.message }
        // Статистика считается только по завершённым поездкам и только для входа
        repository.taxiStats("client").onSuccess { stats = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои заказы") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Активные", "Завершённые", "Отменённые").forEach { name ->
                    FilterChip(selected = tab == name, onClick = { tab = name }, label = { Text(name) })
                }
            }
            Spacer(Modifier.height(12.dp))

            stats?.takeIf { it.rides > 0 }?.let { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Потрачено ${s.total_rub} ₽",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${s.rides} поездок · ${s.total_km} км · ${s.period ?: "за год"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            val list = orders
            when {
                error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.error)
                list == null -> Text("Загружаем…")
                else -> {
                    val shown = list.filter { order ->
                        val status = order.status ?: "new"
                        when (tab) {
                            "Завершённые" -> status == "finished" || status == "rated"
                            "Отменённые" -> status == "cancelled"
                            else -> status in ACTIVE
                        }
                    }
                    if (shown.isEmpty()) {
                        Text("Здесь пусто")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(shown, key = { it.id ?: it.created_at ?: "" }) { order ->
                                OrderRow(order)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: OrderDto) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${order.from_address ?: "Откуда не указано"} → ${order.to_address ?: "Куда не указано"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text("${order.status?.let(::statusLabel) ?: "Новый"} · ${order.price ?: 0} ₽", style = MaterialTheme.typography.bodyMedium)
            order.scheduled_at_ms?.let {
                Text(
                    Schedule.pickupLabel(it) ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            order.distance_m?.let {
                Text(
                    "%.1f км".format(it / 1000.0) +
                        (order.driver_name?.let { name -> " · $name" } ?: "") +
                        (order.plate?.let { plate -> " · $plate" } ?: ""),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            order.created_at?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
