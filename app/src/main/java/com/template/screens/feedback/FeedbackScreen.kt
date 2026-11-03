package com.template.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.template.presentation.viewmodel.FeedbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(onBack: () -> Unit = {}) {
    val viewModel: FeedbackViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.result) {
        state.result?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ошибки и предложения") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ошибки и предложения", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = state.text,
                onValueChange = { viewModel.updateText(it) },
                label = { Text("Опишите проблему или идею") },
                placeholder = { Text("Что не работает? Что можно улучшить?") },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                maxLines = 10
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.submit() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isSending && state.text.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отправить", style = MaterialTheme.typography.titleMedium)
                }
            }
            
        }
    }
}
