package com.example.ganggreen.data

import com.example.ganggreen.data.model.ScoreboardResponse
import com.example.ganggreen.data.model.RosterResponse
import com.example.ganggreen.data.model.Event
import com.example.ganggreen.data.model.PlayByPlayResponse
import com.example.ganggreen.data.network.EspnApi
import com.example.ganggreen.data.network.NewsItem
import com.example.ganggreen.data.network.RssParser
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class DataRepository {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { 
            level = if (com.example.ganggreen.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE 
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://site.api.espn.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val espnApi = retrofit.create(EspnApi::class.java)
    private val rssParser = RssParser(client)

    suspend fun getScoreboard(): ScoreboardResponse {
        return espnApi.getScoreboard()
    }

    suspend fun getJetsEvent(): Event? {
        val schedule = espnApi.getSchedule()
        
        val nowMs = System.currentTimeMillis()
        val events = schedule.events

        // 1. Check for live game
        val liveGame = events.find { it.competitions.firstOrNull()?.status?.type?.state == "in" }
        if (liveGame != null) return liveGame

        // 2. Check for recently finished game (within last 36 hours)
        val postGames = events.filter { it.competitions.firstOrNull()?.status?.type?.state == "post" }
        val lastPostGame = postGames.lastOrNull()
        if (lastPostGame != null) {
            val dateStr = lastPostGame.competitions.firstOrNull()?.date ?: lastPostGame.date
            try {
                var safeDateStr = dateStr
                if (safeDateStr.count { it == ':' } == 1) {
                    safeDateStr = safeDateStr.replace("Z", ":00Z")
                }
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val gameTimeMs = format.parse(safeDateStr)?.time ?: 0L
                val timeSinceGameMs = nowMs - gameTimeMs
                // If the game happened within the last 36 hours, show it. Otherwise show next week's game.
                if (timeSinceGameMs in 0..(36L * 60 * 60 * 1000)) {
                    return lastPostGame
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Check for next upcoming game
        val preGames = events.filter { it.competitions.firstOrNull()?.status?.type?.state == "pre" }
        val nextPreGame = preGames.firstOrNull()
        if (nextPreGame != null) return nextPreGame

        // 4. Fallback to the last post game
        return lastPostGame
    }



    suspend fun getRoster(): com.example.ganggreen.data.model.RosterResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val roster = espnApi.getRoster()
        try {
            val depthUrl = "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/seasons/2026/teams/20/depthcharts"
            val req = okhttp3.Request.Builder().url(depthUrl).build()
            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                val body = res.body?.string()
                if (body != null) {
                    val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                    val items = jsonObj["items"]?.jsonArray
                    val rankMap = mutableMapOf<String, Int>()
                    items?.forEach { item ->
                        val positions = item.jsonObject["positions"]?.jsonObject
                        positions?.entries?.forEach { (_, posObj) ->
                            val athletesArray = posObj.jsonObject["athletes"]?.jsonArray
                            athletesArray?.forEach { athObj ->
                                val rank = athObj.jsonObject["rank"]?.jsonPrimitive?.content?.toIntOrNull() ?: 99
                                val ref = athObj.jsonObject["athlete"]?.jsonObject?.get("\$ref")?.jsonPrimitive?.content
                                if (ref != null) {
                                    val id = ref.substringAfter("athletes/").substringBefore("?")
                                    rankMap[id] = rank
                                }
                            }
                        }
                    }
                    roster.athletes.forEach { group ->
                        group.items.forEach { athlete ->
                            athlete.depthChartRank = rankMap[athlete.id] ?: 99
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        roster
    }

    suspend fun getNews(url: String): List<NewsItem> {
        return rssParser.fetchNews(url)
    }

    suspend fun getTwitterNews(): List<NewsItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val tweets = mutableListOf<NewsItem>()
        try {
            val url = "https://syndication.twitter.com/srv/timeline-profile/screen-name/nyjets"
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: return@withContext emptyList()
                val doc = org.jsoup.Jsoup.parse(html)
                val script = doc.getElementById("__NEXT_DATA__")
                if (script != null) {
                    val jsonObj = Json.parseToJsonElement(script.html()).jsonObject
                    val props = jsonObj["props"]?.jsonObject
                    val pageProps = props?.get("pageProps")?.jsonObject
                    val timeline = pageProps?.get("timeline")?.jsonObject
                    val entries = timeline?.get("entries")?.jsonArray
                    entries?.forEach { entry ->
                        val type = entry.jsonObject["type"]?.jsonPrimitive?.content
                        if (type == "tweet") {
                            val tweet = entry.jsonObject["content"]?.jsonObject?.get("tweet")?.jsonObject
                            if (tweet != null) {
                                val text = tweet["full_text"]?.jsonPrimitive?.content ?: ""
                                val idStr = tweet["id_str"]?.jsonPrimitive?.content ?: ""
                                
                                var imgUrl: String? = null
                                val extendedEntities = tweet["extended_entities"]?.jsonObject ?: tweet["entities"]?.jsonObject
                                val media = extendedEntities?.get("media")?.jsonArray
                                if (media != null && media.isNotEmpty()) {
                                    imgUrl = media[0].jsonObject["media_url_https"]?.jsonPrimitive?.content
                                    if (imgUrl != null && !imgUrl.contains("name=")) {
                                        imgUrl += "?name=large"
                                    } else if (imgUrl != null) {
                                        imgUrl = imgUrl.replace(Regex("name=[a-z]+"), "name=large")
                                    }
                                }
                                
                                val title = text.take(50) + if (text.length > 50) "..." else ""
                                val createdAt = tweet["created_at"]?.jsonPrimitive?.content ?: "Recent"
                                tweets.add(NewsItem(title, "https://x.com/nyjets/status/$idStr", text, imgUrl, createdAt))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        tweets
    }

    suspend fun getPlayByPlay(eventId: String): PlayByPlayResponse {
        val pbp = espnApi.getPlayByPlay(eventId)
        return try {
            val probUrl = "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/events/$eventId/competitions/$eventId/probabilities"
            val req = okhttp3.Request.Builder().url(probUrl).build()
            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                val body = res.body?.string()
                if (body != null) {
                    val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                    val items = jsonObj["items"]?.jsonArray
                    val history = mutableListOf<com.example.ganggreen.data.model.WinProbability>()
                    items?.forEach { item ->
                        val obj = item.jsonObject
                        val home = obj["homeWinPercentage"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                        val away = obj["awayWinPercentage"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                        history.add(com.example.ganggreen.data.model.WinProbability(home, away))
                    }
                    pbp.copy(winProbabilityHistory = history)
                } else pbp
            } else pbp
        } catch (e: Exception) {
            e.printStackTrace()
            pbp
        }
    }

    suspend fun fetchArticleBody(url: String): List<com.example.ganggreen.data.model.ArticleBlock> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val blocks = mutableListOf<com.example.ganggreen.data.model.ArticleBlock>()
        try {
            if (url.contains("twitter.com") || url.contains("x.com")) {
                val tweetId = url.substringAfterLast("/")
                val apiUrl = "https://cdn.syndication.twimg.com/tweet-result?id=$tweetId&token=a"
                val apiReq = okhttp3.Request.Builder().url(apiUrl).build()
                val apiRes = client.newCall(apiReq).execute()
                if (apiRes.isSuccessful) {
                    val jsonString = apiRes.body?.string()
                    if (jsonString != null) {
                        val jsonObj = Json.parseToJsonElement(jsonString).jsonObject
                        
                        val userObj = jsonObj["user"]?.jsonObject
                        if (userObj != null) {
                            val name = userObj["name"]?.jsonPrimitive?.content ?: ""
                            val screenName = userObj["screen_name"]?.jsonPrimitive?.content ?: ""
                            val profileImageUrl = userObj["profile_image_url_https"]?.jsonPrimitive?.content ?: ""
                            blocks.add(com.example.ganggreen.data.model.ArticleBlock.TweetHeader(name, screenName, profileImageUrl))
                        }

                        val text = jsonObj["text"]?.jsonPrimitive?.content ?: ""
                        blocks.add(com.example.ganggreen.data.model.ArticleBlock.Text(text))

                        val media = jsonObj["mediaDetails"]?.jsonArray
                        if (media != null && media.isNotEmpty()) {
                            val mediaObj = media[0].jsonObject
                            val type = mediaObj["type"]?.jsonPrimitive?.content
                            if (type == "video") {
                                val variants = mediaObj["video_info"]?.jsonObject?.get("variants")?.jsonArray
                                var bestVideoUrl: String? = null
                                var maxBitrate = -1
                                variants?.forEach { v ->
                                    val variant = v.jsonObject
                                    if (variant["content_type"]?.jsonPrimitive?.content == "video/mp4") {
                                        val bitrate = variant["bitrate"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                                        if (bitrate > maxBitrate) {
                                            maxBitrate = bitrate
                                            bestVideoUrl = variant["url"]?.jsonPrimitive?.content
                                        }
                                    }
                                }
                                if (bestVideoUrl != null) {
                                    val thumbnailUrl = mediaObj["media_url_https"]?.jsonPrimitive?.content
                                    val arArray = mediaObj["video_info"]?.jsonObject?.get("aspect_ratio")?.jsonArray
                                    val aspectRatio = if (arArray != null && arArray.size == 2) {
                                        arArray[0].jsonPrimitive.content.toFloat() / arArray[1].jsonPrimitive.content.toFloat()
                                    } else 16f/9f
                                    blocks.add(com.example.ganggreen.data.model.ArticleBlock.Video(bestVideoUrl!!, thumbnailUrl, aspectRatio))
                                } else {
                                    val imgUrl = mediaObj["media_url_https"]?.jsonPrimitive?.content
                                    if (imgUrl != null) blocks.add(com.example.ganggreen.data.model.ArticleBlock.Image(imgUrl, null))
                                }
                            } else {
                                val imgUrl = mediaObj["media_url_https"]?.jsonPrimitive?.content
                                if (imgUrl != null) blocks.add(com.example.ganggreen.data.model.ArticleBlock.Image(imgUrl, null))
                            }
                        }
                    }
                }
                return@withContext blocks
            }

            var rawHtml: String? = null
            
            // Try ESPN Core API first
            val idMatch = Regex("/id/(\\d+)").find(url)
            if (idMatch != null) {
                val storyId = idMatch.groupValues[1]
                val apiUrl = "https://content.core.api.espn.com/v1/sports/news/$storyId"
                val apiReq = okhttp3.Request.Builder().url(apiUrl).build()
                val apiRes = client.newCall(apiReq).execute()
                if (apiRes.isSuccessful) {
                    val jsonString = apiRes.body?.string()
                    if (jsonString != null) {
                        try {
                            val jsonObj = Json.parseToJsonElement(jsonString).jsonObject
                            val headlines = jsonObj["headlines"]?.jsonArray
                            val headlineObj = headlines?.get(0)?.jsonObject
                            
                            // Add main header image if exists
                            val images = headlineObj?.get("images")?.jsonArray
                            if (images != null && images.isNotEmpty()) {
                                val imgUrl = images[0].jsonObject["url"]?.jsonPrimitive?.content
                                val imgCaption = images[0].jsonObject["caption"]?.jsonPrimitive?.content
                                if (imgUrl != null) {
                                    blocks.add(com.example.ganggreen.data.model.ArticleBlock.Image(imgUrl, imgCaption))
                                }
                            }
                            
                            rawHtml = headlineObj?.get("story")?.jsonPrimitive?.content
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }

            // Fallback to web scraping if API failed
            if (rawHtml.isNullOrEmpty()) {
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val html = response.body?.string()
                    if (!html.isNullOrEmpty()) {
                        val doc = org.jsoup.Jsoup.parse(html)
                        val articleBody = doc.select(".article-body").first() ?: doc.select(".story-container").first() ?: doc.select("article").first()
                        rawHtml = articleBody?.html() ?: doc.body().html()
                    }
                }
            }
            
            if (rawHtml.isNullOrEmpty()) {
                return@withContext listOf(com.example.ganggreen.data.model.ArticleBlock.Text("Unable to load article content."))
            }

            val doc = org.jsoup.Jsoup.parseBodyFragment(rawHtml)
            val elements = doc.body().select("p, h1, h2, h3, h4, h5, h6, img")
            for (element in elements) {
                when (element.tagName()) {
                    "p" -> {
                        val text = element.text().trim()
                        if (text.isNotEmpty()) blocks.add(com.example.ganggreen.data.model.ArticleBlock.Text(text))
                    }
                    "h1", "h2", "h3", "h4", "h5", "h6" -> {
                        val text = element.text().trim()
                        if (text.isNotEmpty()) blocks.add(com.example.ganggreen.data.model.ArticleBlock.Text(text, isHeader = true))
                    }
                    "img" -> {
                        var src = element.attr("data-src").takeIf { it.isNotEmpty() } ?: element.attr("src")
                        val srcset = element.attr("srcset")
                        if (srcset.isNotEmpty()) {
                            val urls = srcset.split(",").map { it.trim().split(" ")[0] }
                            if (urls.isNotEmpty()) src = urls.last()
                        }
                        if (src.isNotEmpty() && !src.startsWith("data:image")) {
                            var highResSrc = src.replace("/t_lazy", "")
                            highResSrc = highResSrc.replace(Regex("&w=\\d+"), "&w=1080")
                            highResSrc = highResSrc.replace(Regex("\\?w=\\d+"), "?w=1080")
                            // Try to find a caption if it's in a figure
                            val parentFigure = element.parents().select("figure").first()
                            val caption = parentFigure?.select("figcaption")?.text()
                            blocks.add(com.example.ganggreen.data.model.ArticleBlock.Image(highResSrc, caption))
                        }
                    }
                }
            }
            if (blocks.isEmpty()) {
                val text = doc.body().text().trim()
                if (text.isNotEmpty()) blocks.add(com.example.ganggreen.data.model.ArticleBlock.Text(text))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (blocks.isEmpty()) {
                blocks.add(com.example.ganggreen.data.model.ArticleBlock.Text("Unable to load article content."))
            }
        }
        return@withContext blocks
    }

    suspend fun getAthleteStats(id: String): Map<String, List<Pair<String, String>>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val result = mutableMapOf<String, List<Pair<String, String>>>()
        try {
            val statsUrl = "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/seasons/2026/types/2/athletes/$id/statistics"
            val req = okhttp3.Request.Builder().url(statsUrl).build()
            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                val body = res.body?.string()
                if (body != null) {
                    val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                    val categories = jsonObj["splits"]?.jsonObject?.get("categories")?.jsonArray
                    categories?.forEach { catObj ->
                        val name = catObj.jsonObject["name"]?.jsonPrimitive?.content ?: "Stats"
                        val displayName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                        val statsArray = catObj.jsonObject["stats"]?.jsonArray
                        val statList = mutableListOf<Pair<String, String>>()
                        statsArray?.forEach { statObj ->
                            val statName = statObj.jsonObject["displayName"]?.jsonPrimitive?.content ?: ""
                            val statValue = statObj.jsonObject["displayValue"]?.jsonPrimitive?.content ?: ""
                            val floatValue = statObj.jsonObject["value"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                            // Only include key stats or non-zero stats to avoid clutter
                            if (statName.isNotEmpty() && statValue.isNotEmpty() && (floatValue > 0 || statName.contains("Percentage") || statName.contains("Rating"))) {
                                statList.add(statName to statValue)
                            }
                        }
                        if (statList.isNotEmpty()) {
                            result[displayName] = statList
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun getInjuryDetail(athleteId: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val url = "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/seasons/2026/athletes/$athleteId/injuries"
            val req = okhttp3.Request.Builder().url(url).build()
            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                val body = res.body?.string()
                if (body != null) {
                    val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                    val items = jsonObj["items"]?.jsonArray
                    val firstItem = items?.firstOrNull()?.jsonObject
                    val details = firstItem?.get("details")?.jsonObject
                    val type = details?.get("type")?.jsonPrimitive?.content
                    return@withContext type
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
