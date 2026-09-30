package com.example.ganggreen.data.model

sealed class ArticleBlock {
    data class Text(val content: String, val isHeader: Boolean = false) : ArticleBlock()
    data class Image(val url: String, val caption: String?) : ArticleBlock()
    data class Video(val url: String, val thumbnailUrl: String? = null, val aspectRatio: Float? = null) : ArticleBlock()
    data class TweetHeader(val name: String, val handle: String, val avatarUrl: String) : ArticleBlock()
}
