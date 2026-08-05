package com.kuote.agent.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kuote.agent.ui.theme.*

@Composable
fun AddServiceDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, category: String, basePrice: Double, ratePerMile: Double, keywords: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("GENERAL") }
    var basePriceText by remember { mutableStateOf("95.00") }
    var ratePerMileText by remember { mutableStateOf("3.50") }
    var keywordsText by remember { mutableStateOf("Emergency, Onsite, Repair") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, FastingoBorder, RoundedCornerShape(20.dp)),
            color = FastingoCardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Add New Service",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FastingoTextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name (e.g. Battery Jumpstart)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FastingoCyan,
                        unfocusedBorderColor = FastingoBorder,
                        focusedLabelColor = FastingoCyan
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = basePriceText,
                        onValueChange = { basePriceText = it },
                        label = { Text("Base Price ($)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FastingoCyan,
                            unfocusedBorderColor = FastingoBorder
                        )
                    )

                    OutlinedTextField(
                        value = ratePerMileText,
                        onValueChange = { ratePerMileText = it },
                        label = { Text("Rate/Mile ($)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FastingoCyan,
                            unfocusedBorderColor = FastingoBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = { Text("AI Keywords (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FastingoCyan,
                        unfocusedBorderColor = FastingoBorder
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = FastingoTextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val base = basePriceText.toDoubleOrNull() ?: 50.0
                            val rate = ratePerMileText.toDoubleOrNull() ?: 0.0
                            onAdd(
                                name.ifBlank { "Custom Service" },
                                category,
                                base,
                                rate,
                                keywordsText
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FastingoCyan,
                            contentColor = FastingoBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to Catalog", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
