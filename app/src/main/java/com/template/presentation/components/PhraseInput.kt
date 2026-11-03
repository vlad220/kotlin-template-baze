package com.template.presentation.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.template.util.ParsedPhrase
import com.template.util.PhraseParser

/**
 * Поле «скажите как есть» с микрофоном системного распознавателя.
 * Разбор — по Enter или после голоса, без отдельной кнопки.
 */
@Composable
fun PhraseInput(
    placeholder: String,
    onParsed: (ParsedPhrase) -> Unit
) {
    var phrase by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }

    fun apply(text: String) {
        val parsed = PhraseParser.parse(text)
        if (parsed == null || (parsed.fromText == null && parsed.toText == null && parsed.price == null)) {
            notice = "Не понял фразу — заполните поля сами"
        } else {
            notice = null
            onParsed(parsed)
        }
    }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val said = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (said.isNullOrBlank()) {
            notice = "Речь не услышано — наберите фразу текстом"
        } else {
            phrase = said
            apply(said)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = {
                        phrase = it
                        notice = null
                    },
                    label = { Text(placeholder) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { apply(phrase) }),
                    shape = MaterialTheme.shapes.medium
                )
                FilledIconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                        }
                        try {
                            voice.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            notice = "Голосовой ввод недоступен на этом устройстве"
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(52.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Сказать голосом")
                }
            }
            notice?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
