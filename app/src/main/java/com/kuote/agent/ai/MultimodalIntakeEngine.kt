package com.kuote.agent.ai

import android.util.Base64
import com.kuote.agent.data.model.CompanyProfile
import com.kuote.agent.data.model.FieldService
import com.kuote.agent.data.model.Quote
import com.kuote.agent.data.remote.GeminiApiClient
import com.kuote.agent.data.remote.GeminiContent
import com.kuote.agent.data.remote.GeminiPart
import com.kuote.agent.data.remote.GeminiRequest
import com.kuote.agent.data.remote.InlineData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class GeminiQuoteOutput(
    val service_name: String,
    val customer_summary: String,
    val estimated_total: Double,
    val required_deposit: Double,
    val platform_fee: Double,
    val action_required: String = "SEND_STRIPE_PAYMENT_LINK"
)

data class ExtractedServiceItem(
    val name: String,
    val price: Double,
    val description: String = ""
)

class MultimodalIntakeEngine {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(GeminiQuoteOutput::class.java)

    suspend fun analyzeCustomerRequest(
        customerPhone: String,
        location: String = "Local Service Area",
        textInput: String?,
        imageBase64: String? = null,
        audioBase64: String? = null,
        companyProfile: CompanyProfile,
        services: List<FieldService>
    ): Quote = withContext(Dispatchers.IO) {

        val apiKey = GeminiApiClient.getApiKey()
        
        val catalogDescription = services.joinToString("\n") { s ->
            "- ${s.name} (${s.category}): Base Price $${s.basePrice}, Rate/Mile $${s.ratePerMile}. AI Keywords: ${s.aiKeywords.joinToString(", ")}"
        }

        val systemPromptText = """
            You are "Kuote Field-Agent AI", an automated multi-industry customer service & instant quotation engine for field services.
            Company Name: ${companyProfile.name}
            Industry: ${companyProfile.industry}
            Base Service Fee: $${companyProfile.baseServiceFee}
            Default Required Deposit: $${companyProfile.defaultDeposit}
            
            ACTIVE SERVICE CATALOG:
            $catalogDescription
            
            CORE INSTRUCTIONS:
            1. Extract relevant job details from text, photos (Vision), or voice audio.
            2. Match against catalog prices. If no catalog match fits, estimate fairly starting from the Base Service Fee ($${companyProfile.baseServiceFee}).
            3. Deposit is strictly $${companyProfile.defaultDeposit}. Platform fee is 5% of deposit or $2.50 ($2.50 minimum).
            4. Respond in JSON format with these EXACT fields:
            {
              "service_name": "<Detected Service>",
              "customer_summary": "<Concise summary of issue/job>",
              "estimated_total": 150.00,
              "required_deposit": 50.00,
              "platform_fee": 2.50,
              "action_required": "SEND_STRIPE_PAYMENT_LINK"
            }
        """.trimIndent()

        val parts = mutableListOf<GeminiPart>()
        if (!textInput.isNullOrBlank()) {
            parts.add(GeminiPart(text = "Customer Text Note: $textInput"))
        }
        if (!imageBase64.isNullOrBlank()) {
            parts.add(GeminiPart(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64)))
            parts.add(GeminiPart(text = "Inspect job photo for damage, size, or required equipment."))
        }
        if (!audioBase64.isNullOrBlank()) {
            parts.add(GeminiPart(inlineData = InlineData(mimeType = "audio/mp3", data = audioBase64)))
            parts.add(GeminiPart(text = "Parse voice audio note for customer service details."))
        }
        if (parts.isEmpty()) {
            parts.add(GeminiPart(text = "Customer requested service quote for ${companyProfile.industry}."))
        }

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = parts)),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPromptText)))
        )

        val quoteId = "q_" + UUID.randomUUID().toString().take(8)

        if (apiKey.isNotBlank()) {
            try {
                val response = GeminiApiClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                val parsed = parseJson(responseText)
                if (parsed != null) {
                    val stripeLink = "https://checkout.stripe.com/pay/${companyProfile.stripeAccountId}?quote=$quoteId&amt=${parsed.required_deposit}"
                    return@withContext Quote(
                        id = quoteId,
                        customerPhone = customerPhone,
                        customerLocation = location,
                        timeAgoText = "Just now",
                        timestampMillis = System.currentTimeMillis(),
                        serviceCategory = companyProfile.industry.uppercase(),
                        audioUrl = if (!audioBase64.isNullOrBlank()) "audio_note_sample.mp3" else null,
                        audioDurationText = if (!audioBase64.isNullOrBlank()) "0:14" else null,
                        photoUrl = if (!imageBase64.isNullOrBlank()) "photo_job_sample.jpg" else null,
                        aiSummary = "Detected: ${parsed.service_name}. ${parsed.customer_summary}",
                        estimatedTotal = parsed.estimated_total,
                        requiredDeposit = parsed.required_deposit,
                        platformFee = parsed.platform_fee,
                        status = "PENDING_APPROVAL",
                        stripePaymentLink = stripeLink,
                        rawCustomerNote = textInput
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback intelligent quote generation (works offline or without key)
        val matchedService = services.firstOrNull { s ->
            textInput?.contains(s.name, ignoreCase = true) == true || s.aiKeywords.any { kw -> textInput?.contains(kw, ignoreCase = true) == true }
        } ?: services.firstOrNull()

        val serviceName = matchedService?.name ?: "${companyProfile.industry} Service"
        val totalEst = (matchedService?.basePrice ?: companyProfile.baseServiceFee) + 40.0
        val deposit = companyProfile.defaultDeposit
        val platformFee = (deposit * 0.05).coerceAtLeast(2.50)
        val stripeLink = "https://checkout.stripe.com/pay/${companyProfile.stripeAccountId}?quote=$quoteId&amt=$deposit"

        Quote(
            id = quoteId,
            customerPhone = customerPhone,
            customerLocation = location,
            timeAgoText = "Just now",
            timestampMillis = System.currentTimeMillis(),
            serviceCategory = companyProfile.industry.uppercase(),
            audioUrl = if (!audioBase64.isNullOrBlank()) "audio_note_sample.mp3" else null,
            audioDurationText = if (!audioBase64.isNullOrBlank()) "0:14" else null,
            photoUrl = if (!imageBase64.isNullOrBlank()) "job_inspection.jpg" else null,
            aiSummary = "Detected: $serviceName. Customer requested dispatch for $location.",
            estimatedTotal = totalEst,
            requiredDeposit = deposit,
            platformFee = platformFee,
            status = "PENDING_APPROVAL",
            stripePaymentLink = stripeLink,
            rawCustomerNote = textInput ?: "Missed call voice note / photo inquiry"
        )
    }

    private fun parseJson(text: String): GeminiQuoteOutput? {
        val cleanJson = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return try {
            adapter.fromJson(cleanJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun extractServicesFromPrompt(query: String): List<ExtractedServiceItem> = withContext(Dispatchers.IO) {
        val apiKey = GeminiApiClient.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext emptyList()
        }

        val systemPrompt = """
            You are a pricing catalog extractor. Parse the input text line-by-line and return a JSON array of objects.
            
            Extract a clean list of individual services with their exact titles and base prices.
            Do NOT split service titles on words like 'up to', 'per line', or 'each'.
            Ignore context notes when determining the title name, but keep the full descriptive name intact 
            (e.g., 'Wi-Fi Mesh Setup (Up to 2 routers)' -> Price: 80.00).

            Respond ONLY with a valid JSON ARRAY of objects with EXACTLY these fields:
            "name" (string): Service title/name
            "price" (number): Base price or flat rate as Double
            "description" (string): Short description or keywords
            
            Example JSON output:
            [
              {
                "name": "Wi-Fi Mesh Setup (Up to 2 routers)",
                "price": 80.00,
                "description": "Mesh network setup"
              }
            ]
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = "Extract service items from this input: $query")))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = com.kuote.agent.data.remote.GenerationConfig(temperature = 0.1f, responseMimeType = "application/json")
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            return@withContext parseServicesJsonArray(responseText)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun parseServicesJsonArray(jsonText: String): List<ExtractedServiceItem> {
        val clean = jsonText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val results = mutableListOf<ExtractedServiceItem>()
        try {
            val array = org.json.JSONArray(clean)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", obj.optString("service_name", "Field Service"))
                val price = obj.optDouble("price", obj.optDouble("estimated_total", 95.0))
                val desc = obj.optString("description", obj.optString("customer_summary", ""))
                if (name.length >= 3 && !invalidFragments.any { name.contains(it, ignoreCase = true) }) {
                    results.add(ExtractedServiceItem(name = name, price = price, description = desc))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    private val invalidFragments = listOf("up to", "for", "each")
}
