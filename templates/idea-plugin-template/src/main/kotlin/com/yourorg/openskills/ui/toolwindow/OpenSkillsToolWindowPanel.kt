package com.yourorg.openskills.ui.toolwindow

import com.intellij.openapi.project.Project
import com.yourorg.openskills.ui.catalog.CatalogPanel
import com.yourorg.openskills.ui.project.ProjectPanel
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTabbedPane

class OpenSkillsToolWindowPanel(project: Project) : JPanel(BorderLayout()) {
    init {
        val tabs = JTabbedPane()
        tabs.addTab("Catalog", CatalogPanel(project))
        tabs.addTab("Project", ProjectPanel(project))
        tabs.addTab("Settings", JPanel().apply { add(JLabel("TODO: Settings")) })
        add(tabs, BorderLayout.CENTER)
    }
}
