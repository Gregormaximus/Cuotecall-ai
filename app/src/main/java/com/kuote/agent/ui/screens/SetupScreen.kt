package com.kuote.agent.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuote.agent.data.model.CompanyProfile
import com.kuote.agent.data.model.FieldService
import com.kuote.agent.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    profile: CompanyProfile,
    services: List<FieldService>,
    isSmartPricingEnabled: Boolean = false,
    onSaveProfile: (name: String, industry: String, smsTemplate: String, deposit: Double, baseFee: Double, hqAddress: String) -> Unit,
    onAddServiceClick: () -> Unit,
    onDeleteService: (FieldService) -> Unit,
    onUpdateService: (FieldService) -> Unit,
    onToggleSmartPricing: (Boolean) -> Unit,
    onAutoSetup: (String) -> Unit
) {
    val context = LocalContext.current

    // Profile state
    var companyName by remember(profile) { mutableStateOf(profile.name) }
    var industryName by remember(profile) { mutableStateOf(profile.industry) }
    var hqAddressText by remember(profile) { mutableStateOf(profile.hqAddress) }
    var smsTemplate by remember(profile) { mutableStateOf(profile.autoSmsTemplate) }
    var depositText by remember(profile) { mutableStateOf(profile.defaultDeposit.toString()) }
    var baseFeeText by remember(profile) { mutableStateOf(profile.baseServiceFee.toString()) }

    // AI Catalog Intake state
    var intakeInputText by remember { mutableStateOf("") }
    var selectedIntakeMode by remember { mutableStateOf("TEXT") } // "TEXT", "PHOTO", "URL"
    var isProcessingAi by remember { mutableStateOf(false) }

    // Search query
    var searchQuery by remember { mutableStateOf("") }

    val filteredServices = remember(searchQuery, services) {
        if (searchQuery.isBlank()) services
        else services.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.aiKeywords.any { kw -> kw.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QuoteBit Setup",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "LIVE SYNC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Unified profile & AI catalog configuration.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // SECTION 1: Business Profile Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Business Profile Settings",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Company Name
                        Column {
                            Text(
                                text = "BUSINESS NAME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = companyName,
                                onValueChange = { companyName = it },
                                placeholder = { Text("e.g. Skynet One") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }

                        // Industry & HQ Address Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "INDUSTRY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = industryName,
                                    onValueChange = { industryName = it },
                                    placeholder = { Text("Starlink Installation") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "HQ ADDRESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = hqAddressText,
                                    onValueChange = { hqAddressText = it },
                                    placeholder = { Text("Batavia, IL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Auto-Reply SMS Template
                        Column {
                            Text(
                                text = "MISSED-CALL AUTO-REPLY SMS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = smsTemplate,
                                onValueChange = { smsTemplate = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Deposit & Base Fee Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DEFAULT DEPOSIT ($)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = depositText,
                                    onValueChange = { depositText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BASE SERVICE FEE ($)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = baseFeeText,
                                    onValueChange = { baseFeeText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Save Profile Button
                        Button(
                            onClick = {
                                val dep = depositText.toDoubleOrNull() ?: 50.0
                                val base = baseFeeText.toDoubleOrNull() ?: 100.0
                                onSaveProfile(companyName, industryName, smsTemplate, dep, base, hqAddressText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Business Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECTION 2: QuoteBit Smart Setup Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✨ QuoteBit Smart Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            Text(
                                text = "Multimodal AI",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Paste unstructured text, upload handwritten price list photos (📸), or enter pricing URL (🔗). AI will infer trade rules and extract catalog items.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Mode Selector Chips (Text / Photo / URL)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedIntakeMode == "TEXT",
                                onClick = { selectedIntakeMode = "TEXT" },
                                label = { Text("💬 Text Prompt") },
                                leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )

                            FilterChip(
                                selected = selectedIntakeMode == "PHOTO",
                                onClick = {
                                    selectedIntakeMode = "PHOTO"
                                    intakeInputText = "[PHOTO ATTACHED]: Handwritten Starlink price sheet image scanned with Gemini Vision OCR"
                                    Toast.makeText(context, "📸 Handwritten Price List photo loaded!", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("📸 Price Photo") },
                                leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )

                            FilterChip(
                                selected = selectedIntakeMode == "URL",
                                onClick = {
                                    selectedIntakeMode = "URL"
                                    if (intakeInputText.isBlank()) {
                                        intakeInputText = "https://skynet-one.com/pricing"
                                    }
                                },
                                label = { Text("🔗 Pricing URL") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        // ChatGPT-Style Prompt Container
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                OutlinedTextField(
                                    value = intakeInputText,
                                    onValueChange = { intakeInputText = it },
                                    placeholder = {
                                        Text(
                                            when (selectedIntakeMode) {
                                                "PHOTO" -> "📸 Scanned price sheet description or photo notes..."
                                                "URL" -> "🔗 Enter URL (e.g. https://mybusiness.com/rates)..."
                                                else -> "Paste pricing text (e.g. Roof mount installation $200, Custom cable routing $75)..."
                                            },
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            selectedIntakeMode = "PHOTO"
                                            intakeInputText = "[PHOTO ATTACHED]: Handwritten Starlink price sheet image scanned with Gemini Vision OCR"
                                            Toast.makeText(context, "📸 Price List photo attached!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = "Attach Photo", tint = MaterialTheme.colorScheme.primary)
                                        }

                                        IconButton(onClick = {
                                            selectedIntakeMode = "URL"
                                            intakeInputText = "https://skynet-one.com/rates"
                                        }) {
                                            Icon(Icons.Default.Link, contentDescription = "Attach Link", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (intakeInputText.isNotBlank()) {
                                                isProcessingAi = true
                                                onAutoSetup(intakeInputText)
                                                isProcessingAi = false
                                            } else {
                                                Toast.makeText(context, "Please enter text, a photo, or URL first", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !isProcessingAi,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        if (isProcessingAi) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Extract Services")
                                        }
                                    }
                                }
                            }
                        }

                        // Quick Prompt Chips
                        Text("Quick Test Prompts:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Starlink Roof Mount ($200) & Cable Routing ($75)",
                                "Towing ($110 Flatbed, $150 Heavy Duty)",
                                "Plumbing ($125 Drain, $185 Pipe Repair)"
                            ).forEach { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                        .clickable {
                                            intakeInputText = prompt
                                            onAutoSetup(prompt)
                                        }
                                ) {
                                    Text(
                                        text = prompt,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: Extracted Service Catalog Data List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Extracted Service Catalog",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Double-check items, inline edit numbers, or manage rows.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Smart Pricing", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isSmartPricingEnabled,
                            onCheckedChange = onToggleSmartPricing,
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            }

            // Search Bar & Add Button Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search services...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = onAddServiceClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Service")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Row")
                    }
                }
            }

            // List of Extracted Service Cards
            if (filteredServices.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No services extracted yet", fontWeight = FontWeight.Bold)
                            Text("Use the ChatGPT AI Intake box above to parse rates from text, photos, or URLs.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredServices, key = { it.id }) { service ->
                    ServiceCatalogCard(
                        service = service,
                        onDelete = { onDeleteService(service) },
                        onUpdate = onUpdateService
                    )
                }
            }
        }
    }
}
