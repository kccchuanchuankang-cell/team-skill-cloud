package com.yourorg.openskills.registry

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegistryIndex(
    @JsonProperty("registry_version")
    val registryVersion: String,
    @JsonProperty("generated_at")
    val generatedAt: String,
    val skills: List<SkillSummary>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SkillSummary(
    val id: String,
    val title: String,
    val description: String,
    val owner: String,
    val tags: List<String>,
    @JsonProperty("latest_version")
    val latestVersion: String,
    @JsonProperty("latest_stable_version")
    val latestStableVersion: String,
    @JsonProperty("index_url")
    val indexUrl: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SkillVersionIndex(
    val id: String,
    val title: String,
    val description: String,
    val owner: String,
    val versions: List<SkillVersion>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SkillVersion(
    val version: String,
    @JsonProperty("published_at")
    val publishedAt: String,
    val breaking: Boolean,
    @JsonProperty("manifest_version")
    val manifestVersion: String,
    @JsonProperty("package_url")
    val packageUrl: String,
    @JsonProperty("checksum_sha256")
    val checksumSha256: String,
    @JsonProperty("notes_url")
    val notesUrl: String?
)
