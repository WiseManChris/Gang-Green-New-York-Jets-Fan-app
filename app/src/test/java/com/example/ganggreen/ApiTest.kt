package com.example.ganggreen

import com.example.ganggreen.data.DataRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ApiTest {
    @Test
    fun testGetJetsEvent() = runBlocking {
        val repo = DataRepository()
        try {
            val event = repo.getJetsEvent()
            println("EVENT ID: ${event?.id}")
            println("EVENT NAME: ${event?.name}")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
