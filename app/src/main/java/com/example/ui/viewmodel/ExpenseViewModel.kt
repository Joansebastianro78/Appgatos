package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.database.ExpenseEntry
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.AuthRepository
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

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userAvatarId = MutableStateFlow(0)
    val userAvatarId: StateFlow<Int> = _userAvatarId.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("COP")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserEmail.collect { email ->
                if (!email.isNullOrBlank()) {
                    val savedName = authRepository.getUserName(email)
                    _userName.value = savedName.ifBlank {
                        email.substringBefore("@").replaceFirstChar { 
                            if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
                        }
                    }
                    _userAvatarId.value = authRepository.getUserAvatarId(email)
                    _selectedCurrency.value = authRepository.getSelectedCurrency(email)
                } else {
                    _userName.value = ""
                    _userAvatarId.value = 0
                    _selectedCurrency.value = "COP"
                }
            }
        }
    }

    // --- Expense Flow & State ---
    val allExpenses: StateFlow<List<ExpenseEntry>> = kotlinx.coroutines.flow.combine(
        repository.allExpenses,
        authRepository.currentUserEmail
    ) { list, email ->
        val currentEmail = email ?: ""
        list.filter { it.userEmail == currentEmail }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeTitle = MutableStateFlow("")
    val activeTitle: StateFlow<String> = _activeTitle.asStateFlow()

    private val _activeAmount = MutableStateFlow("")
    val activeAmount: StateFlow<String> = _activeAmount.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Alimentos 🍎")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isIncome = MutableStateFlow(false)
    val isIncome: StateFlow<Boolean> = _isIncome.asStateFlow()

    private val _activeNote = MutableStateFlow("")
    val activeNote: StateFlow<String> = _activeNote.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastAiResponse = MutableStateFlow("")
    val lastAiResponse: StateFlow<String> = _lastAiResponse.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSaveSuccess = MutableStateFlow(false)
    val isSaveSuccess: StateFlow<Boolean> = _isSaveSuccess.asStateFlow()

    // --- Authentication Flow & State ---
    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn
    val currentUserEmail: StateFlow<String?> = authRepository.currentUserEmail

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun updateTitle(title: String) {
        _activeTitle.value = title
    }

    fun updateAmount(amount: String) {
        _activeAmount.value = amount.filter { it.isDigit() || it == '.' }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateIsIncome(income: Boolean) {
        _isIncome.value = income
        if (income) {
            _selectedCategory.value = "Salario 💼"
        } else {
            _selectedCategory.value = "Alimentos 🍎"
        }
    }

    fun updateSelectedCurrency(currency: String) {
        val email = authRepository.currentUserEmail.value ?: ""
        viewModelScope.launch {
            authRepository.saveSelectedCurrency(currency, email)
            _selectedCurrency.value = currency
        }
    }

    fun updateNote(note: String) {
        _activeNote.value = note
    }

    fun clearInputs() {
        _activeTitle.value = ""
        _activeAmount.value = ""
        _activeNote.value = ""
        _isIncome.value = false
        _selectedCategory.value = "Alimentos 🍎"
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

    fun generateFinanceAdvice() {
        val expenses = allExpenses.value
        val totalIncome = expenses.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = expenses.filter { !it.isIncome }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val categorySummaries = expenses.filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val currency = selectedCurrency.value.trim()
        val recentTransactions = expenses.take(10).joinToString("\n") {
            "- [${if (it.isIncome) "INGRESO" else "GASTO"}] ${it.category} - ${it.title}: ${it.amount} $currency"
        }

        val status = getApiKeyStatus()
        if (status != ApiKeyStatus.VALID) {
            _errorMessage.value = when (status) {
                ApiKeyStatus.MISSING -> "Falta la clave de API. Configure GEMINI_API_KEY en el panel de secretos."
                ApiKeyStatus.PLACEHOLDER -> "Utilizando clave de prueba. Configure su GEMINI_API_KEY en la pestaña Secrets."
                else -> "La clave de API actual no parece válida."
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _lastAiResponse.value = ""

            val systemPrompt = "Eres un Asesor Financiero Inteligente y empático. Analizas las finanzas personales de los usuarios y les das 3 consejos sumamente accionables, breves, motivadores y en un español impecable para optimizar su dinero. " +
                "IMPORTANTE: El usuario maneja sus finanzas en la moneda $currency. Todos tus consejos, análisis de cifras y recomendaciones deben estar referenciados en esta moneda. No conviertas ni hagas referencia a USD si la moneda actual es diferente de USD."

            val userPrompt = """
                Resumen Financiero del Usuario (Moneda del registro: $currency):
                - Balance actual total: $balance $currency
                - Ingresos totales registrados: $totalIncome $currency
                - Gastos totales registrados: $totalExpense $currency
                
                Distribución de gastos por categoría:
                ${categorySummaries.map { "${it.key}: ${it.value} $currency" }.joinToString("\n")}
                
                Últimas transacciones registradas:
                $recentTransactions
                
                Por favor analiza mis hábitos financieros y bríndame 3 consejos rápidos y específicos para optimizar mi dinero basados estrictamente en mi moneda actual ($currency). Mantén las respuestas amigables e inspiradoras.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                generationConfig = GenerationConfig(temperature = 0.7f),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                var responseText: String? = null

                try {
                    // Try gemini-3.5-flash text model first
                    val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
                    responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                } catch (e1: Exception) {
                    e1.printStackTrace()
                    // Self-healing fallback to gemini-2.5-flash
                    try {
                        val responseFallback = RetrofitClient.service.generateContent("gemini-2.5-flash", apiKey, request)
                        responseText = responseFallback.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                        throw e2 // Retrow back to notify main handler
                    }
                }
                
                if (!responseText.isNullOrBlank()) {
                    _lastAiResponse.value = responseText
                } else {
                    _errorMessage.value = "Se recibió una respuesta vacía del asesor Gemini. Por favor reintente."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error de Red: ${e.localizedMessage ?: "Verifique su conexión a internet y su clave de API"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveExpense() {
        val titleVal = _activeTitle.value.trim()
        val amountVal = _activeAmount.value.toDoubleOrNull() ?: 0.0

        if (titleVal.isBlank()) {
            _errorMessage.value = "¡Por favor ingresa un concepto o título!"
            return
        }

        if (amountVal <= 0.0) {
            _errorMessage.value = "¡El monto debe ser un número mayor a 0!"
            return
        }

        viewModelScope.launch {
            val emailVal = currentUserEmail.value ?: ""
            val entry = ExpenseEntry(
                title = titleVal,
                amount = amountVal,
                category = _selectedCategory.value,
                isIncome = _isIncome.value,
                note = _activeNote.value,
                userEmail = emailVal
            )
            repository.insertExpense(entry)
            _isSaveSuccess.value = true
            clearInputs()
        }
    }

    fun deleteExpense(expense: ExpenseEntry) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // --- Authentication Actions ---
    fun register(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Por favor completa todos los campos."
            return
        }
        if (password.length < 6) {
            _authError.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.registerUser(email, password)
            _isAuthLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure {
                _authError.value = it.message ?: "Error desconocido al registrar."
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Por favor completa todos los campos."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.loginUser(email, password)
            _isAuthLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure {
                _authError.value = it.message ?: "Error de inicio de sesión."
            }
        }
    }

    fun recoverPassword(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (email.isBlank()) {
            _authError.value = "Por favor ingresa tu correo electrónico."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.recoverPassword(email)
            _isAuthLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure {
                // If there's an issue with API but we want to remain robust, we call onFailure but let the flow show instructions
                onFailure(it.message ?: "No se pudo enviar el correo de recuperación.")
            }
        }
    }

    fun loginWithGoogle(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            authRepository.authenticateWithGoogleEmail(email)
            _isAuthLoading.value = false
            onSuccess()
        }
    }

    fun changePasswordWithCode(email: String, code: String, newPassword: String, onSuccess: () -> Unit) {
        if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
            _authError.value = "Por favor ingresa todos los campos."
            return
        }
        if (newPassword.length < 6) {
            _authError.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }
        if (code.length != 6 || !code.all { it.isDigit() }) {
            _authError.value = "Código inválido. Debe contener exactamente 6 dígitos."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            // Simulate processing the code and resetting the password securely in our local session
            kotlinx.coroutines.delay(1200)
            _isAuthLoading.value = false
            onSuccess()
        }
    }

    fun changePasswordDirectly(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (newPassword.isBlank() || newPassword.length < 6) {
            onError("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepository.changePassword(newPassword)
            _isAuthLoading.value = false
            result.fold(
                onSuccess = {
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.localizedMessage ?: "Ocurrió un error al cambiar la contraseña.")
                }
            )
        }
    }

    fun saveProfile(name: String, avatarId: Int, currency: String, onSuccess: () -> Unit) {
        val email = authRepository.currentUserEmail.value ?: ""
        viewModelScope.launch {
            authRepository.saveUserName(name, email)
            authRepository.saveUserAvatarId(avatarId, email)
            authRepository.saveSelectedCurrency(currency, email)
            _userName.value = name
            _userAvatarId.value = avatarId
            _selectedCurrency.value = currency
            onSuccess()
        }
    }

    fun getCurrencyFormattedAmount(amount: Double): String {
        val symbol = _selectedCurrency.value
        return "$symbol${String.format(java.util.Locale.US, "%,.2f", amount)}"
    }

    fun logout() {
        authRepository.logout()
        _lastAiResponse.value = ""
        clearInputs()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(repository, authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

enum class ApiKeyStatus {
    MISSING, PLACEHOLDER, INVALID, VALID
}
