package com.yourorg.openskills.registry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class RegistryClient(private val baseUrl: String) {
    private val mapper = jacksonObjectMapper()
    private val http = HttpClient.newBuilder().build()

    fun fetchRegistryIndex(): RegistryIndex {
        return getJson(resolveUrl("index.json"))
    }

    fun fetchSkillVersionIndex(indexUrl: String): SkillVersionIndex {
        return getJson(resolveUrl(indexUrl))
    }

    private inline fun <reified T> getJson(url: String): T {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("Registry request failed: ${response.statusCode()} for $url")
        }
        return mapper.readValue(response.body())
    }

    private fun resolveUrl(path: String): String {
        val trimmedBase = baseUrl.trim().trimEnd('/')
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            val trimmedPath = path.trimStart('/')
            "$trimmedBase/$trimmedPath"
        }
    }
}
