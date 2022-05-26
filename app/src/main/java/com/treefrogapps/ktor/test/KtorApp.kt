package com.treefrogapps.ktor.test

import android.app.Application
import androidx.work.WorkManager
import com.treefrogapps.ktor.test.download.repository.DownloadRepository
import com.treefrogapps.ktor.test.download.viewmodel.KtorViewModel
import com.treefrogapps.ktor.test.download.workers.ChunkedDownloadListenableWorker
import com.treefrogapps.ktor.test.download.workers.DownloadListenableWorker
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.dsl.module
import timber.log.Timber

class KtorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@KtorApp)
            modules(
                networkModule(),
                workModule(),
                repositoryModule(),
                viewModelModule())
        }
    }

    private fun networkModule(): Module = module {
        single {
            HttpClient(OkHttp) {
                expectSuccess = true
                install(Logging) {
                    level = LogLevel.INFO
                }
                install(HttpTimeout) {
                    val timeoutMillis = 45_000L
                    // leave other timeouts high - downloading files may take time to complete ..
                    connectTimeoutMillis = timeoutMillis
                }
            }
        }
    }

    private fun KoinApplication.workModule(): Module = module {
        workManagerFactory()
        worker { parametersHolder ->
            DownloadListenableWorker(
                client = get(),
                context = get(),
                params = parametersHolder.get())
        }
        worker { parametersHolder ->
            ChunkedDownloadListenableWorker(
                client = get(),
                context = get(),
                params = parametersHolder.get())
        }
        single { WorkManager.getInstance(androidContext()) }
    }

    private fun repositoryModule(): Module = module {
        factory {
            DownloadRepository(
                context = androidContext(),
                workManager = get())
        }
    }

    private fun viewModelModule(): Module = module {
        viewModel { KtorViewModel(downloadRepository = get()) }
    }
}