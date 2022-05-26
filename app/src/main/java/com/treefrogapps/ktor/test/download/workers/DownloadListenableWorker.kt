package com.treefrogapps.ktor.test.download.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import timber.log.Timber
import java.io.File
import kotlin.math.roundToInt

class DownloadListenableWorker(
    private val client: HttpClient,
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params) {

    init {
        createChannel(context)
    }

    override suspend fun doWork(): Result {
        val url = params.inputData.downloadUrl()
        val outputFile = File(params.inputData.outputFilename())

        return runCatching {
            client.get(urlString = url) {
                onDownload { bytesSentTotal, contentLength ->
                    val progress = (bytesSentTotal * 100F / contentLength.toDouble()).roundToInt()
                    val progressData = progressData(
                        urlString = url,
                        outputFilename = outputFile.name,
                        progress = progress)
                    setProgress(progressData)
                }
            }.bodyAsChannel()
                .copyAndClose(outputFile.writeChannel())
        }.onSuccess {
            Timber.i("downloading ${params.inputData.downloadUrl()} completed")
        }.onFailure { e ->
            Timber.e(e, "downloading ${params.inputData.downloadUrl()} failed")
        }.toListenableResult(
            retry = { t ->
                when (t) {
                    is ClientRequestException ->
                        t.response.status.value != HttpStatusCode.NotFound.value || t.cause !is OutOfMemoryError
                    else                      -> false
                }
            })
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(context, params.inputData.downloadUrl())
}