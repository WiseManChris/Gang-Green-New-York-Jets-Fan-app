package com.example.ganggreen.data.network

import com.example.ganggreen.data.model.ScoreboardResponse
import com.example.ganggreen.data.model.RosterResponse
import retrofit2.http.GET
import retrofit2.http.Query

import com.example.ganggreen.data.model.PlayByPlayResponse

interface EspnApi {
    @GET("apis/site/v2/sports/football/nfl/teams/20/schedule")
    suspend fun getSchedule(): ScoreboardResponse

    @GET("apis/site/v2/sports/football/nfl/scoreboard")
    suspend fun getScoreboard(): ScoreboardResponse

    @GET("apis/site/v2/sports/football/nfl/teams/20/roster")
    suspend fun getRoster(): RosterResponse

    @GET("apis/site/v2/sports/football/nfl/summary")
    suspend fun getPlayByPlay(@Query("event") eventId: String): PlayByPlayResponse
}
