package com.kuote.agent.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.kuote.agent.ui.theme.*

@Composable
fun RevenueCatPaywallDialog(
    onDismiss: () -> Unit,
    onStartTrial: () -> Unit,
    monthlyPriceText: String = "$9.99/mo"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, FastingoCyanDark, RoundedCornerShape(24.dp)),
            color = FastingoCardBg
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hero Image Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(FastingoSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=800&auto=format&fit=crop",
                            contentDescription = "Service Pro Platform Hero",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient Overlay & Cyan Glow Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(FastingoCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "QuoteCall Pro",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = FastingoTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Elevate your service to AI-speed.",
                        fontSize = 15.sp,
                        color = FastingoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price Banner
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$9.99",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = FastingoTextPrimary
                        )
                        Text(
                            text = " /mo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FastingoTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Features List
                    PaywallFeatureItem(
                        title = "Unlimited Missed-Call Quotes",
                        subtitle = "Never lose a lead when you're under a sink or on a ladder."
                    )

                    PaywallFeatureItem(
                        title = "Voice WebRTC AI Agent",
                        subtitle = "A low-latency virtual assistant that talks to customers in real-time."
                    )

                    PaywallFeatureItem(
                        title = "Multi-Device Firebase Sync",
                        subtitle = "Instant job updates across tablet, phone, and desktop."
                    )

                    PaywallFeatureItem(
                        title = "5% Stripe Success Fee Engine",
                        subtitle = "Lower transaction rates for high-volume service pros."
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Call To Action Button
                    Button(
                        onClick = onStartTrial,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FastingoCyan,
                            contentColor = FastingoBackground
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            text = "Start 7-Day Free Trial",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Cancel anytime. Renews at $monthlyPriceText after trial.",
                        fontSize = 12.sp,
                        color = FastingoTextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Close Button Top Right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FastingoBackground.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Paywall",
                        tint = FastingoTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PaywallFeatureItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = FastingoGreen,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )

        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FastingoTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = FastingoTextSecondary,
                lineHeight = 17.sp
            )
        }
    }
}
