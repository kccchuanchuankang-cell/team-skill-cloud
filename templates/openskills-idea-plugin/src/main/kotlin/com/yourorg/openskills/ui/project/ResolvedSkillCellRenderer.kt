package com.yourorg.openskills.ui.project

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.yourorg.openskills.manifest.ResolvedSkill
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class ResolvedSkillCellRenderer : JPanel(BorderLayout()), ListCellRenderer<ResolvedSkill> {
    private val contentPanel = JPanel()
    private val titleLabel = JLabel()
    private val metaLabel = JLabel()
    private val versionLabel = JLabel()

    init {
        border = JBUI.Borders.empty(6, 8)
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.isOpaque = false

        metaLabel.foreground = JBColor.GRAY
        versionLabel.foreground = JBColor.GRAY

        contentPanel.add(titleLabel)
        contentPanel.add(metaLabel)

        add(contentPanel, BorderLayout.CENTER)
        add(versionLabel, BorderLayout.EAST)
        minimumSize = Dimension(120, 44)
        preferredSize = Dimension(120, 44)
    }

    override fun getListCellRendererComponent(
        list: JList<out ResolvedSkill>,
        value: ResolvedSkill,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        titleLabel.text = value.id
        metaLabel.text = "${inferScope(value)} · ${inferMode(value)}"
        versionLabel.text = value.version

        background = if (isSelected) list.selectionBackground else list.background
        foreground = if (isSelected) list.selectionForeground else list.foreground
        titleLabel.foreground = foreground
        metaLabel.foreground = if (isSelected) foreground else JBColor.GRAY
        versionLabel.foreground = if (isSelected) foreground else JBColor.GRAY
        return this
    }

    private fun inferScope(skill: ResolvedSkill): String = when {
        skill.installedPath.contains("/.agent/skills/") || skill.installedPath.contains("\\.agent\\skills\\") -> if (skill.installedPath.contains(".agent\\skills") && skill.installedPath.contains(":\\Users\\", ignoreCase = true)) "global" else "project"
        skill.installedPath.contains("/.claude/skills/") || skill.installedPath.contains("\\.claude\\skills\\") -> if (skill.installedPath.contains(".claude\\skills") && skill.installedPath.contains(":\\Users\\", ignoreCase = true)) "global" else "project"
        else -> "unknown"
    }

    private fun inferMode(skill: ResolvedSkill): String = when {
        skill.installedPath.contains("/.agent/skills/") || skill.installedPath.contains("\\.agent\\skills\\") -> "universal"
        skill.installedPath.contains("/.claude/skills/") || skill.installedPath.contains("\\.claude\\skills\\") -> "claude"
        else -> "unknown"
    }
}
