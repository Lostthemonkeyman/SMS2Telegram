package com.example.telegramforwarder.data.remote

import android.util.Log
import com.example.telegramforwarder.data.LogRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository(private val logger: LogRepository) {

    private val modelName = "models/gemini-flash-latest"

    suspend fun checkMessageForCode(message: String, keys: List<String>): String? {
        if (keys.isEmpty()) {
            logger.logError("GeminiRepository", "No API keys configured.")
            return null
        }

        // Prompt designed to force specific output
        val prompt = """
            Analyze the following SMS message.
            Does it contain a verification code, authentication code, or OTP?
            If YES, output ONLY the code itself (the number or string).
            If NO, output the word "NO".

            Message: "$message"
        """.trimIndent()

        for (key in keys) {
            try {
                logger.logInfo("GeminiRepository", "Attempting to call Gemini with key: ${key.take(4)}***")
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = key
                )

                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(prompt)
                }

                val output = response.text?.trim() ?: "NO"
                logger.logInfo("GeminiRepository", "Gemini response: $output")

                if (output.equals("NO", ignoreCase = true)) {
                    return null
                } else {
                    // Basic cleanup if the model got chatty, though the prompt tries to prevent it
                    // Find the first sequence of digits/letters that looks like a code if "NO" isn't the answer
                    // For now, trust the model returned the code as requested.
                    return output
                }
            } catch (e: Exception) {
                logger.logError("GeminiRepository", "Error calling Gemini with key ending in ...${key.takeLast(4)}: ${e.message}")
                // Continue to next key
            }
        }

        logger.logError("GeminiRepository", "All Gemini API keys failed.")
        return null
    }
}
