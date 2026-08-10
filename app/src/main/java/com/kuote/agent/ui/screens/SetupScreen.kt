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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.kuote.agent.data.model.BrandingChatMessage
import com.kuote.agent.data.model.CompanyProfile
import com.kuote.agent.data.model.CompanyWebConfig
import com.kuote.agent.data.model.FieldService
import com.kuote.agent.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    profile: CompanyProfile,
    services: List<FieldService>,
    webConfig: CompanyWebConfig = CompanyWebConfig(),
    brandingChatMessages: List<BrandingChatMessage> = emptyList(),
    isProcessingBrandingChat: Boolean = false,
    isSmartPricingEnabled: Boolean = false,
    onSaveProfile: (name: String, industry: String, smsTemplate: String, deposit: Double, baseFee: Double, hqAddress: String) -> Unit,
    onSendBrandingChatMessage: (input: String, attachmentType: String?) -> Unit = { _, _ -> },
    onAddServiceClick: () -> Unit,
    onDeleteService: (FieldService) -> Unit,
    onUpdateService: (FieldService) -> Unit,
    onToggleSmartPricing: (Boolean) -> Unit,
    onAutoSetup: suspend (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Profile state
    var companyName by remember(profile) { mutableStateOf(profile.name) }
    var industryName by remember(profile) { mutableStateOf(profile.industry) }
    var hqAddressText by remember(profile) { mutableStateOf(profile.hqAddress) }
    var smsTemplate by remember(profile) { mutableStateOf(profile.autoSmsTemplate) }
    var depositText by remember(profile) { mutableStateOf(profile.defaultDeposit.toString()) }
    var baseFeeText by remember(profile) { mutableStateOf(profile.baseServiceFee.toString()) }

    // Chat Studio State
    var chatInputText by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Chat Studio, 1: Manual Profile Settings

    // Search query for catalog
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
                            text = "Branding & Catalog Studio",
                            fontSize = 26.sp,
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
                                    text = "LIVE FIRESTORE SYNC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Conversational AI branding extraction for ${profile.name}.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab selector (Chat Studio vs Form Settings)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = 0 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "💬 Chat Branding Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (activeTab == 0) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = 1 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "⚙️ Manual Profile Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (activeTab == 1) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (activeTab == 0) {
                // SECTION 1: Conversational (Chat-Driven) Branding Studio
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                        color = MaterialTheme.colorScheme.surface
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
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ChatBrandingStudio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Gemini 1.5 Multimodal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Paste website URLs, upload photos/documents (brochures, business cards, logos), or type commands to customize branding & services.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Chat Feed Display
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                LazyColumn(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(brandingChatMessages) { msg ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (msg.sender == "USER") Arrangement.End else Arrangement.Start
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (msg.sender == "USER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.widthIn(max = 280.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    if (msg.attachmentType != null) {
                                                        Text(
                                                            "📎 Attachment: ${msg.attachmentType}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (msg.sender == "USER") Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                    }
                                                    Text(
                                                        text = msg.messageText,
                                                        fontSize = 12.sp,
                                                        color = if (msg.sender == "USER") Color.Black else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Quick Action Attachment Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        chatInputText = "[PHOTO ATTACHED]: Business card & Starlink installation brochure image scanned"
                                        onSendBrandingChatMessage(chatInputText, "PHOTO")
                                        chatInputText = ""
                                    },
                                    label = { Text("📸 Brochure Photo") },
                                    leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )

                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        chatInputText = "https://skynet-one.com"
                                        onSendBrandingChatMessage(chatInputText, "URL")
                                        chatInputText = ""
                                    },
                                    label = { Text("🔗 Website URL") },
                                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )

                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        chatInputText = "[DOCUMENT ATTACHED]: Official Starlink Roof Mount & Fiber Routing Price Sheet PDF"
                                        onSendBrandingChatMessage(chatInputText, "DOCUMENT")
                                        chatInputText = ""
                                    },
                                    label = { Text("📄 PDF / Business Card") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            // Chat Text Input Field
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = chatInputText,
                                    onValueChange = { chatInputText = it },
                                    placeholder = { Text("Type command (e.g. Set theme color to #00E5FF for Skynet One)...", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (chatInputText.isNotBlank()) {
                                            onSendBrandingChatMessage(chatInputText, null)
                                            chatInputText = ""
                                        }
                                    },
                                    enabled = !isProcessingBrandingChat,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    if (isProcessingBrandingChat) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                                    } else {
                                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp), tint = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: Immediate Live Preview Card (Microsite Real-time Preview)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
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
                                Text("📱 Real-Time Microsite Live Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                                ) {
                                    Text("slug: ${profile.name.lowercase().replace(" ", "-")}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            // Live Mobile Card Simulation
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp)),
                                color = Color(0xFF0B132B)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Header Logo & Business Name
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AsyncImage(
                                            model = profile.logoUrl,
                                            contentDescription = "Logo Preview",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, Color(0xFF00E5FF), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column {
                                            Text(
                                                text = profile.name,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Text(
                                                text = profile.industry,
                                                fontSize = 11.sp,
                                                color = Color(0xFF00E5FF)
                                            )
                                        }
                                    }

                                    Text(
                                        text = webConfig.siteSubtitle,
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )

                                    // Voice Assistant Action Button Preview
                                    Button(
                                        onClick = { },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(webConfig.voiceCallButtonText, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    // Service Catalog Tags Preview
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        services.take(4).forEach { service ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color.White.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    text = "${service.name} ($${service.basePrice.toInt()})",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // SECTION 1 (Manual Profile Form Settings)
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
