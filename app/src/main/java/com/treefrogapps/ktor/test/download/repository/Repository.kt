package com.treefrogapps.ktor.test.download.repository

import kotlinx.coroutines.flow.Flow

interface Repository<T, V> {

    fun add(t : T)

    fun observe() : Flow<V>
}