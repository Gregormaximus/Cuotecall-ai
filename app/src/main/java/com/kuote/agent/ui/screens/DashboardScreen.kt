package com.kuote.agent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.kuote.agent.data.model.ConversationLog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuote.agent.data.model.AnalyticsStats
import com.kuote.agent.data.model.DayActivity
import com.kuote.agent.data.model.Job
import com.kuote.agent.data.model.JobStatus
import com.kuote.agent.data.model.MonthRevenue
import com.kuote.agent.data.model.Quote

@Composable
fun DashboardScreen(
    quotes: List<Quote>,
    jobs: List<Job>,
    analyticsStats: AnalyticsStats = AnalyticsStats(),
    recentConversations: List<ConversationLog> = emptyList(),
    onApproveQuote: (Quote) -> Unit,
    onDeclineQuote: (Quote) -> Unit,
    onSelectJob: (Job) -> Unit,
    onSimulateMissedCall: () -> Unit,
    onRecordSiteClick: () -> Unit = {}
) {
    var selectedChartMetric by remember { mutableIntStateOf(0) } // 0: Calls & SMS, 1: Site Clicks, 2: Deposits ($)
    var selectedDayIndex by remember { mutableStateOf<Int?>(4) } // Default to Friday
    var selectedMonthIndex by remember { mutableStateOf<Int?>(analyticsStats.monthlyRevenueTrend.lastIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Firestore Live Sync Banner
            item {
                FirestoreSyncHeader()
            }

            // 2. Real-time Analytics Cards Grid (4 Cards)
            item {
                Text(
                    text = "REAL-TIME ANALYTICS CARDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Missed Calls Handled & Auto-SMS Sent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsCard(
                            title = "MISSED CALLS HANDLED",
                            value = "${analyticsStats.missedCallsHandled}",
                            subtitle = "AI Agent Answered",
                            badge = "+12% wk",
                            icon = Icons.Default.PhoneMissed,
                            accentColor = Color(0xFF00E5FF),
                            modifier = Modifier.weight(1f)
                        )

                        AnalyticsCard(
                            title = "AUTO-SMS SENT",
                            value = "${analyticsStats.autoSmsSent}",
                            subtitle = "Instant Quote Links",
                            badge = "100% Sent",
                            icon = Icons.Default.Send,
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Micro-Site Clicks & CTR % + Deposits Collected
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsCard(
                            title = "SITE CLICKS & CTR %",
                            value = "${analyticsStats.microSiteClicks} clicks",
                            subtitle = "${String.format("%.1f", analyticsStats.ctrPercentage)}% CTR",
                            badge = "Click Link",
                            icon = Icons.Default.TouchApp,
                            accentColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f),
                            onCardClick = onRecordSiteClick
                        )

                        AnalyticsCard(
                            title = "TOTAL DEPOSITS ($)",
                            value = "$${String.format("%.2f", analyticsStats.totalDepositsCollected)}",
                            subtitle = "Secured via Stripe",
                            badge = "Stripe Live",
                            icon = Icons.Default.Payments,
                            accentColor = Color(0xFFA855F7),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2b. Recent Conversations Log (Live Firestore Stream)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                RecentConversationsSection(conversations = recentConversations)
            }

            // 3. Monthly Revenue Trend Chart (NEW FEATURE - Interactive Stripe Income Trend)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                MonthlyRevenueTrendChart(
                    monthlyTrend = analyticsStats.monthlyRevenueTrend,
                    momGrowthPercentage = analyticsStats.momGrowthPercentage,
                    selectedMonthIndex = selectedMonthIndex,
                    onSelectMonth = { selectedMonthIndex = if (selectedMonthIndex == it) null else it }
                )
            }

            // 4. Selected Month Revenue Breakdown Popover Card
            if (selectedMonthIndex != null && selectedMonthIndex!! in analyticsStats.monthlyRevenueTrend.indices) {
                val monthData = analyticsStats.monthlyRevenueTrend[selectedMonthIndex!!]
                item {
                    MonthRevenueDetailCard(monthData = monthData)
                }
            }

            // 5. Weekly Overview Bar Chart
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WEEKLY OVERVIEW (LAST 7 DAYS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedChartMetric == 0,
                            onClick = { selectedChartMetric = 0 },
                            label = { Text("Calls/SMS", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = selectedChartMetric == 1,
                            onClick = { selectedChartMetric = 1 },
                            label = { Text("Clicks", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = selectedChartMetric == 2,
                            onClick = { selectedChartMetric = 2 },
                            label = { Text("Deposits", fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                WeeklyBarChart(
                    weeklyOverview = analyticsStats.weeklyOverview,
                    selectedMetric = selectedChartMetric,
                    selectedDayIndex = selectedDayIndex,
                    onSelectDay = { selectedDayIndex = if (selectedDayIndex == it) null else it }
                )
            }

            // 6. Selected Day Detail Popover
            if (selectedDayIndex != null && selectedDayIndex!! in analyticsStats.weeklyOverview.indices) {
                val dayData = analyticsStats.weeklyOverview[selectedDayIndex!!]
                item {
                    DayDetailCard(dayData = dayData)
                }
            }

            // 7. Active Jobs Section
            if (jobs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ACTIVE JOBS & BALANCE SETTLEMENTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                items(jobs) { job ->
                    JobSummaryCard(
                        job = job,
                        onClick = { onSelectJob(job) }
                    )
                }
            }

            // 8. AI Quotes Feed Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LIVE AI MISSED CALL QUOTES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            items(quotes) { quote ->
                CaughtCallCard(
                    quote = quote,
                    onApprove = { onApproveQuote(quote) },
                    onDecline = { onDeclineQuote(quote) }
                )
            }
        }

        // Simulate Call FAB
        FloatingActionButton(
            onClick = onSimulateMissedCall,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Simulate Missed Call", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/**
 * Monthly Revenue Trend Chart Component (Stripe Income Over Time)
 */
@Composable
fun MonthlyRevenueTrendChart(
    monthlyTrend: List<MonthRevenue>,
    momGrowthPercentage: Double,
    selectedMonthIndex: Int?,
    onSelectMonth: (Int) -> Unit
) {
    var isFullYearView by remember { mutableStateOf(false) } // Toggle 6M vs 12M
    val displayTrend = remember(monthlyTrend, isFullYearView) {
        if (!isFullYearView && monthlyTrend.size > 6) {
            monthlyTrend.takeLast(6)
        } else {
            monthlyTrend
        }
    }

    val totalPeriodRevenue = remember(displayTrend) {
        displayTrend.sumOf { it.totalRevenue }
    }

    val latestMonthRevenue = remember(displayTrend) {
        displayTrend.lastOrNull()?.totalRevenue ?: 0.0
    }

    val maxRevenue = remember(displayTrend) {
        (displayTrend.maxOfOrNull { it.totalRevenue } ?: 1000.0).coerceAtLeast(1.0)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Title & Time Period Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MONTHLY REVENUE TREND",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Stripe Connect Direct Deposits",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = !isFullYearView,
                        onClick = { isFullYearView = false },
                        label = { Text("6M", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = isFullYearView,
                        onClick = { isFullYearView = true },
                        label = { Text("12M", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Revenue Metric Display with Currency ($) & Growth Indicator Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current Month Revenue",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = String.format("%,.2f", latestMonthRevenue),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Month-over-Month Growth Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+${String.format("%.1f", momGrowthPercentage)}% vs last mo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Line Chart Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val lineColor = Color(0xFF00E5FF)
                val fillGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.45f),
                        Color(0xFF10B981).copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(displayTrend) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (displayTrend.size.coerceAtLeast(1))
                                val tappedIdx = (offset.x / stepX).toInt().coerceIn(0, displayTrend.lastIndex)
                                val originalIndex = monthlyTrend.indexOf(displayTrend[tappedIdx])
                                if (originalIndex != -1) {
                                    onSelectMonth(originalIndex)
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height - 24.dp.toPx() // leave room at bottom for month labels
                    val count = displayTrend.size
                    if (count == 0) return@Canvas

                    val stepX = width / count

                    // Draw 3 Horizontal Grid Lines
                    for (i in 0..3) {
                        val y = height * (i / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Calculate point positions
                    val points = displayTrend.mapIndexed { idx, item ->
                        val x = stepX * idx + (stepX / 2)
                        val normY = (item.totalRevenue / maxRevenue).toFloat().coerceIn(0.05f, 0.95f)
                        val y = height * (1f - normY)
                        Offset(x, y)
                    }

                    // Build line path & area path
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val p1 = points[i - 1]
                            val p2 = points[i]
                            val cx = (p1.x + p2.x) / 2
                            cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                        }
                    }

                    val areaPath = Path().apply {
                        moveTo(points.first().x, height)
                        lineTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val p1 = points[i - 1]
                            val p2 = points[i]
                            val cx = (p1.x + p2.x) / 2
                            cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                        }
                        lineTo(points.last().x, height)
                        close()
                    }

                    // Draw area gradient fill
                    drawPath(path = areaPath, brush = fillGradient)

                    // Draw line path
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw data point circles & selected highlight glow
                    points.forEachIndexed { idx, point ->
                        val item = displayTrend[idx]
                        val originalIdx = monthlyTrend.indexOf(item)
                        val isSelected = selectedMonthIndex == originalIdx

                        if (isSelected) {
                            // Glowing Outer Ring
                            drawCircle(
                                color = lineColor.copy(alpha = 0.35f),
                                radius = 14.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 8.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 5.dp.toPx(),
                                center = point
                            )
                        } else {
                            drawCircle(
                                color = Color(0xFF121A21),
                                radius = 6.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }

                // Month Labels along X-Axis below Canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    displayTrend.forEach { item ->
                        val originalIdx = monthlyTrend.indexOf(item)
                        val isSelected = selectedMonthIndex == originalIdx
                        Text(
                            text = item.monthLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onSelectMonth(originalIdx) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total Period Summary Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Total Period Revenue (${displayTrend.size} Mos):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$${String.format("%,.2f", totalPeriodRevenue)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Selected Month Detailed Revenue Breakdown Card
 */
@Composable
fun MonthRevenueDetailCard(monthData: MonthRevenue) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        color = Color(0xFF00E5FF).copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${monthData.monthLabel} Revenue Breakdown",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${monthData.totalJobsCount} Jobs Completed",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailStatItem("Total Revenue", "$${String.format("%,.2f", monthData.totalRevenue)}")
                DetailStatItem("Deposits Collected", "$${String.format("%,.2f", monthData.depositRevenue)}")
                DetailStatItem("Avg / Job", "$${String.format("%.2f", if (monthData.totalJobsCount > 0) monthData.totalRevenue / monthData.totalJobsCount else 0.0)}")
            }
        }
    }
}

@Composable
fun FirestoreSyncHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Firestore Live Sync Active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Collection: users/{userId}/stats/analytics",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = "Firestore Synced",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AnalyticsCard(
    title: String,
    value: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .then(if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
        }
    }
}

@Composable
fun WeeklyBarChart(
    weeklyOverview: List<DayActivity>,
    selectedMetric: Int, // 0: Calls & SMS, 1: Site Clicks, 2: Deposits ($)
    selectedDayIndex: Int?,
    onSelectDay: (Int) -> Unit
) {
    val maxValue = remember(weeklyOverview, selectedMetric) {
        when (selectedMetric) {
            0 -> weeklyOverview.maxOfOrNull { maxOf(it.missedCalls, it.smsSent) }?.toDouble() ?: 1.0
            1 -> weeklyOverview.maxOfOrNull { it.siteClicks }?.toDouble() ?: 1.0
            else -> weeklyOverview.maxOfOrNull { it.depositsAmount } ?: 1.0
        }.coerceAtLeast(1.0)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (selectedMetric) {
                            0 -> "Missed Calls & Auto-SMS Activity"
                            1 -> "Micro-Site Visitor Traffic"
                            else -> "Daily Deposits Collected ($)"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Tap bar for details",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bar Chart Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyOverview.forEachIndexed { index, day ->
                    val isSelected = selectedDayIndex == index
                    val valToDisplay = when (selectedMetric) {
                        0 -> day.missedCalls.toDouble()
                        1 -> day.siteClicks.toDouble()
                        else -> day.depositsAmount
                    }

                    val heightFactor = (valToDisplay / maxValue).toFloat().coerceIn(0.1f, 1f)

                    val barColor = when (selectedMetric) {
                        0 -> if (isSelected) Color(0xFF00E5FF) else Color(0xFF00B4D8).copy(alpha = 0.5f)
                        1 -> if (isSelected) Color(0xFFF59E0B) else Color(0xFFF59E0B).copy(alpha = 0.5f)
                        else -> if (isSelected) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.5f)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectDay(index) }
                            .padding(horizontal = 2.dp)
                    ) {
                        // Value label above bar
                        Text(
                            text = if (selectedMetric == 2) "$${valToDisplay.toInt()}" else "${valToDisplay.toInt()}",
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bar Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .fillMaxHeight(heightFactor)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            barColor,
                                            barColor.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    ) else Modifier
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day label
                        Text(
                            text = day.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayDetailCard(dayData: DayActivity) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "${dayData.dayLabel} Activity Breakdown",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailStatItem("Calls Handled", "${dayData.missedCalls}")
                DetailStatItem("SMS Sent", "${dayData.smsSent}")
                DetailStatItem("Site Clicks", "${dayData.siteClicks}")
                DetailStatItem("Deposits", "$${dayData.depositsAmount.toInt()}")
            }
        }
    }
}

@Composable
fun DetailStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun JobSummaryCard(job: Job, onClick: () -> Unit) {
    val isSettled = job.status == JobStatus.COMPLETED_PAID_STRIPE || job.status == JobStatus.COMPLETED_PAID_EXTERNALLY

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = job.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = job.serviceTitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val statusBg = if (isSettled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                val statusFg = if (isSettled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = job.status.replace("_", " "),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Balance Due",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", job.balanceDue)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSettled) "Receipt" else "Settle Job",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CaughtCallCard(
    quote: Quote,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name/Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Customer: ${quote.customerPhone}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = quote.timeAgoText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge(containerColor = if (quote.status == "PENDING_APPROVAL") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondary) {
                    Text(text = quote.status, color = if (quote.status == "PENDING_APPROVAL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondary, modifier = Modifier.padding(4.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = quote.aiSummary, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))

            // Financials
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialItem(label = "Estimate", value = "$${quote.estimatedTotal}")
                FinancialItem(label = "Deposit", value = "$${quote.requiredDeposit}")
                FinancialItem(label = "Fee (4%)", value = "$${String.format("%.2f", quote.estimatedTotal * 0.04)}")
            }

            Spacer(modifier = Modifier.height(12.dp))
            // Actions
            if (quote.status == "PENDING_APPROVAL") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) { Text("Decline") }
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) { Text("Approve & Deposit", color = MaterialTheme.colorScheme.onPrimary) }
                }
            }
        }
    }
}

@Composable
fun FinancialItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun RecentConversationsSection(conversations: List<ConversationLog>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Conversations",
                        tint = Color(0xFF06B6D4),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "RECENT CONVERSATIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF082F49),
                    border = BorderStroke(1.dp, Color(0xFF0E7490))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF06B6D4))
                        )
                        Text(
                            text = "conversations (${conversations.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22D3EE)
                        )
                    }
                }
            }

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent conversations logged yet.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                conversations.take(6).forEach { log ->
                    ConversationLogCard(log = log)
                }
            }
        }
    }
}

