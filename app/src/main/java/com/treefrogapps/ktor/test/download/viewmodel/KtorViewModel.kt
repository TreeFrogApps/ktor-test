package com.treefrogapps.ktor.test.download.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.treefrogapps.ktor.test.download.repository.Download
import com.treefrogapps.ktor.test.download.repository.DownloadRepository
import com.treefrogapps.ktor.test.download.repository.Downloadable
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.*

class KtorViewModel(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val downloads: StateFlow<List<Download>> by lazy {
        downloadRepository.observe()
            .stateIn(scope = viewModelScope,
                started = WhileSubscribed(5000),
                initialValue = emptyList())
    }

    fun onAddDownload(downloadUrl: String) {
        downloadRepository.add(
            t = Downloadable(
                urlString = downloadUrl,
                outputFilename = UUID.randomUUID().toString()))
    }

    fun onClearCompleted() {
        downloadRepository.clean()
    }
}