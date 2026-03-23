package com.yourorg.openskills.ui.project

import com.yourorg.openskills.manifest.ResolvedSkill
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class ResolvedSkillCellRenderer : JPanel(BorderLayout()), ListCellRenderer<ResolvedSkill> {
    private val titleLabel = JLabel()
    private val versionLabel = JLabel()

    init {
        add(titleLabel, BorderLayout.CENTER)
        add(versionLabel, BorderLayout.EAST)
    }

    override fun getListCellRendererComponent(
        list: JList<out ResolvedSkill>,
        value: ResolvedSkill,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        titleLabel.text = value.id
        versionLabel.text = value.version

        background = if (isSelected) list.selectionBackground else list.background
        foreground = if (isSelected) list.selectionForeground else list.foreground
        titleLabel.foreground = foreground
        versionLabel.foreground = foreground
        return this
    }
}
