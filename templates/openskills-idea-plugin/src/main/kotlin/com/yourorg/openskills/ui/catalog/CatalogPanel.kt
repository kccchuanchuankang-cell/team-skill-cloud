package com.yourorg.openskills.ui.catalog

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.yourorg.openskills.cli.OpenSkillsCliAdapter
import com.yourorg.openskills.install.SkillInstaller
import com.yourorg.openskills.registry.RegistryClient
import com.yourorg.openskills.registry.RegistryIndex
import com.yourorg.openskills.registry.SkillSummary
import com.yourorg.openskills.settings.PluginSettingsService
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel

class CatalogPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val registryUrlField = JTextField(PluginSettingsService.getInstance().currentState().registryUrl)
    private val loadButton = JButton("Load")
    private val installButton = JButton("Install Latest")
    private val readButton = JButton("Read")
    private val skillListModel = DefaultListModel<SkillSummary>()
    private val skillList = JBList(skillListModel)
    private val detailArea = JTextArea()
    private val cli = OpenSkillsCliAdapter(project)

    init {
        border = JBUI.Borders.empty(8)

        val topBar = JPanel(BorderLayout(8, 0))
        topBar.add(JLabel("Registry URL:"), BorderLayout.WEST)
        topBar.add(registryUrlField, BorderLayout.CENTER)

        val actions = JPanel(GridLayout(1, 3, 8, 0))
        actions.add(loadButton)
        actions.add(readButton)
        actions.add(installButton)
        topBar.add(actions, BorderLayout.EAST)

        skillList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        skillList.cellRenderer = SkillSummaryCellRenderer()
        skillList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                renderSelectedSkill()
            }
        }

        detailArea.isEditable = false
        detailArea.lineWrap = true
        detailArea.wrapStyleWord = true
        detailArea.text = "Load a registry to browse skills."
        detailArea.margin = JBUI.insets(8)

        val content = JPanel(GridLayout(1, 2, 8, 0))
        content.add(JBScrollPane(skillList))
        content.add(JBScrollPane(detailArea))

        add(topBar, BorderLayout.NORTH)
        add(content, BorderLayout.CENTER)

        loadButton.addActionListener { loadRegistry() }
        readButton.addActionListener { readSelectedSkill() }
        installButton.addActionListener { installSelectedSkill() }
    }

    private fun loadRegistry() {
        try {
            val client = RegistryClient(registryUrlField.text)
            val registry = client.fetchRegistryIndex()
            renderRegistry(registry)
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                project,
                ex.message ?: "Failed to load registry.",
                "OpenSkills Registry Error"
            )
        }
    }

    private fun readSelectedSkill() {
        val skill = skillList.selectedValue ?: run {
            Messages.showInfoMessage(project, "Select a skill first.", "OpenSkills")
            return
        }

        readButton.isEnabled = false
        detailArea.text = "Reading ${skill.id}..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Read", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running openskills read ${skill.id}"
                val result = cli.read(skill.id)
                ApplicationManager.getApplication().invokeLater {
                    readButton.isEnabled = true
                    if (result.isSuccess()) {
                        detailArea.text = result.stdout.ifBlank { "No content returned by openskills read ${skill.id}." }
                    } else {
                        Messages.showErrorDialog(
                            project,
                            result.combinedOutput().ifBlank { "Read failed." },
                            "OpenSkills Read Error"
                        )
                        renderSelectedSkill()
                    }
                }
            }
        })
    }

    private fun installSelectedSkill() {
        val skill = skillList.selectedValue ?: run {
            Messages.showInfoMessage(project, "Select a skill first.", "OpenSkills")
            return
        }

        installButton.isEnabled = false
        detailArea.text = "Installing ${skill.id}..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Install", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Installing ${skill.id}"
                try {
                    val installer = SkillInstaller(project, registryUrlField.text)
                    val installed = installer.installLatestStable(skill)
                    ApplicationManager.getApplication().invokeLater {
                        installButton.isEnabled = true
                        Messages.showInfoMessage(
                            project,
                            "Installed ${installed.skillId} ${installed.version}.\nUpdated openskills.json and openskills.lock.json.\nNext step: wire this action to openskills install once a stable install source is available.",
                            "OpenSkills Install Complete"
                        )
                        renderSelectedSkill()
                    }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        installButton.isEnabled = true
                        Messages.showErrorDialog(
                            project,
                            ex.message ?: "Install failed.",
                            "OpenSkills Install Error"
                        )
                        renderSelectedSkill()
                    }
                }
            }
        })
    }

    private fun renderRegistry(registry: RegistryIndex) {
        skillListModel.clear()
        registry.skills.forEach(skillListModel::addElement)
        if (!skillListModel.isEmpty) {
            skillList.selectedIndex = 0
        } else {
            detailArea.text = "No skills found in registry."
        }
    }

    private fun renderSelectedSkill() {
        val skill = skillList.selectedValue ?: run {
            detailArea.text = "Select a skill to view details."
            return
        }

        detailArea.text = buildString {
            appendLine(skill.title)
            appendLine()
            appendLine(skill.description)
            appendLine()
            appendLine("ID: ${skill.id}")
            appendLine("Owner: ${skill.owner}")
            appendLine("Latest version: ${skill.latestVersion}")
            appendLine("Latest stable: ${skill.latestStableVersion}")
            appendLine("Tags: ${skill.tags.joinToString(", ")}")
            appendLine("Index URL: ${skill.indexUrl}")
            appendLine()
            appendLine("Read: runs `openskills read ${skill.id}` via the configured launcher.")
            appendLine("Install Latest: current prototype installs from the registry package and will later be switched to a stable OpenSkills install source.")
        }
    }
}
