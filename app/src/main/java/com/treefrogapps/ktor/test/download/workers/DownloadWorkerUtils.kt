package com.treefrogapps.ktor.test.download.workers

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.*
import com.treefrogapps.ktor.test.download.ui.KtorDownloadActivity
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.*

const val DOWNLOAD_TAG = "download_task_tag"
const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
const val DOWNLOAD_NOTIFICATION_NAME = "downloads"
const val DOWNLOAD_NOTIFICATION_ID = 10
const val DOWNLOAD_URL_KEY = "download_key"
const val FILENAME_KEY = "filename_key"
const val PROGRESS_KEY = "progress_key"

fun Data.downloadUrl(): String =
    requireNotNull(getString(DOWNLOAD_URL_KEY))

fun Data.outputFilename(): String =
    requireNotNull(getString(FILENAME_KEY))

fun <T> Result<T>.toListenableResult(
    successData: Data = Data.EMPTY,
    retry: (Throwable?) -> Boolean = { true },
    failureData: Data = Data.EMPTY
): ListenableWorker.Result = when {
    isSuccess                -> ListenableWorker.Result.success(successData)
    retry(exceptionOrNull()) -> ListenableWorker.Result.retry()
    else                     -> ListenableWorker.Result.failure(failureData)
}

fun <T> LiveData<T>.asFlow(): Flow<T> = channelFlow {
    val observer: Observer<T> = Observer<T>(this::trySend)
    this@asFlow.observeForever(observer)
    awaitClose { this@asFlow.removeObserver(observer) }
}.distinctUntilChanged()

fun workDownloadData(downloadUrl: String, outputFilename: String): Data = Data.Builder()
    .putString(DOWNLOAD_URL_KEY, downloadUrl)
    .putString(FILENAME_KEY, outputFilename)
    .build()

fun progressData(
    urlString: String,
    outputFilename: String,
    progress: Int
): Data = Data.Builder()
    .putString(DOWNLOAD_URL_KEY, urlString)
    .putString(FILENAME_KEY, outputFilename)
    .putInt(PROGRESS_KEY, progress)
    .build()

fun OneTimeWorkRequest.Builder.addDownloadTag(): OneTimeWorkRequest.Builder = addTag(DOWNLOAD_TAG)

fun WorkManager.downloadWorkInfoListFlow(): Flow<List<WorkInfo>> =
    getWorkInfosByTagLiveData(DOWNLOAD_TAG).asFlow()

fun WorkManager.downloadWorkInfoFlow(uuid: UUID): Flow<WorkInfo> =
    getWorkInfoByIdLiveData(uuid).asFlow()

fun WorkInfo.idAsString(): String = id.toString()

fun WorkInfo.downloadProgress(): Int = progress.getInt(PROGRESS_KEY, 0)

fun WorkInfo.urlString(): String = progress.getString(DOWNLOAD_URL_KEY) ?: ""

fun WorkInfo.outputFilename(): String = progress.getString(FILENAME_KEY) ?: ""

fun WorkInfo.downloadAttempt(): Int = runAttemptCount

fun WorkInfo.stateAsString(): String = state.toString()

fun foregroundInfo(context: Context, downloadUrl: String): ForegroundInfo =
    when {
        isEqualOrAboveApi29() -> ForegroundInfo(DOWNLOAD_NOTIFICATION_ID, createNotification(context, downloadUrl), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else                  -> ForegroundInfo(DOWNLOAD_NOTIFICATION_ID, createNotification(context, downloadUrl))
    }

fun createChannel(context: Context) {
    context.getSystemService<NotificationManager>()?.run {
        createNotificationChannel(NotificationChannel(
            DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            DOWNLOAD_NOTIFICATION_NAME,
            NotificationManager.IMPORTANCE_LOW))
    }
}

private fun createNotification(
    context: Context,
    downloadUrl : String
): Notification =
    NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(com.treefrogapps.ktor.test.R.drawable.ic_download_icon)
        .setContentTitle("Downloading File")
        .setContentIntent(TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(Intent(context, KtorDownloadActivity::class.java))
            getPendingIntent(100, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) })
        .setContentText(downloadUrl)
        .build()

private fun isEqualOrAboveApi29(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

fun HttpResponse.contentLength(): Long =
    runCatching {
        headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
    }.getOrDefault(defaultValue = 0L)