package com.template.screens.driver

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.template.data.network.SaveDriverRequest
import com.template.presentation.viewmodel.DriverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(onBack: () -> Unit, viewModel: DriverViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val state by viewModel.state.collectAsState()

    var carModel by remember { mutableStateOf(state.profile?.car_model ?: "") }
    var carColor by remember { mutableStateOf(state.profile?.car_color ?: "") }
    var plate by remember { mutableStateOf(state.profile?.plate ?: "") }
    var rateStr by remember { mutableStateOf((state.profile?.rate_per_km ?: 50).toString()) }
    var radius by remember { mutableStateOf((state.profile?.search_radius_km ?: 5)) }

    LaunchedEffect(state.profile) {
        val p = state.profile
        if (p != null) {
            carModel = p.car_model ?: carModel
            carColor = p.car_color ?: carColor
            plate = p.plate ?: plate
            rateStr = (p.rate_per_km ?: rateStr.toIntOrNull() ?: 50).toString()
            radius = p.search_radius_km ?: radius
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль водителя") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ваша машина и тариф", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = carModel, onValueChange = { carModel = it }, label = { Text("Марка и модель (напр. Lada Granta)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = carColor, onValueChange = { carColor = it }, label = { Text("Цвет") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Гос. номер") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(16.dp))

            // Тариф за км — главное поле
            OutlinedTextField(
                value = rateStr,
                onValueChange = { rateStr = it.filter { c -> c.isDigit() } },
                label = { Text("Тариф, ₽ за км") },
                supportingText = { Text("Клиент увидит ориентир цены по вашей ставке") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            // Радиус поиска заявок
            Text("Радиус поиска заявок: $radius км", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = radius.toFloat(),
                onValueChange = { radius = it.toInt() },
                valueRange = 2f..10f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Чем шире радиус — тем больше заявок, но дальше ехать до клиента", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val rate = rateStr.toIntOrNull() ?: 50
                    viewModel.updateProfile(
                        SaveDriverRequest(
                            car_model = carModel.ifBlank { null },
                            car_color = carColor.ifBlank { null },
                            plate = plate.ifBlank { null },
                            rate_per_km = rate,
                            search_radius_km = radius
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !state.saving
            ) {
                if (state.saving) CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text("Сохранить")
            }

            state.message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}