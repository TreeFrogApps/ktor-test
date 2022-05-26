package com.treefrogapps.ktor.test.download.repository

data class Download(
    val id: String,
    val urlString: String,
    val outputFilename: String,
    val progress: Int,
    val downloadAttempt : Int,
    val state: String)