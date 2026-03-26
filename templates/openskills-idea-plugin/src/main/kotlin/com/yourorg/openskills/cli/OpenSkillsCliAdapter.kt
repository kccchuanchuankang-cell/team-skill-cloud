package com.yourorg.openskills.cli

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.project.Project
import com.yourorg.openskills.settings.PluginSettingsService
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class OpenSkillsCliAdapter(private val project: Project) {
    fun list(): CliCommandResult = run(*baseCommand(), "list")

    fun sync(): CliCommandResult = run(*baseCommand(), "sync", "-y")

    fun read(skillName: String): CliCommandResult = run(*baseCommand(), "read", skillName)

    fun update(skillName: String? = null): CliCommandResult {
        val args = mutableListOf(*baseCommand(), "update")
        if (!skillName.isNullOrBlank()) {
            args += skillName
        }
        return run(*args.toTypedArray())
    }

    fun remove(skillName: String): CliCommandResult = run(*baseCommand(), "remove", skillName)

    fun install(source: String, global: Boolean = false, universal: Boolean = false, yes: Boolean = true): CliCommandResult {
        val args = mutableListOf(*baseCommand(), "install", source)
        if (global) args += "--global"
        if (universal) args += "--universal"
        if (yes) args += "--yes"
        return run(*args.toTypedArray())
    }

    private fun baseCommand(): Array<String> {
        val configured = PluginSettingsService.getInstance().currentState().cliLauncher.trim()
        val launcher = configured.ifBlank { PluginSettingsService.defaultCliLauncher() }
        return arrayOf(launcher, "openskills")
    }

    private fun run(vararg command: String): CliCommandResult {
        val basePath = project.basePath
            ?: return CliCommandResult.failure(command.toList(), "Project base path is not available.")

        return try {
            val cmd = GeneralCommandLine(*command)
                .withWorkingDirectory(Path.of(basePath))
                .withCharset(StandardCharsets.UTF_8)

            val output = CapturingProcessHandler(cmd).runProcess(120_000)
            CliCommandResult(
                command = command.toList(),
                stdout = output.stdout,
                stderr = output.stderr,
                exitCode = output.exitCode,
                timedOut = output.isTimeout
            )
        } catch (ex: ExecutionException) {
            CliCommandResult.failure(command.toList(), buildFailureMessage(command.toList(), ex))
        } catch (ex: RuntimeException) {
            CliCommandResult.failure(command.toList(), ex.message ?: "Unknown OpenSkills CLI error.")
        }
    }

    private fun buildFailureMessage(command: List<String>, ex: ExecutionException): String {
        val joined = command.joinToString(" ")
        val base = ex.message ?: "Unable to run $joined."
        return if (base.contains("Cannot run program", ignoreCase = true)) {
            "Unable to run `$joined`. Configure a valid OpenSkills CLI launcher in Settings > Tools > OpenSkills. On Windows, a common value is `C:\\Program Files\\nodejs\\npx.cmd`."
        } else {
            base
        }
    }
}

data class CliCommandResult(
    val command: List<String>,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val timedOut: Boolean
) {
    fun isSuccess(): Boolean = !timedOut && exitCode == 0

    fun combinedOutput(): String = buildString {
        if (stdout.isNotBlank()) appendLine(stdout.stripAnsi().trim())
        if (stderr.isNotBlank()) appendLine(stderr.stripAnsi().trim())
    }.trim()

    private fun String.stripAnsi(): String = replace(Regex("\\u001B\\[[;\\d]*[ -/]*[@-~]"), "")

    companion object {
        fun failure(command: List<String>, message: String): CliCommandResult = CliCommandResult(
            command = command,
            stdout = "",
            stderr = message,
            exitCode = -1,
            timedOut = false
        )
    }
}
