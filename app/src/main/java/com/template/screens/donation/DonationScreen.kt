package com.template.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.template.R
import com.template.presentation.viewmodel.DonationViewModel
import kotlinx.coroutines.launch

private val SBP_LINK = "https://vtb.paymo.ru/collect-money/qr/?transaction=0ebf2cf1-246c-415f-9700-1444fdf98fa6"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: DonationViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.done) {
        if (state.done) {
            snackbarHostState.showSnackbar("Спасибо за поддержку!")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поддержать проект") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (state.done) {
                Icon(
                    Icons.Default.CheckCircle, null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Спасибо за поддержку!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Поддержать ещё раз", style = MaterialTheme.typography.titleMedium)
                }
                return@Column
            }

            Icon(
                Icons.Default.Favorite, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Поддержать проект",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            val cardNumber = "2200241857420750"

            Image(
                painter = painterResource(R.drawable.ic_qr_vtb_pay),
                contentDescription = "QR-код для перевода — нажмите чтобы открыть страницу оплаты по СБП",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("card", cardNumber))
                        scope.launch {
                            snackbarHostState.showSnackbar("Открываем страницу оплаты по СБП")
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SBP_LINK))
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    },
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Нажмите на QR — откроется страница оплаты",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "1. Нажмите на QR-код\n" +
                "2. Откроется страница оплаты\n" +
                "3. Выберите свой банк и введите сумму\n" +
                "4. Деньги поступят на счёт проекта\n" +
                "Либо скопируйте номер карты и переведите вручную",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("card", cardNumber))
                    scope.launch {
                        snackbarHostState.showSnackbar("Открываем страницу оплаты по СБП")
                    }
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SBP_LINK))
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Оплатить через СБП", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val copyDetails = "2200 2418 5742 0750"

            Text(
                "Реквизиты для перевода:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                copyDetails,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val cardNumber = "2200 2418 5742 0750"
                    clipboard.setPrimaryClip(ClipData.newPlainText("card", cardNumber))
                    scope.launch {
                        snackbarHostState.showSnackbar("Номер карты скопирован")
                    }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Нажмите на реквизиты, чтобы скопировать номер карты",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
