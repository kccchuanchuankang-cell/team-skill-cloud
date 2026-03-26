package com.yourorg.openskills.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class OpenSkillsConfigurable : Configurable {
    private var panel: JPanel? = null
    private val cliLauncherField = JTextField()
    private val autoSyncCheckBox = JCheckBox("Run sync automatically after install")
    private val hintLabel = JLabel("Current prototype settings are focused on the OpenSkills CLI launcher and whether AGENTS.md should be synced automatically after installs.").apply {
        foreground = JBColor.GRAY
    }

    override fun getDisplayName(): String = "OpenSkills"

    override fun createComponent(): JComponent {
        if (panel == null) {
            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("CLI launcher", cliLauncherField)
                .addComponent(autoSyncCheckBox)
                .addComponent(hintLabel)
                .addComponentFillVertically(JPanel(), 0)
                .panel
            reset()
        }
        return panel!!
    }

    override fun isModified(): Boolean {
        val state = PluginSettingsService.getInstance().currentState()
        return cliLauncherField.text != state.cliLauncher ||
            autoSyncCheckBox.isSelected != state.autoSync
    }

    override fun apply() {
        val current = PluginSettingsService.getInstance().currentState()
        val cliLauncher = cliLauncherField.text.trim().ifBlank { current.cliLauncher }
        PluginSettingsService.getInstance().updateState(
            current.copy(
                cliLauncher = cliLauncher,
                autoSync = autoSyncCheckBox.isSelected,
                syncCommand = "$cliLauncher openskills sync"
            )
        )
    }

    override fun reset() {
        val state = PluginSettingsService.getInstance().currentState()
        cliLauncherField.text = state.cliLauncher
        autoSyncCheckBox.isSelected = state.autoSync
    }
}

