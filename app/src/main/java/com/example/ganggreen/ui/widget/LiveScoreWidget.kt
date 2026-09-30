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

class LiveScoreWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = DataRepository()
        var event: Event? = null
        try {
            event = repo.getJetsEvent()
        } catch (e: Exception) {}
        
        provideContent {
            WidgetContent(event)
        }
    }

    @Composable
    fun WidgetContent(event: Event?) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF09140E))).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (event == null) {
                Text("-", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White)))
                return@Column
            }

            val comp = event.competitions.firstOrNull()
            val jets = comp?.competitors?.find { it.team.abbreviation == "NYJ" }
            val opp = comp?.competitors?.find { it.team.abbreviation != "NYJ" }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(jets?.team?.abbreviation ?: "NYJ", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF28986B)), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(jets?.displayScore ?: "0", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(opp?.team?.abbreviation ?: "OPP", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA0B3A6)), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(opp?.displayScore ?: "0", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            
            val status = event.status
            val statusText = if (status?.type?.state == "in") "Q${status.period} ${status.displayClock}" else status?.type?.shortDetail ?: ""
            Text(statusText, style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Red), fontSize = 12.sp))
        }
    }
}

class LiveScoreWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiveScoreWidget()
}
