package com.yourorg.openskills.install

import com.intellij.openapi.project.Project
import com.yourorg.openskills.manifest.ProjectManifestService
import com.yourorg.openskills.registry.RegistryClient
import com.yourorg.openskills.registry.SkillSummary
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class SkillInstaller(private val project: Project, private val registryBaseUrl: String) {
    private val http = HttpClient.newBuilder().build()
    private val manifests = ProjectManifestService(project)
    private val registryClient = RegistryClient(registryBaseUrl)

    fun installLatestStable(skill: SkillSummary): InstalledSkillResult {
        val versionIndex = registryClient.fetchSkillVersionIndex(skill.indexUrl)
        val target = versionIndex.versions.firstOrNull { it.version == skill.latestStableVersion }
            ?: versionIndex.versions.firstOrNull()
            ?: error("No installable versions found for ${skill.id}")

        val tempZip = Files.createTempFile("openskills-${skill.id}-", ".zip")
        downloadFile(target.packageUrl, tempZip)
        verifyChecksum(tempZip, target.checksumSha256)
        extractZip(tempZip, manifests.installPath(registryBaseUrl).resolve(skill.id).parent)
        Files.deleteIfExists(tempZip)

        manifests.upsertInstalledSkill(
            registry = registryBaseUrl,
            skillId = skill.id,
            versionConstraint = target.version,
            resolvedVersion = target.version,
            packageUrl = target.packageUrl,
            checksumSha256 = target.checksumSha256,
            sourceIndexUrl = skill.indexUrl
        )

        return InstalledSkillResult(skill.id, target.version)
    }

    private fun downloadFile(url: String, output: Path) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(resolveUrl(url)))
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofFile(output))
        if (response.statusCode() !in 200..299) {
            throw IOException("Failed to download package: ${response.statusCode()} for $url")
        }
    }

    private fun verifyChecksum(file: Path, expectedSha256: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expectedSha256, ignoreCase = true)) {
            throw IOException("Checksum mismatch. Expected $expectedSha256 but got $actual")
        }
    }

    private fun extractZip(zipFile: Path, destinationRoot: Path) {
        Files.createDirectories(destinationRoot)
        ZipInputStream(Files.newInputStream(zipFile)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val output = destinationRoot.resolve(entry.name).normalize()
                if (!output.startsWith(destinationRoot.normalize())) {
                    throw IOException("Blocked unsafe zip entry: ${entry.name}")
                }
                if (entry.isDirectory) {
                    Files.createDirectories(output)
                } else {
                    Files.createDirectories(output.parent)
                    Files.newOutputStream(output).use { out ->
                        zip.copyTo(out)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun resolveUrl(path: String): String {
        val trimmedBase = registryBaseUrl.trim().trimEnd('/')
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            val trimmedPath = path.trimStart('/')
            "$trimmedBase/$trimmedPath"
        }
    }
}

data class InstalledSkillResult(
    val skillId: String,
    val version: String
)
