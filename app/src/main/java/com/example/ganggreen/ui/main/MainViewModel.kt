package com.example.ganggreen.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import com.example.ganggreen.data.DataRepository
import com.example.ganggreen.data.model.Event
import com.example.ganggreen.data.model.RosterResponse
import com.example.ganggreen.data.network.NewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: DataRepository = DataRepository()) : ViewModel() {

    



    private val _selectedAthlete = kotlinx.coroutines.flow.MutableStateFlow<com.example.ganggreen.data.model.Athlete?>(null)
    val selectedAthlete: kotlinx.coroutines.flow.StateFlow<com.example.ganggreen.data.model.Athlete?> = _selectedAthlete.asStateFlow()

    private val _athleteStats = kotlinx.coroutines.flow.MutableStateFlow<Map<String, List<Pair<String, String>>>>(emptyMap())
    val athleteStats: kotlinx.coroutines.flow.StateFlow<Map<String, List<Pair<String, String>>>> = _athleteStats.asStateFlow()

    fun selectAthlete(athlete: com.example.ganggreen.data.model.Athlete?) {
        _selectedAthlete.value = athlete
        if (athlete != null && athlete.id != null) {
            viewModelScope.launch {
                val stats = repository.getAthleteStats(athlete.id)
                _athleteStats.value = stats
            }
        } else {
            _athleteStats.value = emptyMap()
        }
    }
    private val _newsState = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsState: StateFlow<List<NewsItem>> = _newsState.asStateFlow()
    
    private val _selectedNewsTab = MutableStateFlow("Jets")
    val selectedNewsTab: StateFlow<String> = _selectedNewsTab.asStateFlow()

    fun selectNewsTab(tab: String) {
        _selectedNewsTab.value = tab
        fetchNewsForTab(tab)
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        _isRefreshing.value = true
        fetchNewsForTab(_selectedNewsTab.value)
    }

    private fun fetchNewsForTab(tab: String) {
        viewModelScope.launch {
            try {
                if (!_isRefreshing.value) {
                    _newsState.value = emptyList()
                }
                
                if (tab == "Jets") {
                    val jetsNewsDeferred = async(Dispatchers.IO) { repository.getNews("https://www.newyorkjets.com/rss/news") }
                    val yahooDeferred = async(Dispatchers.IO) { repository.getNews("https://sports.yahoo.com/nfl/teams/new-york-jets/rss.xml") }
                    val twitterNewsDeferred = async(Dispatchers.IO) { repository.getTwitterNews() }
                    
                    val jetsNews = jetsNewsDeferred.await()
                    val yahooNews = yahooDeferred.await()
                    val twitterNews = twitterNewsDeferred.await()
                    
                    val combined = mutableListOf<NewsItem>()
                    val maxSize = maxOf(jetsNews.size, yahooNews.size, twitterNews.size)
                    for (i in 0 until maxSize) {
                        if (i < twitterNews.size) combined.add(twitterNews[i])
                        if (i < jetsNews.size) combined.add(jetsNews[i])
                        if (i < yahooNews.size) combined.add(yahooNews[i])
                    }
                    _newsState.value = combined
                } else {
                    val url = "https://www.espn.com/espn/rss/nfl/news"
                    _newsState.value = repository.getNews(url)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val _liveEvent = MutableStateFlow<Event?>(null)
    val liveEvent: StateFlow<Event?> = _liveEvent.asStateFlow()

    private val _rosterState = MutableStateFlow<RosterResponse?>(null)
    val rosterState: StateFlow<RosterResponse?> = _rosterState.asStateFlow()

    private val _playByPlayState = MutableStateFlow<com.example.ganggreen.data.model.PlayByPlayResponse?>(null)
    val playByPlayState: StateFlow<com.example.ganggreen.data.model.PlayByPlayResponse?> = _playByPlayState.asStateFlow()

    val jetsRecord: StateFlow<String> = _liveEvent
        .map { event ->
            val comp = event?.competitions?.firstOrNull()
            val jets = comp?.competitors?.find { it.team.abbreviation == "NYJ" }
            jets?.records?.firstOrNull()?.summary ?: "0-0"
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "0-0")

    init {
        
        fetchData()
        fetchNewsForTab("Jets")
    }

    private fun fetchData() {
        viewModelScope.launch {
            try {
                val event = repository.getJetsEvent()
                _liveEvent.value = event
                if (event != null) {
                    try {
                        _playByPlayState.value = repository.getPlayByPlay(event.id)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            try {
                _rosterState.value = repository.getRoster()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private var livePollingJob: kotlinx.coroutines.Job? = null

    fun startPollingLiveGame() {
        if (livePollingJob?.isActive == true) return
        livePollingJob = viewModelScope.launch {
            while(true) {
                try {
                    val event = repository.getJetsEvent()
                    _liveEvent.value = event
                    if (event != null && event.status?.type?.state != "pre") {
                        try {
                            _playByPlayState.value = repository.getPlayByPlay(event.id)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    fun stopPollingLiveGame() {
        livePollingJob?.cancel()
        livePollingJob = null
    }

    private val _articleContent = MutableStateFlow<List<com.example.ganggreen.data.model.ArticleBlock>?>(null)
    val articleContent: StateFlow<List<com.example.ganggreen.data.model.ArticleBlock>?> = _articleContent.asStateFlow()

    fun fetchArticle(url: String) {
        _articleContent.value = null
        viewModelScope.launch {
            _articleContent.value = repository.fetchArticleBody(url)
        }
    }

    fun clearArticle() {
        _articleContent.value = null
    }


    private val _injuryDetails = MutableStateFlow<Map<String, String>>(emptyMap())
    val injuryDetails: StateFlow<Map<String, String>> = _injuryDetails.asStateFlow()

    fun fetchInjuryDetails(athleteIds: List<String>) {
        viewModelScope.launch {
            val map = _injuryDetails.value.toMutableMap()
            athleteIds.filter { !map.containsKey(it) }.forEach { id ->
                val type = repository.getInjuryDetail(id)
                if (type != null) {
                    map[id] = type
                }
            }
            _injuryDetails.value = map
        }
    }
}
