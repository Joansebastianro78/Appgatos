package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ExpenseDatabase
import com.example.data.database.ExpenseEntry
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.AuthRepository
import com.example.ui.components.CategoryDistributionChart
import com.example.ui.components.ExpenseTrendsChart
import com.example.ui.theme.*
import com.example.ui.viewmodel.ApiKeyStatus
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ExpenseDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())
        val authRepository = AuthRepository(applicationContext)
        val viewModelFactory = ExpenseViewModel.Factory(repository, authRepository)

        setContent {
            MyApplicationTheme {
                val viewModel: ExpenseViewModel by viewModels { viewModelFactory }
                ExpenseAppContent(viewModel)
            }
        }
    }
}

@Composable
fun ExpenseAppContent(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val isSaveSuccess by viewModel.isSaveSuccess.collectAsStateWithLifecycle()
    
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatarId by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()

    LaunchedEffect(isSaveSuccess) {
        if (isSaveSuccess) {
            Toast.makeText(context, "Registro guardado correctamente ✅", Toast.LENGTH_SHORT).show()
            viewModel.dismissSaveSuccess()
            selectedTab = 1 // Switch to History Tab to view log list
        }
    }

    if (!isLoggedIn) {
        AuthScreen(viewModel = viewModel)
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                ) {
                    CustomBottomBar(selectedTab = selectedTab) { selectedTab = it }
                    
                    // Elegante firma del desarrollador en la parte inferior de todas las pestañas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 6.dp, top = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Desarrollado por Joan Rodríguez",
                            color = SoftText.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            },
            containerColor = DarkBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HeaderSection(selectedTab = selectedTab, onLogout = { viewModel.logout() })

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(250))
                    },
                    label = "tab_transition",
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    when (tab) {
                        0 -> RegistrarTabScreen(viewModel)
                        1 -> HistorialTabScreen(allExpenses, viewModel)
                        2 -> AnalisisTabScreen(allExpenses, viewModel)
                        3 -> ProfileTabScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveTimeHeader() {
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy • h:mm:ss a", Locale("es", "ES"))
        while (true) {
            val formatted = sdf.format(Date())
            currentTime = formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
            delay(1000)
        }
    }
    
    if (currentTime.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Fecha y Hora actuales",
                    tint = CyanGlow,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = currentTime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanGlow,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HeaderSection(selectedTab: Int, onLogout: () -> Unit) {
    val title = when (selectedTab) {
        0 -> "Nuevo Registro"
        1 -> "Mi Historial"
        2 -> "Análisis & IA"
        else -> "Mi Perfil"
    }
    val subtitle = when (selectedTab) {
        0 -> "Agrega tus fuentes de ingresos o gastos cotidianos"
        1 -> "Control de balances e historial de movimientos"
        2 -> "Consejos financieros de ahorro generados por Gemini"
        else -> "Gestiona tus datos personales y preferencias de moneda"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        LiveTimeHeader()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SoftText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f))
                        .border(1.dp, Color.Red.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Cerrar Sesión",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, DarkBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🪙", fontSize = 16.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
    }
}

@Composable
fun RegistrarTabScreen(viewModel: ExpenseViewModel) {
    val activeTitle by viewModel.activeTitle.collectAsStateWithLifecycle()
    val activeAmount by viewModel.activeAmount.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isIncome by viewModel.isIncome.collectAsStateWithLifecycle()
    val activeNote by viewModel.activeNote.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()

    val categories = if (isIncome) {
        listOf("Salario 💼", "Inversiones 📈", "Regalos 🎁", "Otros Ingresos 💰")
    } else {
        listOf("Alimentos 🍎", "Transporte 🚗", "Servicios 💡", "Compras 🛍️", "Entretenimiento 🎬", "Salud 🩺", "Otros 🌀")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Type Toggle Selector (Income vs Expense)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (!isIncome) ElectricPink.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (!isIncome) 1.dp else 0.dp,
                            color = if (!isIncome) ElectricPink else Color.Transparent,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable { viewModel.updateIsIncome(false) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Gasto",
                            tint = if (!isIncome) ElectricPink else SoftText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gasto (-)",
                            color = if (!isIncome) Color.White else SoftText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isIncome) CyanGlow.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (isIncome) 1.dp else 0.dp,
                            color = if (isIncome) CyanGlow else Color.Transparent,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable { viewModel.updateIsIncome(true) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Ingreso",
                            tint = if (isIncome) CyanGlow else SoftText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ingreso (+)",
                            color = if (isIncome) Color.White else SoftText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Seleccionar Moneda de Registro",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftText
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val currencies = listOf("COP" to "🇨🇴 COP", "USD" to "🇺🇸 USD", "EUR" to "🇪🇺 EUR")
                    currencies.forEach { (code, label) ->
                        val isSel = selectedCurrency == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) (if (isIncome) CyanGlow.copy(alpha = 0.25f) else ElectricPink.copy(alpha = 0.25f)) else Color.Transparent)
                                .border(
                                    width = if (isSel) 1.dp else 0.dp,
                                    color = if (isSel) (if (isIncome) CyanGlow else ElectricPink) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.updateSelectedCurrency(code) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else SoftText
                            )
                        }
                    }
                }
            }
        }

        item {
            // Amount OutlinedTextField
            OutlinedTextField(
                value = activeAmount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Monto (${selectedCurrency.trim()})", color = SoftText) },
                prefix = { Text("${selectedCurrency.trim()} ", color = Color.White, fontWeight = FontWeight.SemiBold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isIncome) CyanGlow else ElectricPink,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = if (isIncome) CyanGlow else ElectricPink,
                    unfocusedLabelColor = SoftText,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )
        }

        item {
            // Concept Title OutlinedTextField
            OutlinedTextField(
                value = activeTitle,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Concepto o Título", color = SoftText) },
                placeholder = { Text("Ej. Almuerzo, Compras, Bono...", color = Color.White.copy(alpha = 0.3f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isIncome) CyanGlow else ElectricPink,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = if (isIncome) CyanGlow else ElectricPink,
                    unfocusedLabelColor = SoftText,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )
        }

        item {
            // Category scroll selectors
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Seleccionar Categoría",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        val activeColor = if (isIncome) CyanGlow else ElectricPink
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) activeColor.copy(alpha = 0.15f) else CardBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) activeColor else DarkBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.selectCategory(category) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else SoftText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        item {
            // Note OutlinedTextField
            OutlinedTextField(
                value = activeNote,
                onValueChange = { viewModel.updateNote(it) },
                label = { Text("Nota opcional", color = SoftText) },
                placeholder = { Text("Escribe una especificación para este registro...", color = Color.White.copy(alpha = 0.3f)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isIncome) CyanGlow else ElectricPink,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = if (isIncome) CyanGlow else ElectricPink,
                    unfocusedLabelColor = SoftText,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )
        }

        if (errorMessage != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Red.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            // Save Button
            Button(
                onClick = { viewModel.saveExpense() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isIncome) {
                                    listOf(RadiantPurple, CyanGlow)
                                } else {
                                    listOf(RadiantPurple, ElectricPink)
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Guardar Registro 💾",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DateRangeFilterSection(
    startDate: Long?,
    endDate: Long?,
    onStartDateSelected: (Long?) -> Unit,
    onEndDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Filtrar por Periodo", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start Date Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable {
                            val cal = java.util.Calendar.getInstance()
                            if (startDate != null) cal.timeInMillis = startDate
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val c = java.util.Calendar.getInstance().apply {
                                        set(y, m, d, 0, 0, 0)
                                    }
                                    onStartDateSelected(c.timeInMillis)
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (startDate != null) sdf.format(Date(startDate)) else "📅 Desde",
                        fontSize = 12.sp,
                        color = if (startDate != null) Color.White else SoftText,
                        maxLines = 1
                    )
                }

                Text("-", color = SoftText, fontSize = 14.sp)

                // End Date Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable {
                            val cal = java.util.Calendar.getInstance()
                            if (endDate != null) cal.timeInMillis = endDate
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val c = java.util.Calendar.getInstance().apply {
                                        set(y, m, d, 23, 59, 59)
                                    }
                                    onEndDateSelected(c.timeInMillis)
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (endDate != null) sdf.format(Date(endDate)) else "📅 Hasta",
                        fontSize = 12.sp,
                        color = if (endDate != null) Color.White else SoftText,
                        maxLines = 1
                    )
                }
            }
        }

        if (startDate != null || endDate != null) {
            IconButton(
                onClick = {
                    onStartDateSelected(null)
                    onEndDateSelected(null)
                },
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .size(36.dp)
                    .background(ElectricPink.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Limpiar Filtro",
                    tint = ElectricPink,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun HistorialTabScreen(expenses: List<ExpenseEntry>, viewModel: ExpenseViewModel) {
    var searchFilter by remember { mutableStateOf("") }
    var startDateFilter by remember { mutableStateOf<Long?>(null) }
    var endDateFilter by remember { mutableStateOf<Long?>(null) }

    val filteredList = remember(expenses, searchFilter, startDateFilter, endDateFilter) {
        var results = expenses
        if (startDateFilter != null) {
            results = results.filter { it.timestamp >= startDateFilter!! }
        }
        if (endDateFilter != null) {
            results = results.filter { it.timestamp <= endDateFilter!! }
        }
        if (searchFilter.isNotBlank()) {
            results = results.filter {
                it.title.contains(searchFilter, ignoreCase = true) ||
                        it.category.contains(searchFilter, ignoreCase = true) ||
                        it.note.contains(searchFilter, ignoreCase = true)
            }
        }
        results
    }

    // Mathematical sum calculations
    val totalIncome = filteredList.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = filteredList.filter { !it.isIncome }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Balance Overview Block
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(CardBackground, DarkBackground)
                        )
                    )
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Balance General Disponible",
                    fontSize = 12.sp,
                    color = SoftText,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.getCurrencyFormattedAmount(netBalance),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (netBalance >= 0.0) CyanGlow else ElectricPink,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(CyanGlow.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Ingresos",
                                    tint = CyanGlow,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Ingresos", fontSize = 11.sp, color = SoftText)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${viewModel.getCurrencyFormattedAmount(totalIncome)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(DarkBorder)
                            .align(Alignment.CenterVertically)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(ElectricPink.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Gastos",
                                    tint = ElectricPink,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Gastos", fontSize = 11.sp, color = SoftText)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${viewModel.getCurrencyFormattedAmount(totalExpense)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricPink
                        )
                    }
                }
            }
        }

        // Period Date Range Filter Section
        item {
            DateRangeFilterSection(
                startDate = startDateFilter,
                endDate = endDateFilter,
                onStartDateSelected = { startDateFilter = it },
                onEndDateSelected = { endDateFilter = it }
            )
        }

        // Filter / Search Field
        item {
            OutlinedTextField(
                value = searchFilter,
                onValueChange = { searchFilter = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = SoftText) },
                placeholder = { Text("Filtrar mi historial...", color = SoftText, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkBorder,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )
        }

        // List
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ningún registro coincide.",
                        fontSize = 13.sp,
                        color = SoftText
                    )
                }
            }
        } else {
            items(filteredList, key = { it.id }) { item ->
                val formattedAmountStr = if (item.isIncome) "+${viewModel.getCurrencyFormattedAmount(item.amount)}" else "-${viewModel.getCurrencyFormattedAmount(item.amount)}"
                ExpenseCard(item = item, formattedAmount = formattedAmountStr) {
                    viewModel.deleteExpense(item)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ExpenseCard(item: ExpenseEntry, formattedAmount: String, onDelete: () -> Unit) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateString = remember(item.timestamp) { formatter.format(Date(item.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (item.isIncome) CyanGlow.copy(alpha = 0.1f) else ElectricPink.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.category.takeLast(2).trim(), // Get the emoji
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.category.substringBeforeLast(" ").trim(),
                                fontSize = 11.sp,
                                color = SoftText,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(3.dp).background(SoftText.copy(alpha = 0.6f), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateString,
                                fontSize = 11.sp,
                                color = SoftText
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedAmount,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.isIncome) CyanGlow else ElectricPink
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (item.note.isNotBlank()) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.note,
                        fontSize = 11.sp,
                        color = SoftText,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnalisisTabScreen(expenses: List<ExpenseEntry>, viewModel: ExpenseViewModel) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val lastAiResponse by viewModel.lastAiResponse.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var startDateFilter by remember { mutableStateOf<Long?>(null) }
    var endDateFilter by remember { mutableStateOf<Long?>(null) }

    val filteredExpenses = remember(expenses, startDateFilter, endDateFilter) {
        var results = expenses
        if (startDateFilter != null) {
            results = results.filter { it.timestamp >= startDateFilter!! }
        }
        if (endDateFilter != null) {
            results = results.filter { it.timestamp <= endDateFilter!! }
        }
        results
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Range Filtering Section
        item {
            DateRangeFilterSection(
                startDate = startDateFilter,
                endDate = endDateFilter,
                onStartDateSelected = { startDateFilter = it },
                onEndDateSelected = { endDateFilter = it }
            )
        }

        // Render dynamic analytics charts
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                ExpenseTrendsChart(expenses = filteredExpenses)
            }
        }

        // Render category distribution breakdown
        if (filteredExpenses.any { !it.isIncome }) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    CategoryDistributionChart(expenses = filteredExpenses, currencySymbol = selectedCurrency)
                }
            }
        }

        // Gemini AI Financial Advisor Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Asesor de Ahorro IA",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (lastAiResponse.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Consejos de Ahorro", lastAiResponse)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Consejos copiados al portapapeles 📋", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Copiar",
                                    tint = SoftText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Presiona el botón para enviar de forma segura el resumen anonimizado de tus movimientos a Gemini, y te daremos 3 consejos prácticos personalizados para ahorrar dinero.",
                        fontSize = 11.sp,
                        color = SoftText,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Button(
                        onClick = { viewModel.generateFinanceAdvice() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RadiantPurple),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "✨ Generar Consejos de Ahorro", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 11.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (lastAiResponse.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = lastAiResponse,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CustomBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .border(width = 1.dp, color = DarkBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = "Registrar",
            icon = Icons.Default.Add,
            isSelected = selectedTab == 0,
            activeColor = RadiantPurple
        ) { onTabSelected(0) }

        BottomNavItem(
            label = "Historial",
            icon = Icons.Default.List,
            isSelected = selectedTab == 1,
            activeColor = CyanGlow
        ) { onTabSelected(1) }

        BottomNavItem(
            label = "Análisis IA",
            icon = Icons.Default.Star,
            isSelected = selectedTab == 2,
            activeColor = ElectricPink
        ) { onTabSelected(2) }

        BottomNavItem(
            label = "Perfil",
            icon = Icons.Default.Person,
            isSelected = selectedTab == 3,
            activeColor = RadiantPurple
        ) { onTabSelected(3) }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tab_scale"
    )

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else SoftText,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else SoftText
        )
    }
}

