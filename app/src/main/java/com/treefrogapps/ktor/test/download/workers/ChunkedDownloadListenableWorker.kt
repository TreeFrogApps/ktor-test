package com.treefrogapps.ktor.test.download.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import timber.log.Timber
import java.io.File
import kotlin.math.roundToInt

class ChunkedDownloadListenableWorker(
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
        if (outputFile.exists()) outputFile.delete()

        return runCatching {
            client.prepareGet(urlString = url).execute { response ->
                val length = response.contentLength().toFloat()
                var readBytes = 0
                val channel: ByteReadChannel = response.body()
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (packet.isNotEmpty) {
                        val bytes: ByteArray = packet.readBytes()
                        outputFile.appendBytes(array = bytes)
                        readBytes += bytes.size
                        val progress = (readBytes * 100F / length).roundToInt()
                        val progressData = progressData(
                            urlString = url,
                            outputFilename = outputFile.name,
                            progress = progress)
                        setProgress(progressData)
                    }
                }
            }
        }.onSuccess {
            Timber.i("downloading ${params.inputData.downloadUrl()} completed")
        }.onFailure { e ->
            Timber.e(e, "downloading ${params.inputData.downloadUrl()} failed")
        }.toListenableResult(
            retry = { t ->
                when (t) {
                    is ClientRequestException ->
                        !(t.response.status == HttpStatusCode.NotFound || t.cause is OutOfMemoryError)
                    else                      -> false
                }.apply { Timber.e("Failed Download retry enabled : $this") }
            })
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(context, params.inputData.downloadUrl())
}