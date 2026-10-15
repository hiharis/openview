// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.hiharis.openview.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

data class UpdateInfo(
  val versionName: String,
  val downloadUrl: String,
  val expectedSha256: String,
  val sizeBytes: Long,
  val releaseNotes: String,
)

sealed class UpdateCheckResult {
  data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult()
  data object UpToDate : UpdateCheckResult()
  data class Error(val message: String) : UpdateCheckResult()
}

sealed class DownloadResult {
  data class Success(val apkFile: File) : DownloadResult()
  data class Error(val message: String) : DownloadResult()
  data object Cancelled : DownloadResult()
}

object UpdateManager {
  private const val TAG = "UpdateManager"
  private const val GITHUB_OWNER = "hiharis"
  private const val GITHUB_REPO = "openview"
  private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
  private const val TIMEOUT_MILLIS = 10_000
  private const val APK_NAME = "openview.apk"

  fun getUpdatesDir(context: Context): File {
    val dir = File(context.cacheDir, "updates")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  fun cleanOldUpdates(context: Context) {
    try {
      val dir = File(context.cacheDir, "updates")
      if (dir.exists()) {
        dir.listFiles()?.forEach { it.delete() }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to clean old updates: ${e.message}")
    }
  }

  suspend fun checkForUpdates(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
      val url = URL(API_URL)
      if (url.protocol != "https") {
        return@withContext UpdateCheckResult.Error("Insecure connection rejected")
      }

      connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = TIMEOUT_MILLIS
        readTimeout = TIMEOUT_MILLIS
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "OpenView")
      }

      val responseCode = connection.responseCode
      if (responseCode != HttpURLConnection.HTTP_OK) {
        return@withContext UpdateCheckResult.Error("HTTP $responseCode from GitHub")
      }

      val responseText = connection.inputStream.bufferedReader().use { it.readText() }
      val json = JSONObject(responseText)

      val isDraft = json.optBoolean("draft", false)
      val isPrerelease = json.optBoolean("prerelease", false)
      if (isDraft || isPrerelease) {
        return@withContext UpdateCheckResult.UpToDate
      }

      val tagName = json.optString("tag_name", "").trim()
      val remoteVersion = tagName.removePrefix("v").removePrefix("V")
      if (remoteVersion.isBlank()) {
        return@withContext UpdateCheckResult.Error("Missing tag_name in release")
      }

      if (!isRemoteVersionHigher(remoteVersion, currentVersion)) {
        return@withContext UpdateCheckResult.UpToDate
      }

      // Find the asset named "openview.apk"
      val assets = json.optJSONArray("assets")
      if (assets == null || assets.length() == 0) {
        return@withContext UpdateCheckResult.Error("No assets found in release")
      }

      var apkUrl: String? = null
      var digestString: String? = null
      var assetSize: Long = 0L

      for (i in 0 until assets.length()) {
        val asset = assets.getJSONObject(i)
        val name = asset.optString("name", "")
        if (name.equals(APK_NAME, ignoreCase = true)) {
          apkUrl = asset.optString("browser_download_url", "")
          digestString = asset.optString("digest", "")
          assetSize = asset.optLong("size", 0L)
          break
        }
      }

      if (apkUrl.isNullOrBlank()) {
        return@withContext UpdateCheckResult.Error("Release does not contain $APK_NAME")
      }

      // Digest field is required per spec ("sha256:<hex>")
      if (digestString.isNullOrBlank() || !digestString.startsWith("sha256:", ignoreCase = true)) {
        return@withContext UpdateCheckResult.Error("Release asset is missing required SHA-256 digest")
      }

      val expectedSha256 = digestString.substringAfter("sha256:").trim().lowercase(Locale.ROOT)
      if (expectedSha256.length != 64) {
        return@withContext UpdateCheckResult.Error("Invalid SHA-256 digest format in release")
      }

      val releaseNotes = json.optString("body", "").take(600)

      UpdateCheckResult.UpdateAvailable(
        UpdateInfo(
          versionName = remoteVersion,
          downloadUrl = apkUrl,
          expectedSha256 = expectedSha256,
          sizeBytes = assetSize,
          releaseNotes = releaseNotes,
        ),
      )
    } catch (e: Exception) {
      Log.e(TAG, "Update check failed: ${e.message}", e)
      UpdateCheckResult.Error(e.message ?: "Network error while checking updates")
    } finally {
      connection?.disconnect()
    }
  }

  suspend fun downloadAndVerifyApk(
    context: Context,
    info: UpdateInfo,
    onProgress: (Int) -> Unit,
    isCancelled: () -> Boolean,
  ): DownloadResult = withContext(Dispatchers.IO) {
    cleanOldUpdates(context)
    val dir = getUpdatesDir(context)
    val targetFile = File(dir, APK_NAME)

    var connection: HttpURLConnection? = null
    try {
      var currentUrl = info.downloadUrl
      var redirects = 0

      // Follow HTTPS redirects
      while (redirects < 5) {
        val url = URL(currentUrl)
        if (url.protocol != "https") {
          targetFile.delete()
          return@withContext DownloadResult.Error("Insecure download URL rejected")
        }

        val conn = (url.openConnection() as HttpURLConnection).apply {
          instanceFollowRedirects = false
          connectTimeout = TIMEOUT_MILLIS
          readTimeout = TIMEOUT_MILLIS
          setRequestProperty("User-Agent", "OpenView")
          setRequestProperty("Accept", "application/octet-stream")
        }

        val code = conn.responseCode
        if (code in 300..399) {
          val location = conn.getHeaderField("Location")
          conn.disconnect()
          if (location.isNullOrBlank()) {
            targetFile.delete()
            return@withContext DownloadResult.Error("Failed to follow redirect")
          }
          currentUrl = location
          redirects++
        } else if (code == HttpURLConnection.HTTP_OK) {
          connection = conn
          break
        } else {
          conn.disconnect()
          targetFile.delete()
          return@withContext DownloadResult.Error("HTTP $code while downloading update")
        }
      }

      val conn = connection ?: run {
        targetFile.delete()
        return@withContext DownloadResult.Error("Too many redirects")
      }

      val totalLength = conn.contentLengthLong.let { if (it > 0) it else info.sizeBytes }
      val inputStream = conn.inputStream
      val outputStream = FileOutputStream(targetFile)
      val digest = MessageDigest.getInstance("SHA-256")

      val buffer = ByteArray(8192)
      var bytesRead: Int
      var downloadedBytes = 0L
      var lastReportedPercent = -1

      try {
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
          if (isCancelled()) {
            outputStream.close()
            inputStream.close()
            targetFile.delete()
            return@withContext DownloadResult.Cancelled
          }
          outputStream.write(buffer, 0, bytesRead)
          digest.update(buffer, 0, bytesRead)
          downloadedBytes += bytesRead

          if (totalLength > 0) {
            val percent = ((downloadedBytes * 100) / totalLength).toInt().coerceIn(0, 100)
            if (percent != lastReportedPercent) {
              lastReportedPercent = percent
              onProgress(percent)
            }
          }
        }
        outputStream.flush()
      } finally {
        outputStream.close()
        inputStream.close()
      }

      if (isCancelled()) {
        targetFile.delete()
        return@withContext DownloadResult.Cancelled
      }

      // Verify SHA-256 digest
      val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
      if (!computedHash.equals(info.expectedSha256, ignoreCase = true)) {
        Log.e(TAG, "SHA-256 mismatch: computed=$computedHash, expected=${info.expectedSha256}")
        targetFile.delete()
        return@withContext DownloadResult.Error("Checksum mismatch: file may be corrupted")
      }

      DownloadResult.Success(targetFile)
    } catch (e: CancellationException) {
      targetFile.delete()
      DownloadResult.Cancelled
    } catch (e: Exception) {
      Log.e(TAG, "Download failed: ${e.message}", e)
      targetFile.delete()
      DownloadResult.Error(e.message ?: "Download failed")
    } finally {
      connection?.disconnect()
    }
  }

  fun canInstallPackages(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.packageManager.canRequestPackageInstalls()
    } else {
      true
    }
  }

  fun launchInstaller(context: Context, apkFile: File): Boolean {
    return try {
      val authority = "${context.packageName}.fileprovider"
      val contentUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
      false
    }
  }

  fun getManageUnknownAppSourcesIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    } else {
      Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }
  }

  fun getInstalledVersionName(context: Context): String {
    return try {
      val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
      } else {
        context.packageManager.getPackageInfo(context.packageName, 0)
      }
      pInfo.versionName ?: "1.0"
    } catch (_: Exception) {
      "1.0"
    }
  }

  fun isRemoteVersionHigher(remote: String, current: String): Boolean {
    try {
      val remoteParts = remote.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
      val currentParts = current.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

      val maxLen = maxOf(remoteParts.size, currentParts.size)
      for (i in 0 until maxLen) {
        val r = remoteParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (r > c) return true
        if (r < c) return false
      }
      return false
    } catch (_: Exception) {
      return false
    }
  }
}

