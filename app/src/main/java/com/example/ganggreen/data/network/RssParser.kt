package com.example.ganggreen.data.network

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

data class NewsItem(
    val title: String,
    val link: String,
    val description: String,
    val imageUrl: String?,
    val pubDate: String = "Recent"
)

class RssParser(private val client: OkHttpClient) {

    suspend fun fetchNews(url: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            response.body?.byteStream()?.let { parse(it) } ?: emptyList()
        }
    }

    private fun parse(inputStream: InputStream): List<NewsItem> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        parser.nextTag()
        return readFeed(parser)
    }

    private fun readFeed(parser: XmlPullParser): List<NewsItem> {
        val entries = mutableListOf<NewsItem>()
        parser.require(XmlPullParser.START_TAG, null, "rss")
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "channel") {
                while (parser.next() != XmlPullParser.END_TAG || parser.name != "channel") {
                    if (parser.eventType != XmlPullParser.START_TAG) continue
                    if (parser.name == "item") {
                        entries.add(readItem(parser))
                    } else {
                        skip(parser)
                    }
                }
            }
        }
        return entries
    }

    private fun readItem(parser: XmlPullParser): NewsItem {
        parser.require(XmlPullParser.START_TAG, null, "item")
        var title = ""
        var link = ""
        var description = ""
        var imageUrl: String? = null
        var pubDate = "Recent"
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "link" -> link = readText(parser)
                "description" -> description = readText(parser)
                "enclosure" -> {
                    if (imageUrl == null) {
                        imageUrl = parser.getAttributeValue(null, "url")
                        imageUrl = imageUrl?.replace(Regex("\\?w=\\d+"), "?w=1080")
                        imageUrl = imageUrl?.replace(Regex("&w=\\d+"), "&w=1080")
                    }
                    skip(parser)
                }
                "content" -> { // media:content is parsed as "content" when namespaces are disabled
                    if (imageUrl == null) {
                        imageUrl = parser.getAttributeValue(null, "url")
                        imageUrl = imageUrl?.replace(Regex("\\?w=\\d+"), "?w=1080")
                        imageUrl = imageUrl?.replace(Regex("&w=\\d+"), "&w=1080")
                    }
                    skip(parser)
                }
                "image" -> if (imageUrl == null) {
                    imageUrl = readText(parser)
                    imageUrl = imageUrl?.replace(Regex("\\?w=\\d+"), "?w=1080")
                    imageUrl = imageUrl?.replace(Regex("&w=\\d+"), "&w=1080")
                }
                "pubDate" -> pubDate = readText(parser)
                else -> skip(parser)
            }
        }
        return NewsItem(title, link, description, imageUrl, pubDate)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) throw IllegalStateException()
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
