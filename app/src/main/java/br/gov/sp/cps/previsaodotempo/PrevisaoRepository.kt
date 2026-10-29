package br.gov.sp.cps.previsaodotempo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PrevisaoRepository {
    suspend fun buscarPrevisao(cidade: String): Previsao = withContext(Dispatchers.IO) {
        val cidadeEncoded = URLEncoder.encode(cidade, "UTF-8")
        val urlGeocoding = URL("https://geocoding-api.open-meteo.com/v1/search?name=$cidadeEncoded&count=1&language=pt&format=json")
        val conexaoGeo = urlGeocoding.openConnection() as HttpURLConnection
        
        val latitude: Double
        val longitude: Double
        val nomeCidade: String

        try {
            conexaoGeo.requestMethod = "GET"
            conexaoGeo.connectTimeout = 10000
            conexaoGeo.readTimeout = 10000

            val codigoResposta = conexaoGeo.responseCode
            val fluxo = if (codigoResposta in 200..299) conexaoGeo.inputStream else conexaoGeo.errorStream
            val resposta = fluxo?.bufferedReader()?.use { it.readText() } ?: throw IOException("A API não retornou conteúdo.")

            if (codigoResposta !in 200..299) throw IOException("A API retornou HTTP $codigoResposta.")

            val json = JSONObject(resposta)
            val results = json.optJSONArray("results")
            
            if (results == null || results.length() == 0) {
                throw Exception("Cidade não encontrada.")
            }
            
            val cidadeObj = results.getJSONObject(0)
            latitude = cidadeObj.getDouble("latitude")
            longitude = cidadeObj.getDouble("longitude")
            nomeCidade = cidadeObj.getString("name")
        } finally {
            conexaoGeo.disconnect()
        }

        val urlWeather = URL("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true")
        val conexaoWeather = urlWeather.openConnection() as HttpURLConnection
        
        try {
            conexaoWeather.requestMethod = "GET"
            conexaoWeather.connectTimeout = 10000
            conexaoWeather.readTimeout = 10000

            val codigoResposta = conexaoWeather.responseCode
            val fluxo = if (codigoResposta in 200..299) conexaoWeather.inputStream else conexaoWeather.errorStream
            val resposta = fluxo?.bufferedReader()?.use { it.readText() } ?: throw IOException("A API não retornou conteúdo.")

            if (codigoResposta !in 200..299) throw IOException("A API retornou HTTP $codigoResposta.")

            val json = JSONObject(resposta)
            val currentWeather = json.getJSONObject("current_weather")
            val temperatura = currentWeather.getDouble("temperature")

            Previsao(nome = nomeCidade, temperatura = temperatura)
        } finally {
            conexaoWeather.disconnect()
        }
    }
}
