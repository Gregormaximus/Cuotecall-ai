package com.kuote.agent.ai

import com.kuote.agent.data.model.CompanyProfile
import com.kuote.agent.data.model.CompanyWebConfig
import com.kuote.agent.data.remote.GeminiApiClient
import com.kuote.agent.data.remote.GeminiContent
import com.kuote.agent.data.remote.GeminiPart
import com.kuote.agent.data.remote.GeminiRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WebSiteConfigOutput(
    val site_title: String,
    val site_subtitle: String,
    val voice_call_button_text: String,
    val voice_call_description: String,
    val quick_deposit_fee: Double,
    val theme_color_hex: String = "#00E5FF"
)

class MicroSiteGeneratorEngine {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(WebSiteConfigOutput::class.java)

    suspend fun generateMicroSiteConfig(
        prompt: String,
        companyProfile: CompanyProfile
    ): CompanyWebConfig = withContext(Dispatchers.IO) {

        val apiKey = GeminiApiClient.getApiKey()

        val systemPrompt = """
            You are an AI Web & Voice Site Generator for field services.
            Convert the user's prompt into a high-converting micro-site configuration for ${companyProfile.name} (${companyProfile.industry}).
            
            Return ONLY a valid JSON object matching this structure:
            {
              "site_title": "TOW PRO EXPRESS",
              "site_subtitle": "24/7 Professional Roadside Assistance",
              "voice_call_button_text": "Instant AI Dispatch",
              "voice_call_description": "Speak directly with our AI to book a tow",
              "quick_deposit_fee": 75.00,
              "theme_color_hex": "#00E5FF"
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        if (apiKey.isNotBlank()) {
            try {
                val response = GeminiApiClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val parsed = parseJson(responseText)
                if (parsed != null) {
                    val slug = companyProfile.name.lowercase().trim().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "my-business" }
                    return@withContext CompanyWebConfig(
                        companyId = companyProfile.id,
                        siteTitle = parsed.site_title,
                        siteSubtitle = parsed.site_subtitle,
                        themeColorHex = parsed.theme_color_hex,
                        voiceCallButtonText = parsed.voice_call_button_text,
                        voiceCallDescription = parsed.voice_call_description,
                        quickDepositFee = parsed.quick_deposit_fee,
                        deployedUrl = "https://quotebit.app/?slug=$slug"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Default or Fallback Site Config
        val slug = companyProfile.name.lowercase().trim().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "my-business" }
        CompanyWebConfig(
            companyId = companyProfile.id,
            siteTitle = companyProfile.name.uppercase(),
            siteSubtitle = "24/7 Professional ${companyProfile.industry} Assistance",
            themeColorHex = "#00E5FF",
            voiceCallButtonText = "Instant AI Dispatch",
            voiceCallDescription = "Speak directly with our AI to book an instant service",
            quickDepositFee = companyProfile.defaultDeposit,
            deployedUrl = "https://quotebit.app/?slug=$slug"
        )
    }

    private fun parseJson(text: String): WebSiteConfigOutput? {
        val clean = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return try {
            adapter.fromJson(clean)
        } catch (e: Exception) {
            null
        }
    }
}
