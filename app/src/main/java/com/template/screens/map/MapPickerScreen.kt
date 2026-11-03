package com.template.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.template.BuildConfig
import com.template.util.GeocoderUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Выбор точки на Яндекс.Карте (WebView + JS API). Координаты метки JavaScript сам
 * присылает в Android при каждом движении, поэтому кнопка активна уже при открытии.
 * Результат кладётся в savedStateHandle предыдущего экрана: picked_lat, picked_lon
 * и picked_for — зачем точка («from», «to» или «driver»).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    navController: NavController,
    title: String = "Выберите точку на карте",
    purpose: String = "from",
    initialLat: Double = 57.1535,
    initialLon: Double = 65.5343
) {
    val context = LocalContext.current
    val picked = remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose { webView?.destroy() }
    }

    LaunchedEffect(picked.value) {
        val p = picked.value ?: return@LaunchedEffect
        address = withContext(Dispatchers.IO) {
            GeocoderUtils.coordinatesToAddress(context, p.first, p.second)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val p = picked.value ?: return@Button
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("picked_lat", p.first)
                        set("picked_lon", p.second)
                        set("picked_for", purpose)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                enabled = picked.value != null
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                val p = picked.value
                Text(
                    text = when {
                        p == null -> "Коснитесь карты"
                        address != null -> "Сохранить: $address"
                        else -> "Сохранить: %.5f, %.5f".format(p.first, p.second)
                    },
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { padding ->
        AndroidView(
            factory = { ctx ->
                createMapWebView(ctx, initialLat, initialLon) { lat, lon ->
                    picked.value = lat to lon
                }.also { webView = it }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createMapWebView(
    context: Context,
    lat: Double,
    lon: Double,
    onPicked: (Double, Double) -> Unit
): WebView {
    val mainThread = Handler(Looper.getMainLooper())
    return WebView(context).apply {
        setBackgroundColor(Color.WHITE)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(true)
        webViewClient = WebViewClient()
        // Метод вызывают из JS-потока WebView — в Compose состояние пишем только через главный поток.
        addJavascriptInterface(object {
            @JavascriptInterface
            fun onCoords(latitude: Double, longitude: Double) {
                mainThread.post { onPicked(latitude, longitude) }
            }
        }, "Android")
        loadDataWithBaseURL(
            "https://yandex.ru",
            buildMapHtml(BuildConfig.YANDEX_MAPS_API_KEY, lat, lon),
            "text/html",
            "UTF-8",
            null
        )
    }
}

private fun buildMapHtml(apiKey: String, lat: Double, lon: Double): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no"/>
        <script src="https://api-maps.yandex.ru/2.1/?apikey=$apiKey&lang=ru_RU" type="text/javascript"></script>
        <style>
          html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; }
        </style>
        </head>
        <body>
        <div id="map"></div>
        <script>
        var map, placemark;
        ymaps.ready(init);
        function init() {
          map = new ymaps.Map("map", { center: [$lat, $lon], zoom: 13, controls: [] });
          map.controls.add('zoomControl');
          placemark = new ymaps.Placemark([$lat, $lon], {}, { draggable: true });
          map.geoObjects.add(placemark);
          placemark.geometry.events.add('change', sendCoords);
          map.events.add('click', function (e) {
            placemark.geometry.setCoordinates(e.get('coords'));
            map.panTo(e.get('coords'));
          });
          sendCoords();
        }
        function sendCoords() {
          var c = placemark.geometry.getCoordinates();
          window.Android.onCoords(c[0], c[1]);
        }
        </script>
        </body>
        </html>
    """.trimIndent()
}
