package com.example.memory

import android.content.Context
import com.example.agent.domain.models.ApiMessage

class MemoryService(private val dao: ConversationDao, context: Context) {
    private val prefs = context.getSharedPreferences("agent_profile", Context.MODE_PRIVATE)

    suspend fun saveConversation(userInput: String, agentResponse: String) {
        dao.insert(ConversationEntity(userMessage = userInput, agentResponse = agentResponse))
        dao.pruneOld()
        extractAndSaveUserInfo(userInput)
    }

    suspend fun getRecentApiMessages(count: Int): List<ApiMessage> {
        return dao.getRecent(count)
            .reversed()
            .flatMap { conv ->
                listOf(
                    ApiMessage("user", conv.userMessage),
                    ApiMessage("assistant", conv.agentResponse)
                )
            }
    }

    fun getMemoryContext(): String {
        val all = prefs.all
        if (all.isEmpty()) return ""
        val sb = StringBuilder()
        all.forEach { (k, v) -> sb.appendLine("- $k: $v") }
        return sb.toString()
    }

    suspend fun clearMemory() {
        dao.clearAll()
        prefs.edit().clear().apply()
    }

    private fun extractAndSaveUserInfo(input: String) {
        val nameMatch = Regex("""اسمم\s+(\w+)|نامم\s+(\w+)|من\s+(\w+)\s+هستم""").find(input)
        nameMatch?.let { m ->
            val name = m.groupValues.drop(1).firstOrNull { it.isNotBlank() }
            name?.let { prefs.edit().putString("نام کاربر", it).apply() }
        }
        val cities = listOf("تهران", "اصفهان", "مشهد", "شیراز", "تبریز", "کرج", "قم")
        cities.forEach { city ->
            if (input.contains(city)) {
                prefs.edit().putString("شهر", city).apply()
            }
        }
    }
}
