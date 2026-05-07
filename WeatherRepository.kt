package iq.gov.smartkarbala.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson
import iq.gov.smartkarbala.BuildConfig
import iq.gov.smartkarbala.data.model.WeatherData
import iq.gov.smartkarbala.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WeatherRepository(private val context: Context) {

    private val prefs = PrefsManager(context)
    private val gson = Gson()

    // Karbala coordinates
    private val KARBALA_LAT = 32.6167
    private val KARBALA_LON = 44.0333
    private val WEATHER_API_KEY = BuildConfig.WEATHER_API_KEY

    sealed class Result {
        data class Success(val weather: WeatherData, val fromCache: Boolean = false) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun getWeather(): Result = withContext(Dispatchers.IO) {
        if (isNetworkAvailable()) {
            fetchFromApi()
        } else {
            getCachedWeather()
        }
    }

    private suspend fun fetchFromApi(): Result {
        return try {
            val url = URL(
                "https://api.openweathermap.org/data/2.5/weather" +
                "?lat=$KARBALA_LAT&lon=$KARBALA_LON" +
                "&units=metric&lang=ar&appid=$WEATHER_API_KEY"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val json = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                val weather = parseWeatherJson(json)

                // Cache for offline
                prefs.cachedWeatherJson = json
                prefs.lastWeatherUpdate = System.currentTimeMillis()

                Result.Success(weather)
            } else {
                connection.disconnect()
                getCachedWeather()
            }
        } catch (e: Exception) {
            getCachedWeather()
        }
    }

    private fun getCachedWeather(): Result {
        val cached = prefs.cachedWeatherJson
        return if (cached.isNotEmpty()) {
            try {
                Result.Success(parseWeatherJson(cached), fromCache = true)
            } catch (e: Exception) {
                Result.Success(getDefaultKarbalaWeather())
            }
        } else {
            Result.Success(getDefaultKarbalaWeather())
        }
    }

    private fun parseWeatherJson(json: String): WeatherData {
        val obj = JSONObject(json)
        val main = obj.getJSONObject("main")
        val weatherArr = obj.getJSONArray("weather")
        val weatherObj = weatherArr.getJSONObject(0)
        val wind = obj.optJSONObject("wind")

        return WeatherData(
            temperature = main.getDouble("temp"),
            feelsLike = main.getDouble("feels_like"),
            description = weatherObj.getString("description"),
            icon = weatherObj.getString("icon"),
            humidity = main.getInt("humidity"),
            windSpeed = wind?.optDouble("speed", 0.0)?.times(3.6) ?: 0.0, // m/s to km/h
            uvIndex = 0.0,
            visibility = obj.optDouble("visibility", 0.0).div(1000), // meters to km
            pressure = main.optInt("pressure", 0),
            cityName = "كربلاء المقدسة",
            timestamp = System.currentTimeMillis()
        )
    }

    private fun getDefaultKarbalaWeather(): WeatherData {
        // Typical summer weather for Karbala
        return WeatherData(
            temperature = 38.0,
            feelsLike = 42.0,
            description = "صافٍ",
            icon = "01d",
            humidity = 15,
            windSpeed = 12.0,
            uvIndex = 9.0,
            cityName = "كربلاء المقدسة"
        )
    }

    fun getWeatherIconUrl(icon: String): String =
        "https://openweathermap.org/img/wn/$icon@2x.png"

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
