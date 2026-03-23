package com.yourorg.openskills.settings

data class PluginSettingsState(
    var registryUrl: String = "https://skills.example.com/registry",
    var installPath: String = ".agent/skills",
    var syncCommand: String = "npx openskills sync",
    var autoSync: Boolean = true,
    var allowPrerelease: Boolean = false
)
