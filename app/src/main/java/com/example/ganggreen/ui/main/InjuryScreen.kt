package com.example.ganggreen.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ganggreen.theme.DarkGreenSurface
import com.example.ganggreen.theme.JetsGreenLight
import com.example.ganggreen.theme.TextPrimary
import com.example.ganggreen.theme.TextSecondary

@Composable
fun InjuryScreen(viewModel: MainViewModel) {
    val rosterResponse by viewModel.rosterState.collectAsStateWithLifecycle()
    val injuryDetails by viewModel.injuryDetails.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Jets Injury Report",
                style = MaterialTheme.typography.headlineMedium,
                color = JetsGreenLight,
                fontWeight = FontWeight.Bold
            )
        }

        if (rosterResponse == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val allAthletes = rosterResponse!!.athletes.flatMap { it.items }
            val injuredAthletes = allAthletes.filter { it.injuries.isNotEmpty() }.sortedBy { it.fullName }

            LaunchedEffect(injuredAthletes) {
                val ids = injuredAthletes.mapNotNull { it.id }
                if (ids.isNotEmpty()) {
                    viewModel.fetchInjuryDetails(ids)
                }
            }

            if (injuredAthletes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No injuries reported.", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(injuredAthletes) { athlete ->
                        val status = athlete.injuries.first().status
                        val symbol = when (status) {
                            "Questionable" -> "Q"
                            "Out" -> "O"
                            "Doubtful" -> "D"
                            "Injured Reserve" -> "IR"
                            else -> status.take(1)
                        }
                        val badgeColor = when (symbol) {
                            "O", "IR" -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
                            "D" -> androidx.compose.ui.graphics.Color(0xFFF57C00)
                            else -> androidx.compose.ui.graphics.Color(0xFFFBC02D)
                        }
                        
                        val detail = athlete.id?.let { injuryDetails[it] }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkGreenSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                athlete.headshot?.href?.let {
                                    AsyncImage(
                                        model = it,
                                        contentDescription = athlete.fullName,
                                        modifier = Modifier.size(64.dp).clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } ?: Spacer(modifier = Modifier.size(64.dp))
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = athlete.fullName ?: "Unknown",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${athlete.position?.abbreviation ?: ""} #${athlete.jersey ?: ""}",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (detail != null && detail.isNotEmpty()) {
                                        Text(
                                            text = "Injury: $detail",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = status,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
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
