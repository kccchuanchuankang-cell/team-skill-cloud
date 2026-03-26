package com.yourorg.openskills.ui.toolwindow

import com.intellij.openapi.project.Project
import com.yourorg.openskills.settings.OpenSkillsConfigurable
import com.yourorg.openskills.ui.install.InstallPanel
import com.yourorg.openskills.ui.project.ProjectPanel
import com.yourorg.openskills.ui.resolution.ResolutionPanel
import java.awt.BorderLayout
import javax.swing.JTabbedPane

class OpenSkillsToolWindowPanel(project: Project) : javax.swing.JPanel(BorderLayout()) {
    init {
        val installedPanel = ProjectPanel(project)
        val resolutionPanel = ResolutionPanel(project)
        val tabs = JTabbedPane()
        tabs.addTab("Installed", installedPanel)
        tabs.addTab("Install", InstallPanel(project) {
            installedPanel.refreshNow()
            resolutionPanel.refreshNow()
            tabs.selectedIndex = 0
        })
        tabs.addTab("Resolution", resolutionPanel)
        tabs.addTab("Settings", OpenSkillsConfigurable().createComponent())
        add(tabs, BorderLayout.CENTER)
    }
}
