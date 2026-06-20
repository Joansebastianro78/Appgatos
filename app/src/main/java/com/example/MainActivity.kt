package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.JournalDatabase
import com.example.data.database.JournalEntry
import com.example.data.repository.JournalRepository
import com.example.ui.components.AuraPulseChart
import com.example.ui.theme.*
import com.example.ui.viewmodel.ApiKeyStatus
import com.example.ui.viewmodel.CompanionType
import com.example.ui.viewmodel.JournalViewModel
import com.example.ui.viewmodel.MoodVibe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core room context setup
        val database = JournalDatabase.getDatabase(applicationContext)
        val repository = JournalRepository(database.journalDao())
        val viewModelFactory = JournalViewModel.Factory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: JournalViewModel by viewModels { viewModelFactory }
                AuraAppContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuraAppContent(viewModel: JournalViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val isSaveSuccess by viewModel.isSaveSuccess.collectAsStateWithLifecycle()

    // Handle save success alerts
    LaunchedEffect(isSaveSuccess) {
        if (isSaveSuccess) {
            Toast.makeText(context, "Reflection anchored securely to your Mind Space!", Toast.LENGTH_SHORT).show()
            viewModel.dismissSaveSuccess()
            selectedTab = 1 // Navigate to History Tab to view
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        bottomBar = {
            CustomBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            HeaderSection()

            when (selectedTab) {
                0 -> WriteTabScreen(viewModel)
                1 -> HistoryTabScreen(allEntries, viewModel)
                2 -> AnalyticsTabScreen(allEntries)
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, CardBackground)
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "AURA JOURNAL",
                    fontSize = 26.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = RadiantPurple
                )
                Text(
                    text = "Mind Resonance & Ideas Organizer",
                    fontSize = 12.sp,
                    color = SoftText
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(RadiantPurple, ElectricPink)
                        )
                    )
                    .padding(2.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(CardBackground)
                ) {
                    Text(text = "🌌", fontSize = 18.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WriteTabScreen(viewModel: JournalViewModel) {
    val activeTitle by viewModel.activeTitle.collectAsStateWithLifecycle()
    val activeContent by viewModel.activeContent.collectAsStateWithLifecycle()
    val selectedCompanion by viewModel.selectedCompanion.collectAsStateWithLifecycle()
    val selectedMood by viewModel.selectedMood.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val lastAiResponse by viewModel.lastAiResponse.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector: AI Companion Choice
        item {
            Text(
                text = "Choose your Spark Companion",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompanionType.values().forEach { companion ->
                    val isSelected = companion == selectedCompanion
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) RadiantPurple else CardBackground)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) ElectricPink else DarkBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectCompanion(companion) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = companion.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else SoftText
                        )
                    }
                }
            }
        }

        // Mood Vibe selection
        item {
            Text(
                text = "Select your current Vibe",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MoodVibe.values().forEach { mood ->
                    val isSelected = mood == selectedMood
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CardBackground else Color.Transparent)
                            .clickable { viewModel.selectMood(mood) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = mood.emoji,
                            fontSize = 24.sp,
                            modifier = Modifier.scaleOnPress(isSelected)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mood.title.split(" ")[0],
                            fontSize = 10.sp,
                            color = if (isSelected) ElectricPink else SoftText
                        )
                    }
                }
            }
        }

        // Text Inputs
        item {
            Text(
                text = "Write your mind reflection",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = activeTitle,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Title (Optional)", color = SoftText) },
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LightText,
                    unfocusedTextColor = SoftText,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedBorderColor = RadiantPurple,
                    unfocusedBorderColor = DarkBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = activeContent,
                onValueChange = { viewModel.updateContent(it) },
                label = { Text("What is on your mind? Brainstorm freely...", color = SoftText) },
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LightText,
                    unfocusedTextColor = SoftText,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedBorderColor = RadiantPurple,
                    unfocusedBorderColor = DarkBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Status or Error card
        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1B1F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "Error", color = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Aura Error Feedback", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage ?: "", color = LightText, fontSize = 12.sp)

                        if (viewModel.getApiKeyStatus() != ApiKeyStatus.VALID) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 To fix this:\n1. Open Google AI Studio 'Secrets' panel.\n2. Add a key named 'GEMINI_API_KEY'.\n3. Click reload/save. No rebuild needed!",
                                color = GoldenAmber,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Action Trigger buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Generate Insight Button
                Button(
                    onClick = { viewModel.generateInsight() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantPurple,
                        disabledContainerColor = RadiantPurple.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(size = 18.dp, color = Color.White)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Resonate AI Insight ✨", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                // Save Entry Button
                Button(
                    onClick = { viewModel.saveSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ElectricPink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Anchor Reflection 🔒", fontSize = 12.sp, color = ElectricPink)
                    }
                }
            }
        }

        // Live AI Insight Output Display
        if (lastAiResponse.isNotBlank() || isLoading) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CyanGlow),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "✨", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${selectedCompanion.displayName}'s Insight",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanGlow
                                )
                            }
                            // Copy button
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Aura Insight", lastAiResponse)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Insight copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Copy text", color = SoftText, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (isLoading && lastAiResponse.isBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = RadiantPurple, strokeWidth = 1.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Resonating cosmic insights...", fontSize = 12.sp, color = SoftText)
                            }
                        } else {
                            Text(
                                text = lastAiResponse,
                                fontSize = 13.sp,
                                color = LightText,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun HistoryTabScreen(entries: List<JournalEntry>, viewModel: JournalViewModel) {
    var searchFilter by remember { mutableStateOf("") }
    val filteredEntries = entries.filter {
        it.title.contains(searchFilter, ignoreCase = true) ||
        it.content.contains(searchFilter, ignoreCase = true) ||
        it.companionType.contains(searchFilter, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Search filter bar
        OutlinedTextField(
            value = searchFilter,
            onValueChange = { searchFilter = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", color = SoftText) },
            placeholder = { Text("Filter anchored memories...", color = SoftText, fontSize = 13.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = LightText,
                unfocusedTextColor = SoftText,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = RadiantPurple,
                unfocusedBorderColor = DarkBorder
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (filteredEntries.isEmpty()) {
            // Pristine empty state representation
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📭",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your Mind Space is Silent",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (searchFilter.isNotBlank()) "No memories match your query." else "Anchor your first idea, and your thoughts will populate here.",
                    fontSize = 12.sp,
                    color = SoftText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredEntries, key = { it.id }) { entry ->
                    HistoryCard(entry, onDelete = { viewModel.deleteEntry(entry) })
                }
            }
        }
    }
}

@Composable
fun HistoryCard(entry: JournalEntry, onDelete: () -> Unit) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val formattedDate = remember(entry.timestamp) {
        try {
            val sdf = SimpleDateFormat("LLL dd, yyyy • hh:mm a", Locale.getDefault())
            sdf.format(Date(entry.timestamp))
        } catch (e: Exception) {
            "Anchor Time"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = SoftText
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RadiantPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = entry.companionType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RadiantPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Content excerpt
            Text(
                text = entry.content,
                fontSize = 12.sp,
                color = SoftText,
                lineHeight = 18.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )

            // Dynamic detail drawer showing AI response if saved
            if (entry.aiResponse.isNotBlank()) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = collapseVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "✨", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Anchored Insight:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanGlow
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = entry.aiResponse,
                            fontSize = 11.sp,
                            color = LightText,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Action line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mood tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎯 Mood: ", fontSize = 10.sp, color = SoftText)
                    Text(
                        text = entry.moodVibe,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricPink
                    )
                }

                // Sharing & deletion row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Share icon
                    IconButton(
                        onClick = {
                            val shareBody = """
                                📝 Aura Journal Memory: ${entry.title}
                                Date: $formattedDate
                                Vibe: ${entry.moodVibe}
                                
                                Reflection:
                                ${entry.content}
                                
                                Companion Insight (${entry.companionType}):
                                ${entry.aiResponse}
                            """.trimIndent()

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Aura Memory"))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share entry", color = SoftText, modifier = Modifier.size(16.dp))
                    }

                    // Copy icon
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Aura Entry", entry.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Memory content copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Copy Content", color = SoftText, modifier = Modifier.size(16.dp))
                    }

                    // Delete icon
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete entry", color = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsTabScreen(entries: List<JournalEntry>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visual custom canvas pulse
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AuraPulseChart(entries = entries)
            }
        }

        // Metrics Row
        item {
            Text(
                text = "My Aura Metrics",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Reflections",
                        value = "${entries.size}",
                        subtitle = "Total Anchored",
                        iconEmoji = "🔒",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Favorite Spark",
                        value = remember(entries) {
                            entries.groupBy { it.companionType }
                                .maxByOrNull { it.value.size }?.key ?: "None yet"
                        },
                        subtitle = "Most consulted companion",
                        iconEmoji = "🔮",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Dominant Vibe",
                        value = remember(entries) {
                            entries.groupBy { it.moodVibe }
                                .maxByOrNull { it.value.size }?.key?.split(" ")?.firstOrNull() ?: "None"
                        },
                        subtitle = "Most recorded mood",
                        iconEmoji = "🎯",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Insight Frequency",
                        value = "${entries.count { it.aiResponse.isNotBlank() }}",
                        subtitle = "AI sessions compiled",
                        iconEmoji = "⚡",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Tips and guidance card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Aura Insights Mind Tip",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RadiantPurple
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try consulting multiple companion sparks (like the Zen Master for ease or the Logical Architect for schedules) to get multidimensional creative viewpoints on a single topic.",
                        fontSize = 11.sp,
                        color = SoftText,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    iconEmoji: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.Bold)
                Text(text = iconEmoji, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ElectricPink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = SoftText)
        }
    }
}

