package com.yourorg.openskills.ui.catalog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.yourorg.openskills.install.SkillInstaller
import com.yourorg.openskills.registry.RegistryClient
import com.yourorg.openskills.registry.RegistryIndex
import com.yourorg.openskills.registry.SkillSummary
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
    private val registryUrlField = JTextField("https://skills.example.com/registry")
    private val loadButton = JButton("Load")
    private val installButton = JButton("Install Latest")
    private val skillListModel = DefaultListModel<SkillSummary>()
    private val skillList = JBList(skillListModel)
    private val detailArea = JTextArea()

    init {
        border = JBUI.Borders.empty(8)

        val topBar = JPanel(BorderLayout(8, 0))
        topBar.add(JLabel("Registry URL:"), BorderLayout.WEST)
        topBar.add(registryUrlField, BorderLayout.CENTER)

        val actions = JPanel(GridLayout(1, 2, 8, 0))
        actions.add(loadButton)
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

    private fun installSelectedSkill() {
        val skill = skillList.selectedValue ?: run {
            Messages.showInfoMessage(project, "Select a skill first.", "OpenSkills")
            return
        }

        try {
            val installer = SkillInstaller(project, registryUrlField.text)
            val installed = installer.installLatestStable(skill)
            Messages.showInfoMessage(
                project,
                "Installed ${installed.skillId} ${installed.version}.\nUpdated openskills.json and openskills.lock.json.",
                "OpenSkills Install Complete"
            )
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                project,
                ex.message ?: "Install failed.",
                "OpenSkills Install Error"
            )
        }
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
            appendLine("Action: Install Latest will download the package, extract it to .agent/skills, and update manifest files.")
        }
    }
}
