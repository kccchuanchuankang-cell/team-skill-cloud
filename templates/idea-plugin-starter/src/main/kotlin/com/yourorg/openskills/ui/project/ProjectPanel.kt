package com.yourorg.openskills.ui.project

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.yourorg.openskills.manifest.ProjectManifestService
import com.yourorg.openskills.manifest.ResolvedSkill
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ListSelectionModel

class ProjectPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val manifests = ProjectManifestService(project)
    private val refreshButton = JButton("Refresh")
    private val openManifestButton = JButton("Open Manifest")
    private val listModel = DefaultListModel<ResolvedSkill>()
    private val skillList = JBList(listModel)
    private val detailArea = JTextArea()

    init {
        border = JBUI.Borders.empty(8)

        val topBar = JPanel(GridLayout(1, 2, 8, 0))
        topBar.add(refreshButton)
        topBar.add(openManifestButton)

        skillList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        skillList.cellRenderer = ResolvedSkillCellRenderer()
        skillList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                renderSelectedSkill()
            }
        }

        detailArea.isEditable = false
        detailArea.lineWrap = true
        detailArea.wrapStyleWord = true
        detailArea.margin = JBUI.insets(8)
        detailArea.text = "No installed skills loaded yet."

        val content = JPanel(GridLayout(1, 2, 8, 0))
        content.add(JBScrollPane(skillList))
        content.add(JBScrollPane(detailArea))

        add(topBar, BorderLayout.NORTH)
        add(content, BorderLayout.CENTER)

        refreshButton.addActionListener { loadInstalledSkills() }
        openManifestButton.addActionListener { openManifest() }

        loadInstalledSkills()
    }

    private fun loadInstalledSkills() {
        listModel.clear()
        val lock = manifests.readLockOrNull()
        if (lock == null) {
            detailArea.text = "No openskills.lock.json found. Install a skill first."
            return
        }

        lock.resolved.forEach(listModel::addElement)
        if (!listModel.isEmpty) {
            skillList.selectedIndex = 0
        } else {
            detailArea.text = "Lock file exists but no resolved skills were found."
        }
    }

    private fun renderSelectedSkill() {
        val skill = skillList.selectedValue ?: run {
            detailArea.text = "Select an installed skill to view details."
            return
        }

        detailArea.text = buildString {
            appendLine(skill.id)
            appendLine()
            appendLine("Installed version: ${skill.version}")
            appendLine("Package URL: ${skill.packageUrl}")
            appendLine("Checksum: ${skill.checksumSha256}")
            appendLine("Installed path: ${skill.installedPath}")
            appendLine("Source index: ${skill.sourceIndexUrl ?: "n/a"}")
            appendLine("Published at: ${skill.publishedAt ?: "n/a"}")
        }
    }

    private fun openManifest() {
        val manifestPath = manifests.manifestPath()
        val vFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(manifestPath)
        if (vFile == null) {
            Messages.showInfoMessage(project, "No openskills.json found yet.", "OpenSkills")
            return
        }
        OpenFileDescriptor(project, vFile).navigate(true)
    }
}
