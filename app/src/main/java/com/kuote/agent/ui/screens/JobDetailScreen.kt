package com.kuote.agent.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.kuote.agent.data.model.DispatchGpsData
import androidx.compose.foundation.BorderStroke
import com.kuote.agent.data.model.Job
import com.kuote.agent.data.model.JobStatus
import com.kuote.agent.data.model.SettlementMethod
import com.kuote.agent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    job: Job,
    dispatches: List<DispatchGpsData> = emptyList(),
    onBack: () -> Unit,
    onSettleJobWithSavedCard: (Job) -> Unit,
    onSettleJobDynamicLink: (Job) -> Unit,
    onSettleJobNfc: (Job) -> Unit,
    onSettleJobManualCard: (Job, String, Int, Int, String) -> Unit,
    onSettleJobExternal: (Job, String) -> Unit
) {
    var showSettlementSheet by remember { mutableStateOf(false) }
    var selectedSettlementOption by remember { mutableStateOf(SettlementMethod.SAVED_CARD) }
    var externalPaymentType by remember { mutableStateOf("Zelle") }

    val matchingDispatch = remember(dispatches, job.id) {
        dispatches.find { it.dispatchId == job.id }
    }
    val effectiveLat = job.lat ?: matchingDispatch?.lat
    val effectiveLng = job.lng ?: matchingDispatch?.lng

    // Manual Card Entry State
    var cardNumber by remember { mutableStateOf("4242••••••••4242") }
    var expMonth by remember { mutableStateOf("12") }
    var expYear by remember { mutableStateOf("28") }
    var cvc by remember { mutableStateOf("123") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KuoteBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KuoteCardBackground)
                    .border(1.dp, KuoteBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = KuoteTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ACTIVE JOB #${job.id.takeLast(6).uppercase()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuoteCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = job.serviceTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuoteTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Job Status Card
        Surface(
            color = KuoteCardBackground,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, KuoteBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = KuoteCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = job.customerName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = KuoteTextPrimary
                        )
                    }

                    // Status Badge
                    val (statusBg, statusFg) = when (job.status) {
                        JobStatus.DEPOSIT_PAID -> Pair(Color(0xFF00E5FF).copy(alpha = 0.2f), Color(0xFF00E5FF))
                        JobStatus.IN_PROGRESS -> Pair(Color(0xFFFFB300).copy(alpha = 0.2f), Color(0xFFFFB300))
                        JobStatus.COMPLETED_PAID_STRIPE -> Pair(Color(0xFF00E676).copy(alpha = 0.2f), Color(0xFF00E676))
                        JobStatus.COMPLETED_PAID_EXTERNALLY -> Pair(Color(0xFFB388FF).copy(alpha = 0.2f), Color(0xFFB388FF))
                        else -> Pair(KuoteTextSecondary.copy(alpha = 0.2f), KuoteTextSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = job.status.replace("_", " "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = KuoteTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = job.customerPhone,
                        fontSize = 13.sp,
                        color = KuoteTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = KuoteTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = job.customerLocation,
                        fontSize = 13.sp,
                        color = KuoteTextSecondary
                    )
                }

                job.notes?.let { note ->
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = KuoteBorder)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Job Scope & AI Notes:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KuoteCyan
                    )
                    Text(
                        text = note,
                        fontSize = 13.sp,
                        color = KuoteTextPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Maps Dispatch Preview Card
        GoogleMapPreviewCard(
            lat = effectiveLat,
            lng = effectiveLng,
            customerAddress = job.customerLocation,
            customerName = job.customerName
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Financial & Stripe Connect 2-Step Breakdown Card
        Surface(
            color = KuoteCardBackground,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, KuoteCyanDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "STRIPE CONNECT FINANCIAL SUMMARY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = KuoteCyan,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Estimate Total
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Estimated Job Total",
                        fontSize = 14.sp,
                        color = KuoteTextSecondary
                    )
                    Text(
                        text = "$${String.format("%.2f", job.estimatedTotal)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuoteTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Step 1 Deposit
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Step 1: Upfront Deposit Paid",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KuoteGreen
                        )
                        Text(
                            text = "4.0% Platform Fee ($${String.format("%.2f", job.depositPlatformFee)}) • Tokenized Card Saved",
                            fontSize = 11.sp,
                            color = KuoteTextSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "- $${String.format("%.2f", job.depositAmount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuoteGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = KuoteBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Step 2 Remaining Balance
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Step 2: Remaining Balance Due",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KuoteTextPrimary
                        )
                        Text(
                            text = "Incentive Platform Fee: 1.5% ($${String.format("%.2f", job.balancePlatformFee)})",
                            fontSize = 11.sp,
                            color = KuoteCyan,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$${String.format("%.2f", job.balanceDue)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = KuoteCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Action Button
        if (job.status != JobStatus.COMPLETED_PAID_STRIPE && job.status != JobStatus.COMPLETED_PAID_EXTERNALLY) {
            Button(
                onClick = { showSettlementSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = KuoteGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = KuoteBackground,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "CLOSE JOB & SETTLE BALANCE ($${String.format("%.2f", job.balanceDue)})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = KuoteBackground
                )
            }
        } else {
            // Already settled banner
            Surface(
                color = KuoteGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KuoteGreen, RoundedCornerShape(12.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = KuoteGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Job Completed & Settled in Full",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KuoteGreen
                        )
                        Text(
                            text = "Method: ${job.finalSettlementMethod ?: "Stripe Connect"}",
                            fontSize = 12.sp,
                            color = KuoteTextPrimary
                        )
                    }
                }
            }
        }
    }

    // 5-Option Settlement Modal Bottom Sheet
    if (showSettlementSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettlementSheet = false },
            containerColor = KuoteCardBackground,
            contentColor = KuoteTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "SELECT SETTLEMENT METHOD",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = KuoteCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Balance Due: $${String.format("%.2f", job.balanceDue)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuoteTextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Settlement Option 1: Saved Card (1.5% Fee)
                SettlementOptionTile(
                    title = "1. Charge Saved Card (Stripe Connect)",
                    subtitle = "Card tokenized from deposit (${job.savedPaymentMethodId ?: "pm_saved"}). Discounted 1.5% platform fee.",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedSettlementOption == SettlementMethod.SAVED_CARD,
                    onClick = { selectedSettlementOption = SettlementMethod.SAVED_CARD }
                )

                // Settlement Option 2: Dynamic QR / Link
                SettlementOptionTile(
                    title = "2. Dynamic QR Code / SMS Payment Link",
                    subtitle = "Generates Stripe Checkout URL/QR for customer Apple Pay, Google Pay, or Card.",
                    icon = Icons.Default.QrCode,
                    isSelected = selectedSettlementOption == SettlementMethod.DYNAMIC_QR_LINK,
                    onClick = { selectedSettlementOption = SettlementMethod.DYNAMIC_QR_LINK }
                )

                // Settlement Option 3: Tap to Pay / NFC
                SettlementOptionTile(
                    title = "3. Tap to Pay / NFC Contactless",
                    subtitle = "Accept contactless card or phone wallet tap directly on your NFC device.",
                    icon = Icons.Default.Nfc,
                    isSelected = selectedSettlementOption == SettlementMethod.TAP_TO_PAY_NFC,
                    onClick = { selectedSettlementOption = SettlementMethod.TAP_TO_PAY_NFC }
                )

                // Settlement Option 4: Manual Keyed-In Card
                SettlementOptionTile(
                    title = "4. Manual Card Entry (Keyed-In)",
                    subtitle = "Type in customer credit card numbers directly in-app.",
                    icon = Icons.Default.Pin,
                    isSelected = selectedSettlementOption == SettlementMethod.MANUAL_KEYED_CARD,
                    onClick = { selectedSettlementOption = SettlementMethod.MANUAL_KEYED_CARD }
                )

                // Settlement Option 5: Mark Paid Externally
                SettlementOptionTile(
                    title = "5. Mark Paid Externally (Cash, Zelle, Venmo)",
                    subtitle = "Bypasses Stripe charges. Dispatches instant digital SMS receipt.",
                    icon = Icons.Default.LocalAtm,
                    isSelected = selectedSettlementOption == SettlementMethod.EXTERNAL_CASH_VENMO_CHECK,
                    onClick = { selectedSettlementOption = SettlementMethod.EXTERNAL_CASH_VENMO_CHECK }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Detail configuration view based on selected settlement option
                when (selectedSettlementOption) {
                    SettlementMethod.MANUAL_KEYED_CARD -> {
                        Surface(
                            color = KuoteBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, KuoteBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                OutlinedTextField(
                                    value = cardNumber,
                                    onValueChange = { cardNumber = it },
                                    label = { Text("Card Number", color = KuoteTextSecondary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = KuoteCyan,
                                        unfocusedBorderColor = KuoteBorder,
                                        focusedTextColor = KuoteTextPrimary,
                                        unfocusedTextColor = KuoteTextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    OutlinedTextField(
                                        value = expMonth,
                                        onValueChange = { expMonth = it },
                                        label = { Text("Exp Month", color = KuoteTextSecondary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = KuoteCyan,
                                            unfocusedBorderColor = KuoteBorder,
                                            focusedTextColor = KuoteTextPrimary,
                                            unfocusedTextColor = KuoteTextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = expYear,
                                        onValueChange = { expYear = it },
                                        label = { Text("Exp Year", color = KuoteTextSecondary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = KuoteCyan,
                                            unfocusedBorderColor = KuoteBorder,
                                            focusedTextColor = KuoteTextPrimary,
                                            unfocusedTextColor = KuoteTextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = cvc,
                                        onValueChange = { cvc = it },
                                        label = { Text("CVC", color = KuoteTextSecondary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = KuoteCyan,
                                            unfocusedBorderColor = KuoteBorder,
                                            focusedTextColor = KuoteTextPrimary,
                                            unfocusedTextColor = KuoteTextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    SettlementMethod.EXTERNAL_CASH_VENMO_CHECK -> {
                        Column {
                            Text(
                                text = "Select External Medium:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KuoteTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Cash", "Zelle", "Venmo", "Check").forEach { type ->
                                    val isSel = externalPaymentType == type
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { externalPaymentType = type },
                                        label = { Text(type, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = KuoteCyan,
                                            selectedLabelColor = KuoteBackground,
                                            containerColor = KuoteBackground,
                                            labelColor = KuoteTextPrimary
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Final Action Trigger Button
                Button(
                    onClick = {
                        showSettlementSheet = false
                        when (selectedSettlementOption) {
                            SettlementMethod.SAVED_CARD -> onSettleJobWithSavedCard(job)
                            SettlementMethod.DYNAMIC_QR_LINK -> onSettleJobDynamicLink(job)
                            SettlementMethod.TAP_TO_PAY_NFC -> onSettleJobNfc(job)
                            SettlementMethod.MANUAL_KEYED_CARD -> onSettleJobManualCard(
                                job,
                                cardNumber,
                                expMonth.toIntOrNull() ?: 12,
                                expYear.toIntOrNull() ?: 28,
                                cvc
                            )
                            SettlementMethod.EXTERNAL_CASH_VENMO_CHECK -> onSettleJobExternal(job, externalPaymentType)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KuoteGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "CONFIRM & SETTLE $${String.format("%.2f", job.balanceDue)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuoteBackground
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SettlementOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) KuoteCyanDark.copy(alpha = 0.3f) else KuoteBackground,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) KuoteCyan else KuoteBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) KuoteCyan else KuoteCardBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) KuoteBackground else KuoteCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuoteTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = KuoteTextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun GoogleMapPreviewCard(
    lat: Double?,
    lng: Double?,
    customerAddress: String,
    customerName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasGps = lat != null && lng != null && lat != 0.0 && lng != 0.0

    Surface(
        color = KuoteCardBackground,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (hasGps) Color(0xFF0EA5E9) else KuoteBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map Preview",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "GOOGLE MAPS DISPATCH PREVIEW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                if (hasGps) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Text(
                                text = "GPS Pin Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (hasGps) {
                // Map Container with Canvas road grid & center marker pin
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .clickable {
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(customerName)})")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                ) {
                    // Map background layout with gridlines & satellite glow
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Grid background simulating map roads
                        val gridStep = 40.dp.toPx()
                        var x = 0f
                        while (x < width) {
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = androidx.compose.ui.geometry.Offset(x, 0f),
                                end = androidx.compose.ui.geometry.Offset(x, height),
                                strokeWidth = 1f
                            )
                            x += gridStep
                        }
                        var y = 0f
                        while (y < height) {
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(width, y),
                                strokeWidth = 1f
                            )
                            y += gridStep
                        }

                        // Simulated road paths
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, height * 0.4f)
                                cubicTo(width * 0.3f, height * 0.2f, width * 0.6f, height * 0.7f, width, height * 0.5f)
                            },
                            color = Color(0xFF334155),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                        )
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(width * 0.5f, 0f)
                                lineTo(width * 0.5f, height)
                            },
                            color = Color(0xFF0284C7).copy(alpha = 0.4f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                        )

                        // Radar pulse circle around marker center
                        drawCircle(
                            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                            radius = 50.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
                        )
                        drawCircle(
                            color = Color(0xFF38BDF8).copy(alpha = 0.3f),
                            radius = 28.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
                        )
                    }

                    // Center Marker Pin overlay
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFEF4444),
                            shadowElevation = 6.dp,
                            border = BorderStroke(2.dp, Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Pin",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = customerName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(12.dp)
                                .background(Color(0xFFEF4444))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    // Top Left Lat/Lng badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "Lat: $lat, Lng: $lng",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Bottom Right Watermark
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0284C7),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Maps Preview",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigate to Customer Button (Intent Action)
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(customerName)})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                            )
                            context.startActivity(browserIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Navigate",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Navigate to Customer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

            } else {
                // FALLBACK STATE: "No GPS shared - address provided: [Address]"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = "No GPS",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "No GPS shared - address provided: ${customerAddress.ifBlank { "Address pending" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF1F5F9),
                                lineHeight = 16.sp
                            )
                        }

                        if (customerAddress.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(customerAddress)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Address",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Search Address in Google Maps",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

