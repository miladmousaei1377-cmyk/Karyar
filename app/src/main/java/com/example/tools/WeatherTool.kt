package com.example.tools

import com.example.core.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WeatherTool : BaseTool() {
    override val name = "get_weather"
    override val description = "اطلاعات آب‌وهوای فعلی یک شهر را برمی‌گرداند"
    override val inputSchema: Map<String, Map<String, Any>> = mapOf(
        "city" to mapOf(
            "type" to "string",
            "description" to "نام شهر به انگلیسی مثل Tehran, London, New York"
        )
    )
    override val requiredFields = listOf("city")

    private val client = OkHttpClient()

    override suspend fun execute(input: Map<String, Any>): String = withContext(Dispatchers.IO) {
        val city = input["city"] as? String ?: return@withContext "نام شهر نامعتبر"
        val apiKey = AppConfig.openWeatherApiKey
        if (apiKey.isBlank()) return@withContext "کلید API آب‌وهوا تنظیم نشده است."

        runCatching {
            val url = "https://api.openweathermap.org/data/2.5/weather" +
                    "?q=${city.replace(" ", "+")}&appid=$apiKey&units=metric&lang=fa"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching "اطلاعات آب‌وهوا برای \"$city\" یافت نشد."
                val json = JSONObject(response.body?.string() ?: return@runCatching "پاسخ خالی")
                val main = json.getJSONObject("main")
                val weather = json.getJSONArray("weather").getJSONObject(0)
                val wind = json.getJSONObject("wind")
                val cityName = json.optString("name", city)

                """آب‌وهوای $cityName:
دما: ${main.getDouble("temp").toInt()}°C (احساس: ${main.getDouble("feels_like").toInt()}°C)
وضعیت: ${weather.optString("description")}
رطوبت: ${main.getInt("humidity")}%
سرعت باد: ${wind.optDouble("speed", 0.0)} m/s"""
            }
        }.getOrElse { "خطا در دریافت آب‌وهوا: ${it.message}" }
    }
}
