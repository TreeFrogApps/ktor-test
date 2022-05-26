package com.treefrogapps.ktor.test.download.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.treefrogapps.ktor.test.download.repository.Download
import java.util.*
import kotlin.random.Random


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KtorDownloadScreen(
    onDownloadUrl: (downloadUrl: String) -> Unit,
    onClear: () -> Unit,
    downloads: State<List<Download>>
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var url by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        OutlinedTextField(
            label = { Text("Download url") },
            maxLines = 1,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            value = url,
            onValueChange = { value -> url = value },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onDownloadUrl(url)
                    keyboardController?.hide()
                }))
        Row(
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = onClear,
                content = { Text(text = "Clear Done") })
            OutlinedButton(
                onClick = {
                    onDownloadUrl(url)
                    keyboardController?.hide()
                },
                content = { Text(text = "Download") })
        }

        LazyColumn {
            items(
                items = downloads.value,
                key = Download::id,
                itemContent = { download ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        elevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            content = {
                                Column(
                                    modifier = Modifier
                                        .weight(weight = 1.0F)
                                        .padding(all = 8.dp),
                                    content = {
                                        SpannableText(id = "Id : ", content = download.id)
                                        SpannableText(id = "Attempt : ", content = download.downloadAttempt.toString())
                                        SpannableText(id = "State : ", content = download.state)
                                        SpannableText(id = "Url : ", content = download.urlString)
                                        SpannableText(id = "Filename : ", content = download.outputFilename)
                                    })
                                Column(
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .padding(all = 8.dp),
                                    content = {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            content = {
                                                when (download.state) {
                                                    "SUCCEEDED" -> DownloadSucceeded()
                                                    "ENQUEUED"  -> DownloadEnqueued()
                                                    "CANCELLED",
                                                    "FAILED"    -> DownloadCancelled()
                                                    "RUNNING"   -> DownloadRunning(progress = download.progress)
                                                }
                                            })
                                    })
                            })
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                })
        }
    }
}

@Composable
private fun SpannableText(id: String, content: String) {
    Text(
        modifier = Modifier.padding(all = 4.dp),
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(Color.Blue)) {
                append(id)
            }
            append(content)
        })
}

@Composable
private fun DownloadSucceeded() {
    Icon(
        modifier = Modifier.size(48.dp),
        imageVector = Icons.Default.CheckCircle,
        tint = Color.Green,
        contentDescription = null)
}

@Composable
private fun DownloadRunning(progress: Int) {
    CircularProgressIndicator(
        progress = 100F, color = Color.LightGray)
    CircularProgressIndicator(
        progress = progress.toFloat() / 100F)
    Text(
        style = MaterialTheme.typography.caption,
        text = "${progress}%",
        color = MaterialTheme.colors.primary)
}

@Composable
private fun DownloadEnqueued() {
    Icon(
        modifier = Modifier.size(48.dp),
        imageVector = Icons.Default.Lock,
        tint = Color.LightGray,
        contentDescription = null)
}

@Composable
private fun DownloadCancelled() {
    Icon(
        modifier = Modifier.size(48.dp),
        imageVector = Icons.Default.Close,
        tint = Color.Red,
        contentDescription = null)
}


@Preview(
    showBackground = true,
    showSystemUi = true)
@Composable
private fun DownloadScreenPreview() {
    val ran1 = remember { Random(0) }
    val ran2 = remember { Random(1) }
    val states = remember { listOf("RUNNING", "FAILED", "ENQUEUED", "CANCELLED", "SUCCEEDED") }

    KtorDownloadScreen(
        onDownloadUrl = {},
        onClear = {},
        downloads = remember {
            mutableStateOf((0 until 10).map { idx ->
                Download(
                    id = UUID.randomUUID().toString(),
                    urlString = "http://testurl$idx.pdf",
                    outputFilename = UUID.randomUUID().toString(),
                    progress = ran1.nextInt(until = 100),
                    downloadAttempt = ran2.nextInt(1, 10),
                    state = states[ran2.nextInt(0, states.size)]
                )
            })
        })
}