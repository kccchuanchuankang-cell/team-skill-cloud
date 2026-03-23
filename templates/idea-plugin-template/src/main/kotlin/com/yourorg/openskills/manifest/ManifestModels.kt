package com.yourorg.openskills.manifest

data class OpenSkillsManifest(
    val manifestVersion: String = "1",
    val registry: String,
    val installPath: String = ".agent/skills",
    val skills: List<ManifestSkill>
)

data class ManifestSkill(
    val id: String,
    val version: String,
    val source: String? = null,
    val enabled: Boolean = true
)

data class OpenSkillsLock(
    val lockVersion: String = "1",
    val registry: String,
    val installPath: String = ".agent/skills",
    val generatedAt: String,
    val resolved: List<ResolvedSkill>
)

data class ResolvedSkill(
    val id: String,
    val version: String,
    val packageUrl: String,
    val checksumSha256: String,
    val installedPath: String,
    val sourceIndexUrl: String? = null,
    val publishedAt: String? = null
)
