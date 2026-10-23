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

    @Test
    fun testGetPlayByPlayWithHeader() = runBlocking {
        val repo = DataRepository()
        // Use the Patriots vs Jets event ID
        val pbp = repo.getPlayByPlay("401873001")
        println("HEADER ID: ${pbp.header?.id}")
        val comps = pbp.header?.competitions?.firstOrNull()?.competitors
        comps?.forEach { c ->
            println("COMPETITOR: ${c.team.abbreviation} score=${c.displayScore} linescores=${c.linescores.map { it.displayValue }}")
        }
        assert(comps != null && comps.isNotEmpty())
    }

    @Test
    fun testKickoffDateParsing() {
        val repo = DataRepository()
        val millis1 = repo.parseGameDateToMillis("2026-10-25T17:00Z")
        val millis2 = repo.parseGameDateToMillis("2026-10-25T17:00:00Z")
        assert(millis1 > 0L)
        assert(millis1 == millis2)
    }

    @Test
    fun testUpcomingGameKickoff() = runBlocking {
        val repo = DataRepository()
        val event = repo.getJetsEvent()
        assert(event != null)
        val comp = event?.competitions?.firstOrNull()
        val dateStr = comp?.date ?: event?.date ?: ""
        val kickoffMs = repo.parseGameDateToMillis(dateStr)
        println("Upcoming Game: ${event?.name} at $kickoffMs (${dateStr})")
        assert(kickoffMs > 0L)
    }
}
