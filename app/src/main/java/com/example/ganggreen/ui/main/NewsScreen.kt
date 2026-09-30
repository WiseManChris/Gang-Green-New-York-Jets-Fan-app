package com.example.ganggreen.ui.main
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.verticalScroll

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ganggreen.data.network.NewsItem
import com.example.ganggreen.theme.DarkGreenSurface
import com.example.ganggreen.theme.JetsGreenLight
import com.example.ganggreen.theme.TextPrimary
import com.example.ganggreen.theme.TextSecondary

@Composable
fun NewsScreen(viewModel: MainViewModel, settingsManager: com.example.ganggreen.data.SettingsManager) {
    val newsItems by viewModel.newsState.collectAsStateWithLifecycle()
    val articleContent by viewModel.articleContent.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<NewsItem?>(null) }

    if (selectedItem != null) {
        BackHandler {
            selectedItem = null
            viewModel.clearArticle()
        }
        ArticleReader(item = selectedItem!!, content = articleContent) {
            selectedItem = null
            viewModel.clearArticle()
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            val selectedNewsTab by viewModel.selectedNewsTab.collectAsStateWithLifecycle()
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip(text = "Jets", isSelected = selectedNewsTab == "Jets", onClick = { viewModel.selectNewsTab("Jets") })
                Chip(text = "ESPN NFL", isSelected = selectedNewsTab == "ESPN NFL", onClick = { viewModel.selectNewsTab("ESPN NFL") })
            }

            val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

            @OptIn(ExperimentalMaterial3Api::class)
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (newsItems.isEmpty() && !isRefreshing) {
                        item {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = JetsGreenLight)
                            }
                        }
                    } else {
                        items(newsItems) { item ->
                            NewsItemCard(item) {
                                selectedItem = item
                                viewModel.fetchArticle(item.link)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, isSelected: Boolean, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) JetsGreenLight else DarkGreenSurface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}



fun formatPubDate(rawDate: String): String {
    if (rawDate == "Recent") return rawDate
    val formats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z", // Standard RSS
        "EEE MMM dd HH:mm:ss Z yyyy", // Twitter
        "yyyy-MM-dd'T'HH:mm:ss'Z'" // Fallback
    )
    for (f in formats) {
        try {
            val sdf = SimpleDateFormat(f, Locale.US)
            val parsed = sdf.parse(rawDate)
            if (parsed != null) {
                val outFmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)
                outFmt.timeZone = TimeZone.getDefault()
                return outFmt.format(parsed)
            }
        } catch (e: Exception) {}
    }
    return rawDate // Return as is if parsing fails
}

fun getSourceTag(link: String): String {
    return when {
        link.contains("nypost.com") -> "NEW YORK POST"
        link.contains("sny.tv") -> "SNY"
        link.contains("cbssports.com") -> "CBS SPORTS"
        link.contains("yahoo.com") -> "YAHOO SPORTS"
        link.contains("newyorkjets.com") -> "NEWYORKJETS.COM"
        link.contains("espn.com") -> "ESPN"
        link.contains("x.com") || link.contains("twitter.com") -> "OFFICIAL 𝕏"
        else -> "NEWS"
    }
}

