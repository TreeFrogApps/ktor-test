package com.treefrogapps.ktor.test.download.repository

import android.content.Context
import androidx.work.*
import androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
import com.treefrogapps.ktor.test.download.workers.*

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class DownloadRepository(
    private val context: Context,
    private val workManager: WorkManager
) : Repository<Downloadable, List<Download>> {

    override fun add(t: Downloadable) {
        createRequest<ChunkedDownloadListenableWorker>(
            urlString = t.urlString,
            outputFilename = t.outputFilename)
            .let { request ->
                workManager.enqueue(request)
            }
    }

    override fun observe(): Flow<List<Download>> =
        workManager.downloadWorkInfoListFlow()
            .map { infoList ->
                infoList.map { info ->
                    Download(
                        id = info.idAsString(),
                        urlString = info.urlString(),
                        outputFilename = info.outputFilename(),
                        progress = info.downloadProgress(),
                        downloadAttempt = info.downloadAttempt(),
                        state = info.stateAsString())
                }
            }

    fun clean() {
        workManager.pruneWork()
    }

    private inline fun <reified T : ListenableWorker> createRequest(
        urlString: String,
        outputFilename: String
    ): WorkRequest =
        OneTimeWorkRequestBuilder<T>()
            .setInputData(workDownloadData(
                downloadUrl = urlString,
                outputFilename = File(context.filesDir, outputFilename).absolutePath))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build())
            .addDownloadTag()
            .setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
}