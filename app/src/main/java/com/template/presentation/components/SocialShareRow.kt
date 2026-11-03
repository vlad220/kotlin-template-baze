package com.template.presentation.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SocialShareRow(
    shareText: String,
    shareUrl: String = ""
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, if (shareUrl.isNotEmpty()) "$shareText\n$shareUrl" else shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Поделиться"))
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Поделиться")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Поделиться")
        }
    }
}