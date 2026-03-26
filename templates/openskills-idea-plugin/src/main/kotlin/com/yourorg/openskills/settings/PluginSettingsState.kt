package com.yourorg.openskills.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Files
import java.nio.file.Path

@Service(Service.Level.APP)
@State(name = "OpenSkillsSettings", storages = [Storage("openskills.xml")])
class PluginSettingsService : PersistentStateComponent<PluginSettingsState> {
    private var state = PluginSettingsState()

    override fun getState(): PluginSettingsState = state

    override fun loadState(state: PluginSettingsState) {
        this.state = state
    }

    fun currentState(): PluginSettingsState = state.copy()

    fun updateState(updated: PluginSettingsState) {
        state = updated
    }

    companion object {
        fun getInstance(): PluginSettingsService = ApplicationManager.getApplication().getService(PluginSettingsService::class.java)

        fun defaultCliLauncher(): String {
            if (SystemInfo.isWindows) {
                val commonLaunchers = listOf(
                    Path.of("C:/Program Files/nodejs/npx.cmd"),
                    Path.of("C:/Program Files/nodejs/npx"),
                    Path.of(System.getProperty("user.home"), "AppData", "Roaming", "npm", "npx.cmd")
                )
                commonLaunchers.firstOrNull(Files::exists)?.let { return it.toString() }
                return "npx.cmd"
            }
            return "npx"
        }
    }
}

data class PluginSettingsState(
    var registryUrl: String = "https://skills.example.com/registry",
    var installPath: String = ".agent/skills",
    var cliLauncher: String = PluginSettingsService.defaultCliLauncher(),
    var syncCommand: String = "npx openskills sync",
    var autoSync: Boolean = true,
    var allowPrerelease: Boolean = false
)
