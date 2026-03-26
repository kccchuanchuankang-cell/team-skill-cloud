package com.yourorg.openskills.ui.catalog

import com.yourorg.openskills.registry.SkillSummary
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class SkillSummaryCellRenderer : JPanel(BorderLayout()), ListCellRenderer<SkillSummary> {
    private val titleLabel = JLabel()
    private val versionLabel = JLabel()

    init {
        add(titleLabel, BorderLayout.CENTER)
        add(versionLabel, BorderLayout.EAST)
    }

    override fun getListCellRendererComponent(
        list: JList<out SkillSummary>,
        value: SkillSummary,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        titleLabel.text = value.title
        versionLabel.text = value.latestVersion

        background = if (isSelected) list.selectionBackground else list.background
        foreground = if (isSelected) list.selectionForeground else list.foreground
        titleLabel.foreground = foreground
        versionLabel.foreground = foreground
        return this
    }
}
