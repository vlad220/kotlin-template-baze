package com.template.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.template.BuildConfig
import com.template.data.network.OrderDto

/**
 * Карта поездки (экран 10): точки подачи и назначения, маршрут на машине и живая
 * точка водителя. WebView создаётся один раз на заказ, координаты машины JS двигает
 * через setDriver() — иначе каждый опрос статуса пересоздавал бы карту и моргал.
 */
@Composable
fun TripMapCard(order: OrderDto) {
    val context = LocalContext.current
    val fromLat = order.from_lat ?: return
    val fromLon = order.from_lon ?: return
    val toLat = order.to_lat ?: return
    val toLon = order.to_lon ?: return

    var webView by remember { mutableStateOf<WebView?>(null) }
    var routeNote by remember { mutableStateOf("Строю маршрут…") }

    DisposableEffect(order.id) {
        onDispose { webView?.destroy() }
    }

    LaunchedEffect(order.driver_lat, order.driver_lon) {
        val lat = order.driver_lat
        val lon = order.driver_lon
        if (lat != null && lon != null) {
            webView?.evaluateJavascript("setDriver($lat, $lon);", null)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column {
            AndroidView(
                factory = { ctx ->
                    createTripWebView(
                        context = ctx,
                        from = fromLat to fromLon,
                        to = toLat to toLon,
                        driver = order.driver_lat?.let { l -> order.driver_lon?.let { d -> l to d } },
                        onRoute = { ok -> routeNote = if (ok) "Маршрут проложен" else "Маршрут недоступен — показаны только точки" }
                    ).also { webView = it }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Text(
                text = routeNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createTripWebView(
    context: Context,
    from: Pair<Double, Double>,
    to: Pair<Double, Double>,
    driver: Pair<Double, Double>?,
    onRoute: (Boolean) -> Unit
): WebView {
    val mainThread = Handler(Looper.getMainLooper())
    return WebView(context).apply {
        setBackgroundColor(Color.WHITE)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(true)
        webViewClient = WebViewClient()
        addJavascriptInterface(object {
            @JavascriptInterface
            fun onRouteResult(ok: Boolean) {
                mainThread.post { onRoute(ok) }
            }
        }, "Android")
        loadDataWithBaseURL(
            "https://yandex.ru",
            buildTripMapHtml(BuildConfig.YANDEX_MAPS_API_KEY, from, to, driver),
            "text/html",
            "UTF-8",
            null
        )
    }
}

private fun buildTripMapHtml(
    apiKey: String,
    from: Pair<Double, Double>,
    to: Pair<Double, Double>,
    driver: Pair<Double, Double>?
): String {
    val driverInit = driver?.let { "[${it.first}, ${it.second}]" } ?: "[${from.first}, ${from.second}]"
    val driverVisible = if (driver != null) "block" else "none"
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
        var map, driverMark;
        ymaps.ready(init);
        function init() {
          var points = [[${from.first}, ${from.second}], [${to.first}, ${to.second}]];
          map = new ymaps.Map("map", { center: points[0], zoom: 12, controls: [] });
          map.controls.add('zoomControl');
          map.geoObjects.add(new ymaps.Placemark(points[0], { iconContent: 'А' },
            { preset: 'islands#greenCircleIcon' }));
          map.geoObjects.add(new ymaps.Placemark(points[1], { iconContent: 'В' },
            { preset: 'islands#redCircleIcon' }));
          driverMark = new ymaps.Placemark($driverInit, {}, { preset: 'islands#autoIcon' });
          driverMark.options.set('visibility', '$driverVisible');
          map.geoObjects.add(driverMark);
          buildRoute(points);
        }
        function buildRoute(points) {
          ymaps.multiRouter.route({ referencePoints: points }, { routingMode: 'auto' })
            .then(function (multiRoute) {
              var route = multiRoute.get('routes').get(0);
              if (!route) { window.Android.onRouteResult(false); return; }
              route.setParent(map.geoObjects);
              map.setBounds(route.geometry.getBounds(), { checkZoomRange: true });
              window.Android.onRouteResult(true);
            }, function () { window.Android.onRouteResult(false); });
        }
        function setDriver(lat, lon) {
          if (!driverMark) return;
          driverMark.options.set('visibility', 'block');
          driverMark.geometry.setCoordinates([lat, lon]);
        }
        </script>
        </body>
        </html>
    """.trimIndent()
}
