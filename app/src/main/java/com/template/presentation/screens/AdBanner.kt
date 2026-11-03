package com.template.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AdBanner() {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primaryContainer)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vsem72.ru/"))
                context.startActivity(intent)
            }
            .padding(vertical = 6.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Бизнес платформа — всё для бизнеса",
            color = colors.onPrimaryContainer,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}