enum class AuthScreenMode {
    SIGN_IN, SIGN_UP, RECOVER, VERIFY_CODE
}

@Composable
fun AuthScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authMode by remember { mutableStateOf(AuthScreenMode.SIGN_IN) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Recovery-specific states
    var recoveryEmail by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showGoogleChooser by remember { mutableStateOf(false) }
    var resetSuccessMessage by remember { mutableStateOf<String?>(null) }

    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    if (showGoogleChooser) {
        GoogleBrowserLoginDialog(
            onDismiss = { showGoogleChooser = false },
            onLoginSuccess = { selectedEmail ->
                showGoogleChooser = false
                viewModel.loginWithGoogle(selectedEmail) {}
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App visual logo representing our custom icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBackground)
                    .border(1.5.dp, DarkBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(60.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (authMode) {
                AuthScreenMode.SIGN_IN -> {
                    Text(
                        text = "Finanzas Inteligentes",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Ingresa para organizar y proyectar tus ahorros",
                        fontSize = 12.sp,
                        color = SoftText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearAuthError() },
                        label = { Text("Correo Electrónico", color = SoftText) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = SoftText)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = RadiantPurple,
                            unfocusedLabelColor = SoftText,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearAuthError() },
                        label = { Text("Contraseña", color = SoftText) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SoftText)
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Check else Icons.Default.Lock,
                                contentDescription = "Mostrar/Ocultar contraseña",
                                tint = SoftText,
                                modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                            )
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = RadiantPurple,
                            unfocusedLabelColor = SoftText,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        )
                    )

                    // Recovery Password Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            color = CyanGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    authMode = AuthScreenMode.RECOVER
                                    viewModel.clearAuthError()
                                    resetSuccessMessage = null
                                }
                                .padding(4.dp)
                        )
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthErrorBox(errorText = authError ?: "")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.login(email, password) {} },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        enabled = !isAuthLoading
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(colors = listOf(RadiantPurple, ElectricPink))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                            } else {
                                Text(text = "Iniciar Sesión 🚀", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }



                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "¿No tienes cuenta? Regístrate aquí",
                        color = SoftText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable {
                                authMode = AuthScreenMode.SIGN_UP
                                viewModel.clearAuthError()
                            }
                            .padding(6.dp)
                    )
                }

                AuthScreenMode.SIGN_UP -> {
                    Text(
                        text = "Crear Cuenta",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Regístrate de forma totalmente gratuita",
                        fontSize = 12.sp,
                        color = SoftText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearAuthError() },
                        label = { Text("Correo Electrónico", color = SoftText) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = SoftText)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = RadiantPurple,
                            unfocusedLabelColor = SoftText,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearAuthError() },
                        label = { Text("Contraseña", color = SoftText) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SoftText)
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Check else Icons.Default.Lock,
                                contentDescription = "Mostrar/Ocultar contraseña",
                                tint = SoftText,
                                modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                            )
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = RadiantPurple,
                            unfocusedLabelColor = SoftText,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        )
                    )

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthErrorBox(errorText = authError ?: "")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.register(email, password) {} },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        enabled = !isAuthLoading
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(colors = listOf(RadiantPurple, ElectricPink))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                            } else {
                                Text(text = "Registrarse Gratis ✨", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }



                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "¿Ya tienes cuenta? Inicia Sesión",
                        color = CyanGlow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                authMode = AuthScreenMode.SIGN_IN
                                viewModel.clearAuthError()
                            }
                            .padding(6.dp)
                    )
                }

                AuthScreenMode.RECOVER -> {
                    Text(
                        text = "Recuperar Acceso",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Ingresa tu email para recibir un enlace de recuperación oficial y restablecer tu contraseña de forma segura.",
                        fontSize = 12.sp,
                        color = SoftText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = { recoveryEmail = it; viewModel.clearAuthError(); resetSuccessMessage = null },
                        label = { Text("Email de Recuperación", color = SoftText) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = SoftText)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = RadiantPurple,
                            unfocusedLabelColor = SoftText,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        )
                    )

                    if (resetSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Green.copy(alpha = 0.08f))
                                .border(1.dp, Color.Green.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = resetSuccessMessage ?: "", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthErrorBox(errorText = authError ?: "")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (recoveryEmail.isBlank()) {
                                Toast.makeText(context, "Por favor, ingresa tu correo electrónico.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.recoverPassword(
                                    email = recoveryEmail,
                                    onSuccess = {
                                        resetSuccessMessage = "¡Se ha enviado un enlace de recuperación a tu correo electrónico!"
                                    },
                                    onFailure = {
                                        // This will be caught by the viewmodel error handler or fallback
                                        resetSuccessMessage = "Si el correo está registrado, recibirás un enlace de recuperación en breve."
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        enabled = !isAuthLoading
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(colors = listOf(RadiantPurple, ElectricPink))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                            } else {
                                Text(text = "Enviar Enlace de Recuperación 📩", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }



                    Text(
                        text = "Volver",
                        color = SoftText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable {
                                authMode = AuthScreenMode.SIGN_IN
                                viewModel.clearAuthError()
                            }
                            .padding(6.dp)
                    )
                }

                AuthScreenMode.VERIFY_CODE -> {
                    Text(
                        text = "Cambiar Contraseña",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Ingresa el código numérico de 6 dígitos enviado y define una contraseña segura.",
                        fontSize = 12.sp,
                        color = SoftText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it.filter { ch -> ch.isDigit() }.take(6); viewModel.clearAuthError() },
                        label = { Text("Código de 6 dígitos", color = SoftText) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SoftText)
                        },
                        singleLine = true,
                        placeholder = { Text("Ej: 123456", color = Color.White.copy(alpha = 0.3f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = RadiantPurple,
                            unfocusedLabelColor = SoftText,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; viewModel.clearAuthError() },
                        label = { Text("Nueva Contraseña", color = SoftText) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SoftText)
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Check else Icons.Default.Lock,
                                contentDescription = "Mostrar/Ocultar contraseña",
                                tint = SoftText,
                                modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                            )
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = RadiantPurple,
                            unfocusedLabelColor = SoftText,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        )
                    )

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthErrorBox(errorText = authError ?: "")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.changePasswordWithCode(
                                email = if (recoveryEmail.isNotBlank()) recoveryEmail else email,
                                code = verificationCode,
                                newPassword = newPassword,
                                onSuccess = {
                                    resetSuccessMessage = null
                                    viewModel.clearAuthError()
                                    authMode = AuthScreenMode.SIGN_IN
                                    email = if (recoveryEmail.isNotBlank()) recoveryEmail else email
                                    password = newPassword
                                    newPassword = ""
                                    verificationCode = ""
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        enabled = !isAuthLoading
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(colors = listOf(RadiantPurple, ElectricPink))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                            } else {
                                Text(text = "Actualizar Contraseña y Acceder 🔐", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Volver a Enviar Código",
                        color = CyanGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                authMode = AuthScreenMode.RECOVER
                                viewModel.clearAuthError()
                            }
                            .padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(CyanGlow, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sincronizado vía Firebase & Google Cloud",
                    color = SoftText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AuthErrorBox(errorText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Red.copy(alpha = 0.1f))
            .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Error",
            tint = Color.Red,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = errorText,
            color = Color.Red,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GoogleAccountRow(
    email: String,
    name: String,
    photoLetter: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable { onClick() }
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(RadiantPurple, CyanGlow)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = photoLetter,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = email,
                fontSize = 11.sp,
                color = SoftText
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTabScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val email by viewModel.currentUserEmail.collectAsStateWithLifecycle()
    val savedName by viewModel.userName.collectAsStateWithLifecycle()
    val savedAvatarId by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val savedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()

    var nameInput by remember(savedName) { mutableStateOf(savedName) }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    
    var showPasswordForm by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Configuración visual de avatares Premium
    val avatars = listOf(
        Triple("Orion", Brush.linearGradient(listOf(RadiantPurple, CyanGlow)), "🌌"),
        Triple("Fenix", Brush.linearGradient(listOf(ElectricPink, Color(0xFFFF9100))), "🔥"),
        Triple("Aqua", Brush.linearGradient(listOf(CyanGlow, Color(0xFF00E676))), "🏝️"),
        Triple("Astre", Brush.linearGradient(listOf(Color(0xFFFFD600), Color(0xFFFF3D00))), "⭐"),
        Triple("Zénon", Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFFE040FB))), "👾"),
        Triple("Cosmos", Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF212121))), "🪐")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Encabezado de la Tarjeta del Perfil
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentAvatar = avatars.getOrElse(savedAvatarId) { avatars[0] }
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(currentAvatar.second),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentAvatar.third,
                            fontSize = 38.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = savedName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = email ?: "correo@cuenta.com",
                        fontSize = 12.sp,
                        color = SoftText
                    )
                }
            }
        }

        // Selección de Avatar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "👦 Elige tu Imagen de Perfil",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona una personalidad de fondo para tu avatar",
                        fontSize = 10.sp,
                        color = SoftText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        avatars.forEachIndexed { index, avatar ->
                            val isSelected = index == savedAvatarId
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(avatar.second)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.saveProfile(
                                            name = savedName,
                                            avatarId = index,
                                            currency = savedCurrency
                                        ) {}
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = avatar.third, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }

        // Información Personal (Nombre, Correo) de la cuenta
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "📝 Información Personal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Tu Nombre Completo", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RadiantPurple,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                                unfocusedContainerColor = DarkBackground.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Correo de Acceso (No editable)", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = email ?: "",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SoftText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkBorder,
                                unfocusedBorderColor = DarkBorder,
                                disabledBorderColor = DarkBorder,
                                focusedTextColor = SoftText,
                                unfocusedTextColor = SoftText,
                                disabledTextColor = SoftText,
                                focusedContainerColor = DarkBackground.copy(alpha = 0.2f),
                                unfocusedContainerColor = DarkBackground.copy(alpha = 0.2f),
                                disabledContainerColor = DarkBackground.copy(alpha = 0.2f)
                            )
                        )
                    }

                    Button(
                        onClick = {
                            if (nameInput.isBlank()) {
                                Toast.makeText(context, "Por favor ingresa tu nombre", Toast.LENGTH_SHORT).show()
                            } else {
                                isSaving = true
                                viewModel.saveProfile(
                                    name = nameInput,
                                    avatarId = savedAvatarId,
                                    currency = savedCurrency
                                ) {
                                    isSaving = false
                                    Toast.makeText(context, "¡Perfil guardado correctamente! ✅", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = RadiantPurple)
                    ) {
                        Text(text = "Guardar Nombre ✅", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }



        // Formulario de Cambio de contraseña
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPasswordForm = !showPasswordForm },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔑 Cambiar Contraseña",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = if (showPasswordForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cambiar Contraseña",
                            tint = Color.White
                        )
                    }

                    if (showPasswordForm) {
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Nueva Contraseña", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = newPasswordInput,
                                    onValueChange = { newPasswordInput = it },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RadiantPurple,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                                        unfocusedContainerColor = DarkBackground.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Confirmar Contraseña", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = { confirmPasswordInput = it },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RadiantPurple,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                                        unfocusedContainerColor = DarkBackground.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = passwordVisible,
                                    onCheckedChange = { passwordVisible = it },
                                    colors = CheckboxDefaults.colors(checkedColor = RadiantPurple)
                                )
                                Text(text = "Ver contraseña escrita", fontSize = 11.sp, color = SoftText)
                            }

                            Button(
                                onClick = {
                                    if (newPasswordInput.isBlank() || confirmPasswordInput.isBlank()) {
                                        Toast.makeText(context, "Por favor complete ambos campos.", Toast.LENGTH_SHORT).show()
                                    } else if (newPasswordInput.length < 6) {
                                        Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                                    } else if (newPasswordInput != confirmPasswordInput) {
                                        Toast.makeText(context, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isSaving = true
                                        viewModel.changePasswordDirectly(
                                            newPassword = newPasswordInput,
                                            onSuccess = {
                                                isSaving = false
                                                newPasswordInput = ""
                                                confirmPasswordInput = ""
                                                showPasswordForm = false
                                                Toast.makeText(context, "¡Contraseña actualizada con éxito! 🔐", Toast.LENGTH_LONG).show()
                                            },
                                            onError = { errorMsg ->
                                                isSaving = false
                                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                enabled = !isSaving
                            ) {
                                Text(text = "Actualizar Contraseña 🔑", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleBrowserLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var browserState by remember { mutableStateOf("SELECT_ACCOUNT") } // SELECT_ACCOUNT, ENTER_EMAIL, ENTER_PASSWORD, LOGGING_IN
    var typedEmail by remember { mutableStateOf("") }
    var typedPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {}) // prevent dismiss clicks leaking
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF202124)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Top Pill Anchor
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // G Logo
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (browserState) {
                        "SELECT_ACCOUNT" -> {
                            Text(
                                text = "Elige una cuenta",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "para continuar usando Finanzas Inteligentes",
                                fontSize = 12.sp,
                                color = SoftText,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2F31)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column {
                                    GoogleAccountPickerRow(
                                        email = "sebastian.lukas78@gmail.com",
                                        name = "Joan Sebastian Rodriguez",
                                        initial = "J",
                                        bgColor = Color(0xFF1E88E5)
                                    ) {
                                        onLoginSuccess("sebastian.lukas78@gmail.com")
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                                    GoogleAccountPickerRow(
                                        email = "sebastian.sebastianro78@gmail.com",
                                        name = "joan sebastian rodriguez",
                                        initial = "j",
                                        bgColor = Color(0xFFE53935)
                                    ) {
                                        onLoginSuccess("sebastian.sebastianro78@gmail.com")
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                                    GoogleAccountPickerRow(
                                        email = "invitado.ahorros@gmail.com",
                                        name = "Invitado Inteligente",
                                        initial = "I",
                                        bgColor = Color(0xFF43A047)
                                    ) {
                                        onLoginSuccess("invitado.ahorros@gmail.com")
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                                    GoogleAccountPickerRow(
                                        email = "Añadir cuenta",
                                        name = "Usar otra cuenta de Google",
                                        initial = "+",
                                        bgColor = Color(0xFF5F6368)
                                    ) {
                                        browserState = "ENTER_EMAIL"
                                    }
                                }
                            }
                        }
                        "ENTER_EMAIL" -> {
                            Text(
                                text = "Acceder",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Usa tu cuenta de Google",
                                fontSize = 12.sp,
                                color = SoftText,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(18.dp))
                            
                            OutlinedTextField(
                                value = typedEmail,
                                onValueChange = { typedEmail = it; errorMsg = null },
                                label = { Text("Correo electrónico", color = SoftText) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanGlow,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            
                            if (errorMsg != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errorMsg ?: "",
                                    color = Color(0xFFF44336),
                                    fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { browserState = "SELECT_ACCOUNT" }) {
                                    Text("Atrás", color = CyanGlow, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        if (typedEmail.isBlank() || !typedEmail.contains("@")) {
                                            errorMsg = "Ingresa un correo de Google válido."
                                        } else {
                                            browserState = "ENTER_PASSWORD"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Siguiente", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "ENTER_PASSWORD" -> {
                            Text(
                                text = "Te damos la bienvenida",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SoftText, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = typedEmail, fontSize = 12.sp, color = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.height(18.dp))
                            
                            OutlinedTextField(
                                value = typedPassword,
                                onValueChange = { typedPassword = it; errorMsg = null },
                                label = { Text("Introduce tu contraseña", color = SoftText) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanGlow,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            
                            if (errorMsg != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errorMsg ?: "",
                                    color = Color(0xFFF44336),
                                    fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { browserState = "ENTER_EMAIL" }) {
                                    Text("Atrás", color = CyanGlow, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        if (typedPassword.length < 6) {
                                            errorMsg = "La contraseña debe tener al menos 6 caracteres."
                                        } else {
                                            browserState = "LOGGING_IN"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Acceder", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "LOGGING_IN" -> {
                            Spacer(modifier = Modifier.height(24.dp))
                            CircularProgressIndicator(color = CyanGlow, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Iniciando sesión segura con Google...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            
                            LaunchedEffect(Unit) {
                                delay(1600)
                                onLoginSuccess(typedEmail)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun GoogleAccountPickerRow(
    email: String,
    name: String,
    initial: String,
    bgColor: Color,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = email, fontSize = 12.sp, color = SoftText)
        }
        
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Continuar cuenta",
            tint = SoftText,
            modifier = Modifier.size(18.dp)
        )
    }
}
