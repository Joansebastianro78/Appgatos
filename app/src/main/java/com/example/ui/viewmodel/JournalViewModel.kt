package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.database.JournalEntry
import com.example.data.repository.JournalRepository
import com.example.data.network.Content
import com.example.data.network.GenerateContentRequest
import com.example.data.network.GenerationConfig
import com.example.data.network.Part
import com.example.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    val allEntries: StateFlow<List<JournalEntry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeTitle = MutableStateFlow("")
    val activeTitle: StateFlow<String> = _activeTitle.asStateFlow()

    private val _activeContent = MutableStateFlow("")
    val activeContent: StateFlow<String> = _activeContent.asStateFlow()

    private val _selectedCompanion = MutableStateFlow(CompanionType.CREATIVE_MUSE)
    val selectedCompanion: StateFlow<CompanionType> = _selectedCompanion.asStateFlow()

    private val _selectedMood = MutableStateFlow(MoodVibe.CALM)
    val selectedMood: StateFlow<MoodVibe> = _selectedMood.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastAiResponse = MutableStateFlow("")
    val lastAiResponse: StateFlow<String> = _lastAiResponse.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSaveSuccess = MutableStateFlow(false)
    val isSaveSuccess: StateFlow<Boolean> = _isSaveSuccess.asStateFlow()

    fun updateTitle(title: String) {
        _activeTitle.value = title
    }

    fun updateContent(content: String) {
        _activeContent.value = content
    }

    fun selectCompanion(companion: CompanionType) {
        _selectedCompanion.value = companion
    }

    fun selectMood(mood: MoodVibe) {
        _selectedMood.value = mood
    }

    fun clearInputs() {
        _activeTitle.value = ""
        _activeContent.value = ""
        _lastAiResponse.value = ""
        _errorMessage.value = null
        _isSaveSuccess.value = false
    }

    fun dismissSaveSuccess() {
        _isSaveSuccess.value = false
    }

    fun getApiKeyStatus(): ApiKeyStatus {
        val apiKey = BuildConfig.GEMINI_API_KEY
        return when {
            apiKey.isBlank() -> ApiKeyStatus.MISSING
            apiKey == "MY_GEMINI_API_KEY" -> ApiKeyStatus.PLACEHOLDER
            apiKey.length < 10 -> ApiKeyStatus.INVALID
            else -> ApiKeyStatus.VALID
        }
    }

    fun generateInsight() {
        if (_activeContent.value.isBlank()) {
            _errorMessage.value = "Please write a thought or reflection first!"
            return
        }

        val status = getApiKeyStatus()
        if (status != ApiKeyStatus.VALID) {
            _errorMessage.value = when (status) {
                ApiKeyStatus.MISSING -> "API Key is missing. Please set GEMINI_API_KEY in the Secrets panel."
                ApiKeyStatus.PLACEHOLDER -> "Using demo placeholder key. Please customize your GEMINI_API_KEY in AI Studio secrets."
                else -> "The set API Key appears invalid. Double check your input."
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _lastAiResponse.value = ""

            val companionPrompt = _selectedCompanion.value.systemInstruction
            val userMood = _selectedMood.value.title
            val userText = _activeContent.value

            val prompt = """
                The user feels: $userMood
                Their journal or brainwriting thoughts: 
                "$userText"
                
                Please generate helpful, distinct creative feedback, journal suggestions, and observations based on your personality instruction.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.7f),
                systemInstruction = Content(parts = listOf(Part(text = companionPrompt)))
            )

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (!responseText.isNullOrBlank()) {
                    _lastAiResponse.value = responseText
                } else {
                    _errorMessage.value = "Received an empty response from Gemini. Please try again."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Network Error: ${e.localizedMessage ?: "Please verify your internet connection and API key"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveSession() {
        if (_activeContent.value.isBlank()) {
            _errorMessage.value = "Content cannot be empty when compiling your Aura entry!"
            return
        }

        viewModelScope.launch {
            val title = if (_activeTitle.value.isNotBlank()) _activeTitle.value else "Aura Reflection"
            val entry = JournalEntry(
                title = title,
                content = _activeContent.value,
                companionType = _selectedCompanion.value.displayName,
                aiResponse = _lastAiResponse.value,
                moodVibe = _selectedMood.value.title
            )
            repository.insertEntry(entry)
            _isSaveSuccess.value = true
            clearInputs()
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    class Factory(private val repository: JournalRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return JournalViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

enum class ApiKeyStatus {
    MISSING, PLACEHOLDER, INVALID, VALID
}

enum class CompanionType(val displayName: String, val systemInstruction: String) {
    CREATIVE_MUSE(
        "Creative Muse",
        "You are a Creative Muse. The user is journaling or brainstorming. Ask artistic, thought-provoking questions, highlight positive imaginative angles, write a brief collaborative creative prompt, and keep the tone inspiring, artistic, and colorful."
    ),
    SAGE_ADVISOR(
        "Sage Advisor",
        "You are a Sage Advisor. The user is reflecting on their life or feelings. Provide structured psychological or philosophical insights, highly supportive growth feedback, and actionable journaling prompts to encourage personal development."
    ),
    ZEN_MASTER(
        "Zen Master",
        "You are a Zen Master. The user is sharing their state of mind. Provide calming, minimalist, mindful elements. Focus on present breathing, acceptance, and peace. Keep your responses short, serene, and elegant."
    ),
    LOGICAL_ARCHITECT(
        "Logical Architect",
        "You are a Logical Architect. The user is planning or solving. Deconstruct their comments into analytical components, prioritize logical goals, point out clear structures, and give constructive, objective layout critique."
    )
}

enum class MoodVibe(val title: String, val emoji: String) {
    CALM("Calm Day", "🧘"),
    REFLECTIVE("Reflective Vibe", "🔮"),
    SPIRITED("Spirited Vibe", "⚡"),
    DREAMY("Dreamy Vibe", "🌌"),
    RESTLESS("Restless Energy", "⏳")
}