@Composable
fun NewsItemCard(item: NewsItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGreenSurface)
    ) {
        Column {
            val isTwitter = item.link.contains("x.com") || item.link.contains("twitter.com")
            
            if (isTwitter) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                    Text("𝕏", color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkGreenSurface.copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(getSourceTag(item.link), color = JetsGreenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (item.imageUrl != null) {
                Box {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkGreenSurface.copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(getSourceTag(item.link), color = JetsGreenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                if (item.imageUrl == null && !isTwitter) {
                    Text(getSourceTag(item.link), color = JetsGreenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formatPubDate(item.pubDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ArticleReader(item: NewsItem, content: List<com.example.ganggreen.data.model.ArticleBlock>?, onBack: () -> Unit) {
    var fullScreenVideoUrl by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Back",
                color = JetsGreenLight,
                modifier = Modifier.clickable(onClick = onBack),
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (!item.link.contains("x.com") && !item.link.contains("twitter.com")) {
                Text(text = item.title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            Spacer(modifier = Modifier.height(16.dp))
            if (item.imageUrl != null && !item.link.contains("x.com") && !item.link.contains("twitter.com")) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (content == null) {
                CircularProgressIndicator(color = JetsGreenLight, modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
            } else if (content.isEmpty() || (content.size == 1 && content[0] is com.example.ganggreen.data.model.ArticleBlock.Text && (content[0] as com.example.ganggreen.data.model.ArticleBlock.Text).content == "Unable to load article content.")) {
                Text(text = "Could not parse article content.", color = TextSecondary)
            } else {
                content.forEach { block ->
                    when (block) {
                        is com.example.ganggreen.data.model.ArticleBlock.TweetHeader -> {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                coil.compose.AsyncImage(
                                    model = block.avatarUrl,
                                    contentDescription = block.name,
                                    modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = block.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "@${block.handle}", color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                        }
                        is com.example.ganggreen.data.model.ArticleBlock.Text -> {
                            if (block.isHeader) {
                                Text(
                                    text = block.content,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Text(
                                    text = block.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        is com.example.ganggreen.data.model.ArticleBlock.Image -> {
                            val isAuthor = block.url.contains("author", true) || block.url.contains("columnists", true) || block.url.contains("profile", true) || block.url.contains("headshots", true)
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = if (isAuthor) Alignment.Center else Alignment.TopStart) {
                                Column(horizontalAlignment = if (isAuthor) Alignment.CenterHorizontally else Alignment.Start) {
                                    coil.compose.AsyncImage(
                                        model = block.url,
                                        contentDescription = block.caption,
                                        modifier = if (isAuthor) Modifier.size(80.dp).clip(androidx.compose.foundation.shape.CircleShape) else Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                                        contentScale = if (isAuthor) androidx.compose.ui.layout.ContentScale.Crop else androidx.compose.ui.layout.ContentScale.FillWidth
                                    )
                                    if (!block.caption.isNullOrEmpty()) {
                                        Text(
                                            text = block.caption,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        is com.example.ganggreen.data.model.ArticleBlock.Video -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                var isPlaying by remember { mutableStateOf(false) }
                                var videoView by remember { mutableStateOf<android.widget.VideoView?>(null) }
                                val context = androidx.compose.ui.platform.LocalContext.current

                                if (isPlaying) {
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = {
                                            android.widget.VideoView(it).apply {
                                                setVideoPath(block.url)
                                                val mediaController = android.widget.MediaController(it)
                                                mediaController.setAnchorView(this)
                                                setMediaController(mediaController)
                                                setOnPreparedListener { mp ->
                                                    mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                                                    start()
                                                }
                                                videoView = this
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().aspectRatio(block.aspectRatio ?: 1.0f)
                                    )
                                } else {
                                    coil.compose.AsyncImage(
                                        model = block.thumbnailUrl,
                                        contentDescription = "Video Thumbnail",
                                        modifier = Modifier.fillMaxWidth().aspectRatio(block.aspectRatio ?: 1.0f).clip(RoundedCornerShape(8.dp)).clickable { isPlaying = true },
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable { isPlaying = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        androidx.compose.material3.IconButton(
                                            onClick = { downloadMedia(context, block.url, true) },
                                            modifier = Modifier.background(Color.Black.copy(alpha=0.5f), CircleShape).size(36.dp)
                                        ) {
                                            androidx.compose.material3.Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Filled.Download,
                                                contentDescription = "Download",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        androidx.compose.material3.IconButton(
                                            onClick = { 
                                                videoView?.pause()
                                                isPlaying = false
                                                fullScreenVideoUrl = block.url 
                                            },
                                            modifier = Modifier.background(Color.Black.copy(alpha=0.5f), CircleShape).size(36.dp)
                                        ) {
                                            androidx.compose.material3.Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Filled.Fullscreen,
                                                contentDescription = "Fullscreen",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    if (fullScreenVideoUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { fullScreenVideoUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        android.widget.VideoView(context).apply {
                            setVideoPath(fullScreenVideoUrl)
                            val mediaController = android.widget.MediaController(context)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setOnPreparedListener { start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                androidx.compose.material3.IconButton(
                    onClick = { fullScreenVideoUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

fun downloadMedia(context: android.content.Context, url: String, isVideo: Boolean = false) {
    val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
        .setTitle(if (isVideo) "GangGreen_Video.mp4" else "GangGreen_Image.jpg")
        .setDescription("Downloading media...")
        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, if (isVideo) "GangGreen_Video.mp4" else "GangGreen_Image.jpg")
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
    val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
    downloadManager.enqueue(request)
    android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
}
