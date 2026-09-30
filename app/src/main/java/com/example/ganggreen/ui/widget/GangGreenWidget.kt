package com.example.ganggreen.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import com.example.ganggreen.data.DataRepository
import com.example.ganggreen.data.model.Event
import java.text.SimpleDateFormat
import java.util.*

class GangGreenWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = DataRepository()
        var event: Event? = null
        try {
            event = repo.getJetsEvent()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        provideContent {
            WidgetContent(event)
        }
    }

    @Composable
    fun WidgetContent(event: Event?) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF09140E))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (event == null) {
                Text("LOADING...", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF28986B)), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                return@Column
            }

            val comp = event.competitions.firstOrNull()
            val jets = comp?.competitors?.find { it.team.abbreviation == "NYJ" }
            val opp = comp?.competitors?.find { it.team.abbreviation != "NYJ" }
            
            val isLive = event.status?.type?.state == "in"
            val statusText = if (isLive) "LIVE NOW: Q${event.status.period} ${event.status.displayClock}" else "NEXT JETS GAME"

            Text(statusText, style = TextStyle(color = ColorProvider(if (isLive) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color(0xFF28986B)), fontSize = 12.sp, fontWeight = FontWeight.Bold))
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(jets?.team?.abbreviation ?: "NYJ", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)), fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    if (isLive) {
                        Text(jets?.displayScore ?: "0", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)), fontSize = 20.sp))
                    }
                }
                Spacer(modifier = GlanceModifier.width(16.dp))
                Text(if (isLive) "-" else "VS", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA0B3A6)), fontSize = 16.sp))
                Spacer(modifier = GlanceModifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(opp?.team?.abbreviation ?: "OPP", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)), fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    if (isLive) {
                        Text(opp?.displayScore ?: "0", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)), fontSize = 20.sp))
                    }
                }
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            if (!isLive) {
                val dateStr = comp?.date ?: ""
                var formattedDate = dateStr
                try {
                    val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                    val sdfOut = SimpleDateFormat("EEEE h:mm a", Locale.US)
                    sdfIn.parse(dateStr)?.let { formattedDate = sdfOut.format(it).uppercase() }
                } catch (e: Exception) {}
                
                Text(formattedDate, style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA0B3A6)), fontSize = 14.sp))
            }
        }
    }
}

class GangGreenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GangGreenWidget()
}
