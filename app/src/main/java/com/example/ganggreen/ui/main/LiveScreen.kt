package com.example.ganggreen.ui.main
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset


import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ganggreen.data.model.Event
import com.example.ganggreen.data.model.PlayByPlayResponse
import com.example.ganggreen.data.model.PlaySituation
import com.example.ganggreen.theme.*
import kotlinx.coroutines.delay
import com.example.ganggreen.data.model.Weather
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas

fun parseEspnDate(dateStr: String): Long {
    val formats = listOf("yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
    for (f in formats) {
        try {
            val sdf = SimpleDateFormat(f, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val parsed = sdf.parse(dateStr)
            if (parsed != null) return parsed.time
        } catch (e: Exception) {}
    }
    return 0L
}

fun formatEspnDate(dateStr: String, pattern: String): String {
    val time = parseEspnDate(dateStr)
    if (time == 0L) return dateStr
    val sdf = SimpleDateFormat(pattern, Locale.US)
    sdf.timeZone = TimeZone.getDefault()
    return sdf.format(Date(time))
}

@Composable
fun LiveScreen(viewModel: MainViewModel) {
    val liveEvent by viewModel.liveEvent.collectAsStateWithLifecycle()
    val pbp by viewModel.playByPlayState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.startPollingLiveGame()
        onDispose {
            viewModel.stopPollingLiveGame()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (liveEvent == null) {
            item {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No Jets game currently available.", color = TextSecondary)
                }
            }
        } else {
            val ev = liveEvent!!
            
            item {
                GameHeader(ev, pbp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (ev.status?.type?.state == "pre") {
                item {
                    NextGameInfoBlocks(ev, pbp)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (pbp != null) {
                // Section: Gamecast & Field
                item {
                    val currentDrive = pbp!!.drives?.current ?: pbp!!.drives?.previous?.firstOrNull()
                    val homeComp = ev.competitions.firstOrNull()?.competitors?.find { it.homeAway == "home" }
                    val awayComp = ev.competitions.firstOrNull()?.competitors?.find { it.homeAway == "away" }
                    val nyjComp = ev.competitions.firstOrNull()?.competitors?.find { it.team.abbreviation == "NYJ" }

                    SectionHeader("Live Field")
                    FootballField(
                        play = currentDrive?.plays?.lastOrNull()?.end,
                        homeAbbr = homeComp?.team?.abbreviation ?: "HOME",
                        awayAbbr = awayComp?.team?.abbreviation ?: "AWAY",
                        homeColor = homeComp?.team?.color ?: "0B3A22",
                        awayColor = awayComp?.team?.color ?: "D6A000",
                        centerLogo = nyjComp?.team?.displayLogo
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentDrive != null) {
                        Text(
                            text = "Drive: ${currentDrive.description} ${if (currentDrive.displayResult.isNotEmpty()) "(${currentDrive.displayResult})" else ""}",
                            color = JetsGreenLight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Section: Plays
                val currentDrive = pbp!!.drives?.current ?: pbp!!.drives?.previous?.firstOrNull()
                val plays = currentDrive?.plays?.reversed() ?: emptyList()
                if (plays.isNotEmpty()) {
                    items(plays) { play ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkGreenSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(play.type?.text ?: "Play", color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text(play.clock?.displayValue ?: "", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(play.text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Section: Team Leaders
                if (ev.status?.type?.state != "pre") {
                    item {
                        SectionHeader("Team Leaders")
                        TeamLeaders(ev)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                // Section: Team Stats
                if (ev.status?.type?.state != "pre" && pbp?.boxscore != null) {
                    item {
                        SectionHeader("Box Score")
                        TeamStats(pbp!!.boxscore!!, ev)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Section: Scoring Summary
                if (!pbp?.scoringPlays.isNullOrEmpty()) {
                    item {
                        SectionHeader("Scoring Plays")
                        ScoringSummary(pbp!!.scoringPlays)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Section: Win Probability & Odds
                item {
                    SectionHeader("Analysis")
                    PredictorAndOdds(pbp!!)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (ev.status?.type?.state != "pre" && !pbp?.winProbabilityHistory.isNullOrEmpty()) {
                        WinProbabilityGraph(pbp!!.winProbabilityHistory, ev)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else if (ev.status?.type?.state == "in") {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(modifier = Modifier.width(4.dp).height(16.dp).background(JetsGreenLight, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun GameHeader(event: Event, pbp: PlayByPlayResponse? = null) {
    val comp = event.competitions.firstOrNull() ?: return
    val homeTeam = comp.competitors.find { it.homeAway == "home" }
    val awayTeam = comp.competitors.find { it.homeAway == "away" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    AsyncImage(model = awayTeam?.team?.displayLogo ?: "", contentDescription = null, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(awayTeam?.team?.abbreviation ?: "", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(awayTeam?.displayScore ?: "0", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    if (event.status?.type?.state == "pre") {
                        val dateStr = formatEspnDate(comp.date, "M/d - h:mm a z")
                        Text(dateStr, color = StatusRedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    } else {
                        Text(event.status?.type?.shortDetail ?: "", color = StatusRedText, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("AT", color = TextSecondary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    AsyncImage(model = homeTeam?.team?.displayLogo ?: "", contentDescription = null, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(homeTeam?.team?.abbreviation ?: "", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(homeTeam?.displayScore ?: "0", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
            }
            
            if (event.status?.type?.state == "in" && pbp != null) {
                val currentDrive = pbp.drives?.current
                val lastPlay = currentDrive?.plays?.lastOrNull()
                val situation = lastPlay?.end ?: lastPlay?.start
                
                if (situation != null && situation.shortDownDistanceText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = TextSecondary.copy(alpha=0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF0C2B1B).copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.4f)) {
                            Text(situation.shortDownDistanceText, color = JetsGreenLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            if (situation.possessionText.isNotEmpty()) {
                                Text(situation.possessionText, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (lastPlay != null) {
                            Text(
                                text = lastPlay.text, 
                                color = TextSecondary, 
                                style = MaterialTheme.typography.bodySmall, 
                                modifier = Modifier.weight(0.6f).padding(start = 12.dp), 
                                maxLines = 4, 
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PredictorAndOdds(pbp: PlayByPlayResponse) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val pred = pbp.predictor
        if (pred != null && pred.homeTeam?.gameProjection != null && pred.awayTeam?.gameProjection != null) {
            val homeProj = pred.homeTeam.gameProjection.toFloatOrNull() ?: 50f
            val awayProj = pred.awayTeam.gameProjection.toFloatOrNull() ?: 50f
            Text("Win Probability", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp))) {
                Box(modifier = Modifier.weight(awayProj).fillMaxHeight().background(Color.White), contentAlignment = Alignment.CenterStart) {
                    Text("Away ${pred.awayTeam.gameProjection}%", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start=8.dp))
                }
                Box(modifier = Modifier.weight(homeProj).fillMaxHeight().background(JetsGreenLight), contentAlignment = Alignment.CenterEnd) {
                    Text("Home ${pred.homeTeam.gameProjection}%", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end=8.dp))
                }
            }
        }
    }
}

@Composable
fun FootballField(
    play: PlaySituation?,
    homeAbbr: String = "NYJ",
    awayAbbr: String = "AWAY",
    homeColor: String = "0B3A22",
    awayColor: String = "D6A000",
    centerLogo: String? = null
) {
    val parseTeamColor = { hex: String, fallback: Long ->
        try { Color(android.graphics.Color.parseColor("#$hex")) } catch(e: Exception) { Color(fallback) }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val W = size.width
            val H = size.height
            
            // Perspective definitions
            val topWidth = W * 0.70f
            val bottomWidth = W * 0.95f
            val topY = H * 0.45f
            val bottomY = H * 0.85f
            val fieldThickness = 12f
            
            fun project(xYards: Float, yRel: Float): Offset {
                val xRel = xYards / 120f
                val currentWidth = topWidth + yRel * (bottomWidth - topWidth)
                val xScreen = (W / 2f) - (currentWidth / 2f) + (xRel * currentWidth)
                val yScreen = topY + yRel * (bottomY - topY)
                return Offset(xScreen, yScreen)
            }

            // 1. Draw Field Base (Thickness)
            val basePath = androidx.compose.ui.graphics.Path().apply {
                val tl = project(0f, 0f)
                val tr = project(120f, 0f)
                val br = project(120f, 1f)
                val bl = project(0f, 1f)
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(tr.x, br.y + fieldThickness)
                lineTo(bl.x, bl.y + fieldThickness)
                lineTo(tl.x, tl.y)
                close()
            }
            drawPath(basePath, color = Color(0xFF143B20))

            // 2. Draw Field Surface
            val surfacePath = androidx.compose.ui.graphics.Path().apply {
                val tl = project(0f, 0f)
                val tr = project(120f, 0f)
                val br = project(120f, 1f)
                val bl = project(0f, 1f)
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }
            drawPath(surfacePath, color = Color(0xFF337A40))

            // 3. Turf banding
            for (i in 0 until 24) {
                if (i % 2 == 1) {
                    val bandPath = androidx.compose.ui.graphics.Path().apply {
                        val startX = i * 5f
                        val endX = (i + 1) * 5f
                        val tl = project(startX, 0f)
                        val tr = project(endX, 0f)
                        val br = project(endX, 1f)
                        val bl = project(startX, 1f)
                        moveTo(tl.x, tl.y)
                        lineTo(tr.x, tr.y)
                        lineTo(br.x, br.y)
                        lineTo(bl.x, bl.y)
                        close()
                    }
                    drawPath(bandPath, color = Color(0xFF3B8C4A))
                }
            }

            // 4. Endzones
            val awayEndzonePath = androidx.compose.ui.graphics.Path().apply {
                val tl = project(0f, 0f); val tr = project(10f, 0f)
                val br = project(10f, 1f); val bl = project(0f, 1f)
                moveTo(tl.x, tl.y); lineTo(tr.x, tr.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close()
            }
            drawPath(awayEndzonePath, color = parseTeamColor(awayColor, 0xFFD6A000))

            val homeEndzonePath = androidx.compose.ui.graphics.Path().apply {
                val tl = project(110f, 0f); val tr = project(120f, 0f)
                val br = project(120f, 1f); val bl = project(110f, 1f)
                moveTo(tl.x, tl.y); lineTo(tr.x, tr.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close()
            }
            drawPath(homeEndzonePath, color = parseTeamColor(homeColor, 0xFF0B3A22))

            // 5. White lines
            for (i in 1..10) {
                val yard = 10f + (i * 10f)
                if (yard <= 110f) {
                    val top = project(yard, 0f)
                    val bot = project(yard, 1f)
                    drawLine(color = Color.White.copy(alpha=0.7f), start = top, end = bot, strokeWidth = 1.5f)
                }
            }
            val ez1Top = project(10f, 0f); val ez1Bot = project(10f, 1f)
            drawLine(color = Color.White, start = ez1Top, end = ez1Bot, strokeWidth = 3f)
            val ez2Top = project(110f, 0f); val ez2Bot = project(110f, 1f)
            drawLine(color = Color.White, start = ez2Top, end = ez2Bot, strokeWidth = 3f)

            // 6. Numbers and Team Text
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                alpha = 180
            }
            val textPaintTeam = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 36f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD_ITALIC)
                alpha = 220
            }

            for (i in 1..9) {
                val yard = 10f + (i * 10f)
                val num = if (i <= 5) i * 10 else (10 - i) * 10
                
                val pTop = project(yard, 0.25f)
                drawContext.canvas.nativeCanvas.drawText(num.toString(), pTop.x, pTop.y + 10f, textPaint)
                
                val pBot = project(yard, 0.75f)
                drawContext.canvas.nativeCanvas.drawText(num.toString(), pBot.x, pBot.y + 10f, textPaint)
            }
            


            val awayCenter = project(5f, 0.5f)
            drawContext.canvas.nativeCanvas.drawText(awayAbbr, awayCenter.x, awayCenter.y + 12f, textPaintTeam)
            val homeCenter = project(115f, 0.5f)
            drawContext.canvas.nativeCanvas.drawText(homeAbbr, homeCenter.x, homeCenter.y + 12f, textPaintTeam)

            // 7. Hash marks
            for (i in 11..109) {
                if (i % 10 != 0) {
                    val ht1 = project(i.toFloat(), 0.43f)
                    val hb1 = project(i.toFloat(), 0.45f)
                    drawLine(color = Color.White.copy(alpha=0.5f), start = ht1, end = hb1, strokeWidth = 1f)

                    val ht2 = project(i.toFloat(), 0.55f)
                    val hb2 = project(i.toFloat(), 0.57f)
                    drawLine(color = Color.White.copy(alpha=0.5f), start = ht2, end = hb2, strokeWidth = 1f)
                }
            }

            // 8. Goalposts
            fun drawGoalpost(xYard: Float) {
                val base = project(xYard, 0.5f)
                val color = Color(0xFFFFD600)
                drawLine(color = color, start = base, end = Offset(base.x, base.y - 45f), strokeWidth = 4f)
                drawLine(color = color, start = Offset(base.x - 20f, base.y - 45f), end = Offset(base.x + 20f, base.y - 45f), strokeWidth = 4f)
                drawLine(color = color, start = Offset(base.x - 20f, base.y - 45f), end = Offset(base.x - 20f, base.y - 110f), strokeWidth = 4f)
                drawLine(color = color, start = Offset(base.x + 20f, base.y - 45f), end = Offset(base.x + 20f, base.y - 110f), strokeWidth = 4f)
            }
            drawGoalpost(0f)
            drawGoalpost(120f)

            // 9. Live Play
            if (play != null && play.yardsToEndzone > 0) {
                val losYardPos = 110f - play.yardsToEndzone
                val firstDownYardPos = losYardPos + play.distance
                
                val losTop = project(losYardPos, 0f)
                val losBot = project(losYardPos, 1f)
                val fdTop = project(firstDownYardPos, 0f)
                val fdBot = project(firstDownYardPos, 1f)

                if (play.distance > 0) {
                    val highlightPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(losTop.x, losTop.y)
                        lineTo(fdTop.x, fdTop.y)
                        lineTo(fdBot.x, fdBot.y)
                        lineTo(losBot.x, losBot.y)
                        close()
                    }
                    drawPath(highlightPath, color = Color(0xFF0D47A1).copy(alpha = 0.4f))
                }

                drawLine(color = Color(0xFFFFD600), start = fdTop, end = fdBot, strokeWidth = 4f)
                drawLine(color = Color(0xFF1976D2), start = losTop, end = losBot, strokeWidth = 4f)

                val fbPos = project(losYardPos - 1f, 0.5f)
                drawOval(color = Color(0xFF3E2723), topLeft = Offset(fbPos.x - 8f, fbPos.y - 10f), size = Size(16f, 20f))
                drawLine(color = Color.White, start = Offset(fbPos.x - 2f, fbPos.y - 6f), end = Offset(fbPos.x - 2f, fbPos.y + 6f), strokeWidth = 1.5f)
            }
        }
        
        if (centerLogo != null) {
            val imageSize = 60.dp
            AsyncImage(
                model = centerLogo,
                contentDescription = "Center Logo",
                modifier = Modifier
                    .size(imageSize)
                    .align(Alignment.TopCenter)
                    .offset(y = maxHeight * 0.65f - (imageSize / 2))
            )
        }
        
        if (play != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = play.downDistanceText.ifEmpty { "Play In Progress" },
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun NextGameInfoBlocks(event: Event, pbp: PlayByPlayResponse?) {
    val comp = event.competitions.firstOrNull() ?: return
    
    var timeRemainingMs by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(comp.date) {
        val targetTime = parseEspnDate(comp.date)
        while (targetTime > 0) {
            val now = System.currentTimeMillis()
            timeRemainingMs = if (targetTime > now) targetTime - now else 0L
            delay(1000L)
        }
    }

    val days = timeRemainingMs / (1000 * 60 * 60 * 24)
    val hrs = (timeRemainingMs / (1000 * 60 * 60)) % 24
    val mins = (timeRemainingMs / (1000 * 60)) % 60
    val secs = (timeRemainingMs / 1000) % 60

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("TILL KICKOFF", color = TextSecondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Timer Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerBox(days.toString().padStart(2, '0'), "DAYS", Modifier.weight(1f))
            TimerBox(hrs.toString().padStart(2, '0'), "HRS", Modifier.weight(1f))
            TimerBox(mins.toString().padStart(2, '0'), "MIN", Modifier.weight(1f))
            TimerBox(secs.toString().padStart(2, '0'), "SEC", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoBox("KICKOFF", formatEspnDate(comp.date, "EEE, MMM d, h:mm a z"), Modifier.weight(1f))
            InfoBox("VENUE", comp.venue?.let { "${it.fullName} · ${it.address?.city}, ${it.address?.state}" } ?: "TBD", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoBox("TV", comp.broadcasts.firstOrNull()?.names?.joinToString(", ") ?: "TBD", Modifier.weight(1f))
            val w = event.weather
            val weatherStr = if (w != null) "${w.temperature}° · ${w.displayValue}" else "TBD"
            InfoBox("WEATHER", weatherStr, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        val dkOdds = pbp?.pickcenter?.find { it.provider?.name == "DraftKings" } ?: pbp?.pickcenter?.firstOrNull()
        val oddsStr = if (dkOdds != null) "${dkOdds.details} · O/U ${dkOdds.overUnder}" else "TBD"
        InfoBox("LINE", oddsStr, Modifier.fillMaxWidth())
    }
}

@Composable
fun TimerBox(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.heightIn(min=70.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TeamLeaders(event: Event) {
    val homeComp = event.competitions.firstOrNull()?.competitors?.find { it.homeAway == "home" }
    val awayComp = event.competitions.firstOrNull()?.competitors?.find { it.homeAway == "away" }
    
    if (homeComp?.leaders.isNullOrEmpty() && awayComp?.leaders.isNullOrEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Top Performers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                if (awayComp != null && !awayComp.leaders.isNullOrEmpty()) {
                    Text(awayComp.team.abbreviation, color = JetsGreenLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    awayComp.leaders.forEach { category ->
                        val leader = category.leaders.firstOrNull()
                        if (leader != null) {
                            LeaderRow(category.shortDisplayName, leader)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (homeComp != null && !homeComp.leaders.isNullOrEmpty()) {
                    Text(homeComp.team.abbreviation, color = JetsGreenLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    homeComp.leaders.forEach { category ->
                        val leader = category.leaders.firstOrNull()
                        if (leader != null) {
                            LeaderRow(category.shortDisplayName, leader)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderRow(category: String, leader: com.example.ganggreen.data.model.LeaderItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val headshot = leader.athlete.headshot
        if (headshot != null) {
            AsyncImage(
                model = headshot,
                contentDescription = leader.athlete.shortName,
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray), contentAlignment = Alignment.Center) {
                Text(leader.athlete.shortName.take(1), color = Color.White, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text("${category}: ${leader.athlete.shortName}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(leader.displayValue, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TeamStats(boxscore: com.example.ganggreen.data.model.Boxscore, event: Event) {
    if (boxscore.teams.size < 2) return
    val homeComp = event.competitions.firstOrNull()?.competitors?.find { it.homeAway == "home" }
    val awayComp = event.competitions.firstOrNull()?.competitors?.find { it.homeAway == "away" }
    
    val homeStats = boxscore.teams.find { it.team.id == homeComp?.team?.id }?.statistics
    val awayStats = boxscore.teams.find { it.team.id == awayComp?.team?.id }?.statistics
    
    if (homeStats.isNullOrEmpty() || awayStats.isNullOrEmpty()) return

    val metricsToTrack = listOf("totalYards", "netPassingYards", "rushingYards", "turnovers", "possessionTime", "thirdDownEff", "totalPenaltiesYards")
    
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(DarkGreenSurface, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(awayComp?.team?.abbreviation ?: "AWAY", color = JetsGreenLight, fontWeight = FontWeight.Bold)
            Text("Team Stats", color = Color.White, fontWeight = FontWeight.Bold)
            Text(homeComp?.team?.abbreviation ?: "HOME", color = JetsGreenLight, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        metricsToTrack.forEach { metricName ->
            val awayStat = awayStats.find { it.name == metricName }
            val homeStat = homeStats.find { it.name == metricName }
            if (awayStat != null && homeStat != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(awayStat.displayValue, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(awayStat.label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(2f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(homeStat.displayValue, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                Divider(color = JetsGreenLight.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
fun ScoringSummary(plays: List<com.example.ganggreen.data.model.Play>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Scoring Summary", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        plays.forEach { play ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                val teamLogo = play.team?.displayLogo
                if (teamLogo != null) {
                    AsyncImage(
                        model = teamLogo,
                        contentDescription = "Team Logo",
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Box(modifier = Modifier.size(24.dp).background(Color.Gray, CircleShape))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(play.text, color = TextPrimary, fontSize = 14.sp)
                    Text("Q${play.period?.number} - ${play.clock?.displayValue}", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("${play.awayScore} - ${play.homeScore}", color = JetsGreenLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            HorizontalDivider(color = DarkGreenSurfaceVariant, thickness = 1.dp)
        }
    }
}

@Composable
fun WinProbabilityGraph(history: List<com.example.ganggreen.data.model.WinProbability>, event: Event) {
    if (history.isEmpty()) return
    
    val homeComp = event.competitions.firstOrNull()?.competitors?.find { it.homeAway == "home" }
    val awayComp = event.competitions.firstOrNull()?.competitors?.find { it.homeAway == "away" }
    
    val homeColor = Color(android.graphics.Color.parseColor("#${homeComp?.team?.color ?: "000000"}"))
    val awayColor = Color(android.graphics.Color.parseColor("#${awayComp?.team?.color ?: "FFFFFF"}"))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(DarkGreenSurface, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Text("Win Probability", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(awayComp?.team?.abbreviation ?: "AWAY", color = awayColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(homeComp?.team?.abbreviation ?: "HOME", color = homeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            val width = size.width
            val height = size.height
            
            // Draw 50% line
            drawLine(
                color = Color.Gray,
                start = Offset(0f, height / 2f),
                end = Offset(width, height / 2f),
                strokeWidth = 1f
            )
            
            val path = Path()
            val stepX = width / (history.size - 1).coerceAtLeast(1).toFloat()
            
            history.forEachIndexed { index, prob ->
                val x = index * stepX
                // prob.homeWinPercentage is 0.0 to 1.0. 
                // Y = 0 is home 100%, Y = height is away 100% (since height is flipped)
                // If homeWinPercentage = 1.0, Y = 0.
                val y = height - (prob.homeWinPercentage.toFloat() * height)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun DriveHistory(drives: List<com.example.ganggreen.data.model.Drive>) {
    var expandedDriveId by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("DRIVE HISTORY & PLAY-BY-PLAY", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        drives.reversed().forEach { drive ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { 
                    expandedDriveId = if (expandedDriveId == drive.id) null else drive.id 
                },
                colors = CardDefaults.cardColors(containerColor = DarkGreenSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        coil.compose.AsyncImage(
                            model = drive.team?.displayLogo,
                            contentDescription = "Team Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(drive.displayResult, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(drive.description, color = TextSecondary, fontSize = 12.sp)
                        }
                        Icon(
                            if (expandedDriveId == drive.id) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Expand",
                            tint = TextSecondary
                        )
                    }
                    
                    if (expandedDriveId == drive.id) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkGreenSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        drive.plays.reversed().forEach { play ->
                            Row(modifier = Modifier.padding(bottom = 12.dp)) {
                                Text(play.clock?.displayValue ?: "", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(play.text, color = Color.White, fontSize = 14.sp)
                                    if (play.start?.shortDownDistanceText?.isNotEmpty() == true) {
                                        Text(play.start.shortDownDistanceText, color = JetsGreenLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
