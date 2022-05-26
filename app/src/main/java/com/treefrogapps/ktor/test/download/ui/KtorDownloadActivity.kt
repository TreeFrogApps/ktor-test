package com.treefrogapps.ktor.test.download.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.treefrogapps.ktor.test.download.viewmodel.KtorViewModel
import com.treefrogapps.ktor.test.theme.KtorTestTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class KtorDownloadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KtorTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val viewModel: KtorViewModel by viewModel()
                    KtorDownloadScreen(
                        onDownloadUrl = viewModel::onAddDownload,
                        onClear = viewModel::onClearCompleted,
                        downloads = viewModel.downloads.collectAsState())
                }
            }
        }
    }
}
