package com.yourorg.openskills.manifest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class ProjectManifestService(private val project: Project) {
    private val mapper = jacksonObjectMapper()

    private fun projectRoot(): Path {
        val basePath = project.basePath ?: error("Project base path is not available.")
        return Path.of(basePath)
    }

    fun manifestPath(): Path = projectRoot().resolve("openskills.json")

    fun lockPath(): Path = projectRoot().resolve("openskills.lock.json")

    fun readManifestOrNull(): OpenSkillsManifest? {
        val path = manifestPath()
        if (!Files.exists(path)) return null
        return mapper.readValue(path.toFile())
    }

    fun readLockOrNull(): OpenSkillsLock? {
        val path = lockPath()
        if (!Files.exists(path)) return null
        return mapper.readValue(path.toFile())
    }

    fun installPath(registry: String): Path {
        val manifest = readManifestOrNull()
        val configured = manifest?.installPath ?: ".agent/skills"
        return projectRoot().resolve(configured)
    }

    fun upsertInstalledSkill(
        registry: String,
        skillId: String,
        versionConstraint: String,
        resolvedVersion: String,
        packageUrl: String,
        checksumSha256: String,
        sourceIndexUrl: String?
    ) {
        val currentManifest = readManifestOrNull()
        val currentLock = readLockOrNull()
        val installPathString = currentManifest?.installPath ?: ".agent/skills"
        val installedPath = "$installPathString/$skillId"

        val manifestSkills = (currentManifest?.skills ?: emptyList())
            .filterNot { it.id == skillId } + ManifestSkill(id = skillId, version = versionConstraint)

        val newManifest = OpenSkillsManifest(
            manifestVersion = "1",
            registry = registry,
            installPath = installPathString,
            skills = manifestSkills.sortedBy { it.id }
        )

        val resolvedSkills = (currentLock?.resolved ?: emptyList())
            .filterNot { it.id == skillId } + ResolvedSkill(
            id = skillId,
            version = resolvedVersion,
            packageUrl = packageUrl,
            checksumSha256 = checksumSha256,
            installedPath = installedPath,
            sourceIndexUrl = sourceIndexUrl,
            publishedAt = Instant.now().toString()
        )

        val newLock = OpenSkillsLock(
            lockVersion = "1",
            registry = registry,
            installPath = installPathString,
            generatedAt = Instant.now().toString(),
            resolved = resolvedSkills.sortedBy { it.id }
        )

        writeJson(manifestPath(), newManifest)
        writeJson(lockPath(), newLock)
    }

    private fun writeJson(path: Path, value: Any) {
        Files.createDirectories(path.parent)
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value)
    }
}

