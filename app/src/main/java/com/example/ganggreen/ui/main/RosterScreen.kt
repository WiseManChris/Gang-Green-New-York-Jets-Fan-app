package com.example.ganggreen.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ganggreen.data.model.Athlete
import com.example.ganggreen.theme.TextPrimary
import com.example.ganggreen.theme.TextSecondary
import com.example.ganggreen.theme.DarkGreenSurface
import com.example.ganggreen.theme.JetsGreenLight
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(viewModel: MainViewModel) {
    val rosterResponse by viewModel.rosterState.collectAsStateWithLifecycle()
    val selectedAthlete by viewModel.selectedAthlete.collectAsStateWithLifecycle()
    val athleteStats by viewModel.athleteStats.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (rosterResponse == null) {
            item {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        } else {
            val allAthletes = rosterResponse!!.athletes.flatMap { it.items }
            val groupedAthletes = allAthletes.groupBy { it.position?.abbreviation ?: "Other" }
            
            // Standard order for football positions
            val positionOrder = listOf("QB", "RB", "FB", "WR", "TE", "LT", "LG", "C", "RG", "RT", "OT", "G", "DE", "DT", "NT", "DL", "OLB", "MLB", "ILB", "LB", "CB", "S", "FS", "SS", "DB", "K", "P", "LS")
            val sortedGroups = groupedAthletes.keys.sortedBy { pos -> 
                val idx = positionOrder.indexOf(pos)
                if (idx == -1) 99 else idx
            }

            sortedGroups.forEach { posName ->
                item {
                    Text(
                        text = posName,
                        style = MaterialTheme.typography.titleLarge,
                        color = JetsGreenLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    
                    val athletesInGroup = (groupedAthletes[posName] ?: emptyList()).sortedWith(compareBy({ it.depthChartRank }, { it.fullName }))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        items(athletesInGroup) { athlete ->
                            AthleteCard(athlete, onClick = { viewModel.selectAthlete(athlete) })
                        }
                    }
                }
            }
        }
    }

    if (selectedAthlete != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectAthlete(null) },
            containerColor = DarkGreenSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row {
                    selectedAthlete!!.headshot?.href?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = selectedAthlete!!.fullName,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } ?: Spacer(modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "${selectedAthlete!!.fullName ?: "Unknown"} #${selectedAthlete!!.jersey ?: "--"}",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedAthlete!!.position?.displayName ?: ""} | Exp: ${selectedAthlete!!.experience?.years ?: 0} yrs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        selectedAthlete!!.college?.name?.let {
                            Text(text = "College: $it", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = TextSecondary.copy(alpha=0.2f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Live Season Stats",
                    style = MaterialTheme.typography.titleMedium,
                    color = JetsGreenLight,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (athleteStats.isEmpty()) {
                    Text("No significant season stats available.", color = TextSecondary)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        athleteStats.forEach { (catName, statsList) ->
                            item {
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(statsList) { stat ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stat.first, color = TextSecondary)
                                    Text(stat.second, color = TextPrimary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AthleteCard(athlete: Athlete, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkGreenSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Box {
                athlete.headshot?.href?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = athlete.fullName,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } ?: Spacer(modifier = Modifier.size(80.dp))
                
                if (athlete.injuries.isNotEmpty()) {
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
                    Box(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(badgeColor)
                            .size(22.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${athlete.fullName ?: "Unknown"}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            val rankString = if (athlete.depthChartRank < 99) "String ${athlete.depthChartRank}" else "Res"
            Text(
                text = "#${athlete.jersey ?: "--"} | $rankString",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
