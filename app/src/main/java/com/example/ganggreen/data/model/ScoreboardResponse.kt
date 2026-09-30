package com.example.ganggreen.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


@Serializable
data class ScoreboardResponse(
    val events: List<Event> = emptyList()
)

@Serializable
data class Event(
    val id: String = "",
    val name: String = "",
    val shortName: String = "",
    val date: String = "",
    val competitions: List<Competition> = emptyList(),
    val status: Status? = null,
    val weather: Weather? = null
)

@Serializable
data class Weather(
    val displayValue: String = "",
    val temperature: Int = 0
)

@Serializable
data class Status(
    val clock: Double = 0.0,
    val displayClock: String = "",
    val period: Int = 0,
    val type: StatusType? = null
)

@Serializable
data class StatusType(
    val id: String = "",
    val name: String = "",
    val state: String = "",
    val completed: Boolean = false,
    val description: String = "",
    val detail: String = "",
    val shortDetail: String = ""
)

@Serializable
data class Competition(
    val id: String = "",
    val date: String = "",
    val competitors: List<Competitor> = emptyList(),
    val situation: Situation? = null,
    val venue: Venue? = null,
    val broadcasts: List<Broadcast> = emptyList(),
    val status: Status? = null
)

@Serializable
data class Venue(
    val fullName: String = "",
    val address: Address? = null
)

@Serializable
data class Address(
    val city: String = "",
    val state: String = ""
)

@Serializable
data class Broadcast(
    val market: kotlinx.serialization.json.JsonElement? = null,
    val names: List<String> = emptyList()
)

@Serializable
data class Situation(
    val down: Int = 0,
    val distance: Int = 0,
    val downDistanceText: String = "",
    val isRedZone: Boolean = false,
    val possessionText: String = ""
)

@Serializable
data class Competitor(
    val id: String = "",
    val type: String = "",
    val order: Int = 0,
    val homeAway: String = "",
    val team: Team = Team(),
    val score: kotlinx.serialization.json.JsonElement? = null,
    val records: List<Record> = emptyList(),
    val leaders: List<LeaderCategory>? = null
) {
    val displayScore: String
        get() {
            if (score == null) return "0"
            return try {
                if (score is kotlinx.serialization.json.JsonObject) {
                    score.jsonObject["displayValue"]?.jsonPrimitive?.content ?: "0"
                } else {
                    score.jsonPrimitive.content
                }
            } catch (e: Exception) {
                "0"
            }
        }
}

@Serializable
data class Team(
    val id: String = "",
    val location: String = "",
    val name: String = "",
    val abbreviation: String = "",
    val displayName: String = "",
    val color: String = "",
    val logo: String = "",
    val logos: List<LogoObj> = emptyList()
) {
    val displayLogo: String
        get() = logo.ifEmpty { logos.firstOrNull()?.href ?: "" }
}

@Serializable
data class LogoObj(
    val href: String = ""
)

@Serializable
data class Record(
    val name: String = "",
    val type: String = "",
    val summary: String = ""
)

@Serializable
data class LeaderCategory(
    val name: String = "",
    val displayName: String = "",
    val shortDisplayName: String = "",
    val abbreviation: String = "",
    val leaders: List<LeaderItem> = emptyList()
)

@Serializable
data class LeaderItem(
    val displayValue: String = "",
    val value: Double = 0.0,
    val athlete: LeaderAthlete = LeaderAthlete()
)

@Serializable
data class LeaderAthlete(
    val id: String = "",
    val fullName: String = "",
    val shortName: String = "",
    val headshot: String? = null,
    val jersey: String? = null
)
