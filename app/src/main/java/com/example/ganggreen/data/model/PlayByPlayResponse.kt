package com.example.ganggreen.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayByPlayResponse(
    val drives: Drives? = null,
    val predictor: Predictor? = null,
    val pickcenter: List<Pickcenter>? = null,
    val boxscore: Boxscore? = null,
    val scoringPlays: List<Play> = emptyList(),
    val winProbabilityHistory: List<WinProbability> = emptyList()
)

@Serializable
data class WinProbability(
    val homeWinPercentage: Double = 0.0,
    val awayWinPercentage: Double = 0.0
)

@Serializable
data class Predictor(
    val homeTeam: PredictorTeam? = null,
    val awayTeam: PredictorTeam? = null
)

@Serializable
data class PredictorTeam(
    val id: String = "",
    val gameProjection: String = ""
)

@Serializable
data class Pickcenter(
    val provider: Provider? = null,
    val details: String = "",
    val overUnder: Double = 0.0,
    val spread: Double = 0.0,
    val awayTeamOdds: TeamOdds? = null,
    val homeTeamOdds: TeamOdds? = null
)

@Serializable
data class Provider(
    val name: String = ""
)

@Serializable
data class TeamOdds(
    val moneyLine: Int = 0
)

@Serializable
data class Drives(
    val current: Drive? = null,
    val previous: List<Drive> = emptyList()
)

@Serializable
data class Drive(
    val id: String = "",
    val description: String = "",
    val plays: List<Play> = emptyList(),
    val displayResult: String = "",
    val yards: Int = 0,
    val timeOfPossession: String = "",
    val team: Team? = null
)

@Serializable
data class Play(
    val id: String = "",
    val sequenceNumber: String = "",
    val type: PlayType? = null,
    val text: String = "",
    val awayScore: Int = 0,
    val homeScore: Int = 0,
    val period: PlayPeriod? = null,
    val clock: PlayClock? = null,
    val start: PlaySituation? = null,
    val end: PlaySituation? = null,
    val team: Team? = null
)

@Serializable
data class PlayType(
    val id: String = "",
    val text: String = "",
    val abbreviation: String = ""
)

@Serializable
data class PlayPeriod(
    val number: Int = 0
)

@Serializable
data class PlayClock(
    val displayValue: String = ""
)

@Serializable
data class PlaySituation(
    val down: Int = 0,
    val distance: Int = 0,
    val downDistanceText: String = "",
    val shortDownDistanceText: String = "",
    val possessionText: String = "",
    val yardLine: Int = 0,
    val yardsToEndzone: Int = 0
)


@Serializable
data class Boxscore(
    val teams: List<BoxscoreTeam> = emptyList()
)

@Serializable
data class BoxscoreTeam(
    val team: Team = Team(),
    val statistics: List<Statistic> = emptyList()
)

@Serializable
data class Statistic(
    val name: String = "",
    val displayValue: String = "",
    val label: String = ""
)
