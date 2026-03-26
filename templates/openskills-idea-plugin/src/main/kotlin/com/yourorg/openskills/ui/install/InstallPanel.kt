package com.yourorg.openskills.ui.install

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.yourorg.openskills.cli.OpenSkillsCliAdapter
import com.yourorg.openskills.settings.PluginSettingsService
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class InstallPanel(
    private val project: Project,
    private val onInstallSuccess: ((String) -> Unit)? = null
) : JPanel(BorderLayout()) {
    private val cli = OpenSkillsCliAdapter(project)
    private val sourceField = JTextField()
    private val browseButton = JButton("Choose Folder")
    private val modeBox = JComboBox(arrayOf(
        "Project (.agent/skills)",
        "Global (~/.agent/skills)"
    ))
    private val installButton = JButton("Install")
    private val outputArea = JBTextArea()
    private val statusLabel = JLabel("Ready")
    private val hintLabel = JLabel("OpenSkills will always use universal mode here, so the installed skill lands in .agent/skills and can be shared by Claude Code and other agents.")

    init {
        border = JBUI.Borders.empty(8)
        sourceField.toolTipText = "GitHub source, private repo URL, local repository path, or local skill folder path."
        browseButton.toolTipText = "Choose a local source folder instead of typing the path manually."

        val sourceRow = JPanel(BorderLayout(8, 0))
        sourceRow.add(sourceField, BorderLayout.CENTER)
        sourceRow.add(browseButton, BorderLayout.EAST)
        modeBox.prototypeDisplayValue = "Global (~/.agent/skills)"
        hintLabel.border = JBUI.Borders.emptyTop(4)
        hintLabel.foreground = JBColor.GRAY
        hintLabel.font = hintLabel.font.deriveFont(hintLabel.font.size2D - 1f)

        val form = JPanel(GridBagLayout())
        form.border = JBUI.Borders.emptyBottom(8)

        addRow(form, 0, "Source", sourceRow)
        addRow(form, 1, "Install Location", modeBox)
        val hintConstraints = GridBagConstraints().apply {
            gridx = 1
            gridy = 2
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.NORTHWEST
            insets = JBUI.insets(0, 0, 0, 0)
        }
        form.add(hintLabel, hintConstraints)

        val topBar = JPanel(BorderLayout())
        val actions = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        actions.add(installButton)
        topBar.add(actions, BorderLayout.WEST)
        topBar.add(statusLabel, BorderLayout.EAST)

        outputArea.isEditable = false
        outputArea.lineWrap = true
        outputArea.wrapStyleWord = true
        outputArea.margin = JBUI.insets(8)
        outputArea.text = "Install logs will appear here."
        val outputScrollPane = JBScrollPane(outputArea).apply {
            preferredSize = Dimension(0, JBUI.scale(180))
        }

        add(form, BorderLayout.NORTH)
        add(topBar, BorderLayout.CENTER)
        add(outputScrollPane, BorderLayout.SOUTH)

        installButton.addActionListener { installSkill() }
        browseButton.addActionListener { chooseLocalSource() }
    }

    private fun addRow(panel: JPanel, row: Int, label: String, component: java.awt.Component) {
        val labelConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = row
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(0, 0, 8, 12)
        }
        panel.add(JLabel(label), labelConstraints)

        val fieldConstraints = GridBagConstraints().apply {
            gridx = 1
            gridy = row
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(0, 0, 8, 0)
        }
        panel.add(component, fieldConstraints)
    }

    private fun chooseLocalSource() {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
            title = "Choose OpenSkills Source Folder"
            description = "Select a local skill folder or a local skills repository."
        }
        val chooser = FileChooserFactory.getInstance().createPathChooser(descriptor, project, null)
        val initialDir = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        chooser.choose(initialDir) { selected ->
            val path = selected.firstOrNull()?.toNioPath()?.toAbsolutePath()?.normalize()?.toString()?.replace('\\', '/')
                ?: return@choose
            sourceField.text = path
        }
    }

    private fun installSkill() {
        val source = normalizeSource(sourceField.text.trim())
        if (source.isBlank()) {
            Messages.showInfoMessage(project, "Enter a skill source first.", "OpenSkills Install")
            return
        }

        val selectedLocation = modeBox.selectedItem?.toString().orEmpty()
        val global = selectedLocation.startsWith("Global")
        val universal = true

        installButton.isEnabled = false
        statusLabel.text = "Installing..."
        outputArea.text = "Running openskills install $source..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Install", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Installing $source"
                val installResult = cli.install(source = source, global = global, universal = universal, yes = true)
                val autoSyncEnabled = PluginSettingsService.getInstance().currentState().autoSync
                val syncResult = if (installResult.isSuccess() && autoSyncEnabled) {
                    indicator.text = "Syncing AGENTS.md"
                    cli.sync()
                } else {
                    null
                }
                ApplicationManager.getApplication().invokeLater {
                    installButton.isEnabled = true
                    val locationLabel = when {
                        global -> "Global (~/.agent/skills)"
                        else -> "Project (./.agent/skills)"
                    }
                    outputArea.text = installResult.combinedOutput().ifBlank {
                        if (installResult.isSuccess()) "Install completed." else "Install failed."
                    }
                    if (installResult.isSuccess()) {
                        val syncSucceeded = syncResult?.isSuccess() != false
                        statusLabel.text = if (syncResult == null) {
                            "Installed"
                        } else if (syncSucceeded) {
                            "Installed + Synced"
                        } else {
                            "Installed (sync failed)"
                        }
                        outputArea.text = buildString {
                            appendLine("Installed successfully.")
                            appendLine()
                            appendLine("Source")
                            appendLine(source)
                            appendLine()
                            appendLine("Install Location")
                            appendLine(locationLabel)
                            appendLine()
                            appendLine("Auto Sync")
                            appendLine(
                                when {
                                    syncResult == null && autoSyncEnabled -> "Skipped"
                                    syncResult == null -> "Disabled"
                                    syncSucceeded -> "Completed"
                                    else -> "Failed"
                                }
                            )
                            val installLog = installResult.combinedOutput()
                            if (installLog.isNotBlank()) {
                                appendLine()
                                appendLine("Install Output")
                                appendLine(installLog)
                            }
                            val syncLog = syncResult?.combinedOutput().orEmpty()
                            if (syncLog.isNotBlank()) {
                                appendLine()
                                appendLine("Sync Output")
                                appendLine(syncLog)
                            }
                        }.trim()
                        onInstallSuccess?.invoke(source)
                        if (syncResult != null && !syncSucceeded) {
                            Messages.showWarningDialog(
                                project,
                                "Installed $source successfully to $locationLabel, but automatic sync failed. Check the output panel for details.",
                                "OpenSkills Install"
                            )
                        } else {
                            Messages.showInfoMessage(
                                project,
                                if (syncResult != null) {
                                    "Installed $source successfully to $locationLabel and synced AGENTS.md."
                                } else {
                                    "Installed $source successfully to $locationLabel."
                                },
                                "OpenSkills Install"
                            )
                        }
                    } else {
                        statusLabel.text = "Install failed"
                        Messages.showErrorDialog(project, outputArea.text, "OpenSkills Install Error")
                    }
                }
            }
        })
    }

    private fun normalizeSource(raw: String): String {
        if (raw.isBlank()) return raw

        return try {
            val candidate = Path.of(raw)
            if (Files.exists(candidate)) {
                val absolute = candidate.toAbsolutePath().normalize()
                val projectRoot = project.basePath?.let { Path.of(it).toAbsolutePath().normalize() }
                if (projectRoot != null && absolute.root == projectRoot.root) {
                    val relative = projectRoot.relativize(absolute).toString().replace('\\', '/')
                    if (relative.startsWith(".")) relative else "./$relative"
                } else {
                    absolute.toString().replace('\\', '/')
                }
            } else {
                raw
            }
        } catch (_: Exception) {
            raw
        }
    }
}