@Composable
fun UpdateDialog(
  info: UpdateInfo,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var isDownloading by remember { mutableStateOf(false) }
  var downloadProgress by remember { mutableIntStateOf(0) }
  var downloadError by remember { mutableStateOf<String?>(null) }
  var isCancelled by remember { mutableStateOf(false) }
  var showPermissionDialog by remember { mutableStateOf(false) }

  if (showPermissionDialog) {
    AlertDialog(
      onDismissRequest = { showPermissionDialog = false },
      title = { Text(stringResource(R.string.update_dialog_title)) },
      text = { Text(stringResource(R.string.update_permission_required)) },
      confirmButton = {
        Button(
          onClick = {
            showPermissionDialog = false
            context.startActivity(UpdateManager.getManageUnknownAppSourcesIntent(context))
          },
        ) {
          Text(stringResource(R.string.update_button_settings))
        }
      },
      dismissButton = {
        TextButton(onClick = { showPermissionDialog = false }) {
          Text(stringResource(R.string.update_button_cancel))
        }
      },
    )
    return
  }

  val currentError = downloadError
  if (currentError != null) {
    AlertDialog(
      onDismissRequest = {
        downloadError = null
        onDismiss()
      },
      title = { Text(stringResource(R.string.update_dialog_title)) },
      text = { Text(currentError) },
      confirmButton = {
        Button(
          onClick = {
            downloadError = null
            onDismiss()
          },
        ) {
          Text(stringResource(android.R.string.ok))
        }
      },
    )
    return
  }

  if (isDownloading) {
    AlertDialog(
      onDismissRequest = { /* prevent outside dismissal while downloading */ },
      title = { Text(stringResource(R.string.update_dialog_title)) },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = if (downloadProgress < 100) {
              stringResource(R.string.update_status_downloading, downloadProgress)
            } else {
              stringResource(R.string.update_status_verifying)
            },
          )
          LinearProgressIndicator(
            progress = { downloadProgress / 100f },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(
          onClick = {
            isCancelled = true
            isDownloading = false
            UpdateManager.cleanOldUpdates(context)
            onDismiss()
          },
        ) {
          Text(stringResource(R.string.update_button_cancel))
        }
      },
    )
    return
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.update_dialog_title),
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Text(
        text = stringResource(R.string.update_dialog_message),
        style = MaterialTheme.typography.bodyMedium,
      )
    },
    confirmButton = {
      Button(
        onClick = {
          if (!UpdateManager.canInstallPackages(context)) {
            showPermissionDialog = true
          } else {
            isDownloading = true
            isCancelled = false
            downloadProgress = 0
            coroutineScope.launch {
              val result = UpdateManager.downloadAndVerifyApk(
                context = context,
                info = info,
                onProgress = { p -> downloadProgress = p },
                isCancelled = { isCancelled },
              )
              isDownloading = false
              when (result) {
                is DownloadResult.Success -> {
                  val launched = UpdateManager.launchInstaller(context, result.apkFile)
                  if (!launched) {
                    downloadError = "Could not open installer"
                  } else {
                    onDismiss()
                  }
                }
                is DownloadResult.Error -> {
                  downloadError = result.message
                }
                DownloadResult.Cancelled -> {
                  onDismiss()
                }
              }
            }
          }
        },
      ) {
        Text(stringResource(R.string.update_button_update_now))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.update_button_not_now))
      }
    },
  )
}
