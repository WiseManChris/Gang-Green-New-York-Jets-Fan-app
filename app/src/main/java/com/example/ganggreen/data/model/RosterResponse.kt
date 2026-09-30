package com.example.ganggreen.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RosterResponse(
    val athletes: List<AthleteItem> = emptyList()
)

@Serializable
data class AthleteItem(
    val position: String = "",
    val items: List<Athlete> = emptyList()
)

@Serializable
data class Athlete(
    val id: String? = null,
    val fullName: String? = null,
    val jersey: String? = null,
    val headshot: Headshot? = null,
    val age: Int? = null,
    val experience: Experience? = null,
    val injuries: List<Injury> = emptyList(),
    val position: Position? = null,
    val college: College? = null,
    var depthChartRank: Int = 99
)

@Serializable
data class Headshot(
    val href: String = ""
)

@Serializable
data class Experience(
    val years: Int = 0
)

@Serializable
data class Position(
    val abbreviation: String = "",
    val displayName: String = ""
)

@Serializable
data class College(
    val name: String = "",
    val shortName: String = ""
)
@Serializable
data class Injury(
    val status: String = "",
    val date: String = ""
)
