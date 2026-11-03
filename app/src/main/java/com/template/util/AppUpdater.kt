package com.template.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.template.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val version: String,
    val versionCode: Int,
    val fileSize: Long,
    val changelog: String,
    val fileName: String,
    val hasUpdate: Boolean
)

object AppUpdater {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
            val appName = BuildConfig.APP_NAME
            val request = Request.Builder()
                .url("${baseUrl}/releases/latest/${appName}")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            if (!json.optBoolean("success", false)) return@withContext null
            val data = json.optJSONObject("data") ?: return@withContext null

            val latestVersionCode = data.optInt("versionCode", 0)
            val hasUpdate = latestVersionCode > BuildConfig.VERSION_CODE

            AppUpdateInfo(
                version = data.optString("version", ""),
                versionCode = latestVersionCode,
                fileSize = data.optLong("fileSize", 0),
                changelog = data.optString("changelog", ""),
                fileName = data.optString("fileName", ""),
                hasUpdate = hasUpdate
            )
        } catch (_: Exception) { null }
    }

    fun downloadAndInstall(context: Context, update: AppUpdateInfo) {
        val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
        val appName = BuildConfig.APP_NAME
        val downloadUrl = "${baseUrl}/releases/download/${appName}/${update.fileName}"

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(downloadUrl)

        val request = DownloadManager.Request(uri)
            .setTitle("Обновление AI-Такси")
            .setDescription("Скачивание версии ${update.version}...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, update.fileName)
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    ctx.unregisterReceiver(this)
                    val file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        update.fileName
                    )
                    installApk(ctx, file)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
fun UpdateDialog(update: AppUpdateInfo, onDismiss: () -> Unit, onUpdate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Доступно обновление ${update.version}") },
        text = {
            Text(
                if (update.changelog.isNotBlank()) update.changelog
                else "Новая версия приложения. Рекомендуем обновить."
            )
        },
        confirmButton = {
            Button(onClick = onUpdate) { Text("Обновить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Позже") }
        }
    )
}

@Composable
fun AutoUpdateCheck(onUpdateAvailable: (AppUpdateInfo) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val update = AppUpdater.checkForUpdate(context)
        if (update != null && update.hasUpdate) {
            onUpdateAvailable(update)
        }
    }
}
