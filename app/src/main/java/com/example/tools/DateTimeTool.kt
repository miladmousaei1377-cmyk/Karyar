package com.example.tools

import java.text.SimpleDateFormat
import java.util.*

class DateTimeTool : BaseTool() {
    override val name = "get_datetime"
    override val description = "تاریخ و ساعت فعلی دستگاه را برمی‌گرداند"
    override val inputSchema: Map<String, Map<String, Any>> = mapOf(
        "format" to mapOf(
            "type" to "string",
            "description" to "فرمت: \"full\", \"date\", \"time\""
        )
    )

    override suspend fun execute(input: Map<String, Any>): String {
        val now = Date()
        return when (input["format"] as? String) {
            "date" -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(now)
            "time" -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
            else -> {
                val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(now)
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                "تاریخ: $date | ساعت: $time"
            }
        }
    }
}