@Composable
fun ConversationLogCard(log: ConversationLog) {
    val (statusLabel, statusBg, statusText, statusBorder) = when (log.status) {
        "LOCATION_DISPATCHED" -> Quadruple("📍 Location Dispatched", Color(0xFF082F49), Color(0xFF22D3EE), Color(0xFF0E7490))
        "DEPOSIT_PAID" -> Quadruple("💳 Deposit Paid ($${String.format("%.0f", log.depositAmount)})", Color(0xFF064E3B), Color(0xFF34D399), Color(0xFF059669))
        "CLIENT_REPLIED" -> Quadruple("💬 Client Replied", Color(0xFF451A03), Color(0xFFFBBF24), Color(0xFFB45309))
        else -> Quadruple("📱 Auto-SMS Sent", Color(0xFF1E1B4B), Color(0xFF818CF8), Color(0xFF4338CA))
    }

    val formattedTime = remember(log.timestamp) {
        val diffMinutes = (System.currentTimeMillis() - log.timestamp) / (1000 * 60)
        when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
            else -> "${diffMinutes / 1440}d ago"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF020617),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneCallback,
                        contentDescription = "Caller",
                        tint = Color(0xFF06B6D4),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${log.customerName} (${log.customerPhone})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }

            // SMS Preview Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Text(
                    text = log.lastSmsText,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(10.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusBorder)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (log.gpsLocation != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF082F49),
                            border = BorderStroke(1.dp, Color(0xFF0891B2))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "GPS",
                                    tint = Color(0xFF22D3EE),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = log.gpsLocation,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22D3EE)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Quote $${String.format("%.0f", log.generatedQuoteAmount)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