@Composable
fun CustomBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        color = CardBackground,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Add,
                label = "Write Reflection",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            BottomNavItem(
                icon = Icons.Default.Star,
                label = "Aura History",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            BottomNavItem(
                icon = Icons.Default.Face,
                label = "Aura Stats",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .minimumInteractiveComponentSize()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) RadiantPurple else SoftText,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) RadiantPurple else SoftText
        )
    }
}

// Simple dynamic spring scale modifier for vibe emoji selection
@Composable
fun Modifier.scaleOnPress(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "emoji_scale"
    )
    return this.then(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}

// Custom flowRow implementation for backwards compatibility if FlowRow is not available in older compose-bom versions
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        androidx.compose.ui.layout.Layout(
            content = content
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
            var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
            var currentRowWidth = 0
            val spacingX = 8.dp.roundToPx()
            val spacingY = 8.dp.roundToPx()

            placeables.forEach { placeable ->
                if (currentRowWidth + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    currentRowWidth = 0
                }
                currentRow.add(placeable)
                currentRowWidth += placeable.width + spacingX
            }
            if (currentRow.isNotEmpty()) {
                rows.add(currentRow)
            }

            val totalHeight = rows.sumOf { row -> row.maxOf { it.height } } + (rows.size - 1) * spacingY
            layout(constraints.maxWidth, totalHeight) {
                var yOffset = 0
                rows.forEach { row ->
                    val rowHeight = row.maxOf { it.height }
                    var xOffset = 0
                    row.forEach { placeable ->
                        placeable.placeRelative(xOffset, yOffset)
                        xOffset += placeable.width + spacingX
                    }
                    yOffset += rowHeight + spacingY
                }
            }
        }
    }
}
