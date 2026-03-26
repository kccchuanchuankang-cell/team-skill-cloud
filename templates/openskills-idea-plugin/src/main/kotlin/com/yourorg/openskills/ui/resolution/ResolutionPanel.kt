package com.yourorg.openskills.ui.resolution

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.yourorg.openskills.cli.OpenSkillsCliAdapter
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class ResolutionPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val cli = OpenSkillsCliAdapter(project)
    private val refreshButton = JButton("Refresh")
    private val statusLabel = JLabel("Ready")
    private var syncingSelection = false

    private val priorityRoot = DefaultMutableTreeNode("Priority")
    private val priorityModel = DefaultTreeModel(priorityRoot)
    private val priorityTree = JTree(priorityModel)

    private val visibleRoot = DefaultMutableTreeNode("Visible Skills")
    private val visibleModel = DefaultTreeModel(visibleRoot)
    private val visibleTree = JTree(visibleModel)

    private val detailsArea = JBTextArea()
    private val outputArea = JBTextArea()

    init {
        border = JBUI.Borders.empty(8)

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        toolbar.add(refreshButton)

        val header = JPanel(BorderLayout())
        header.add(toolbar, BorderLayout.WEST)
        header.add(statusLabel, BorderLayout.EAST)

        priorityTree.isRootVisible = false
        priorityTree.showsRootHandles = true
        priorityTree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        priorityTree.cellRenderer = ResolutionTreeCellRenderer()
        buildPriorityTree()
        expandAll(priorityTree)
        priorityTree.addTreeSelectionListener {
            if (!syncingSelection && it.isAddedPath) {
                syncingSelection = true
                visibleTree.clearSelection()
                syncingSelection = false
                renderPrioritySelection()
            }
        }

        visibleTree.isRootVisible = false
        visibleTree.showsRootHandles = true
        visibleTree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        visibleTree.cellRenderer = ResolutionTreeCellRenderer()
        visibleTree.addTreeSelectionListener {
            if (!syncingSelection && it.isAddedPath) {
                syncingSelection = true
                priorityTree.clearSelection()
                syncingSelection = false
                renderVisibleSelection()
            }
        }

        detailsArea.isEditable = false
        detailsArea.lineWrap = true
        detailsArea.wrapStyleWord = true
        detailsArea.margin = JBUI.insets(8)
        detailsArea.text = "Select a priority rule or visible skill to inspect resolution details."

        outputArea.isEditable = false
        outputArea.lineWrap = true
        outputArea.wrapStyleWord = true
        outputArea.margin = JBUI.insets(8)
        outputArea.text = "OpenSkills list output will appear here."

        val leftSplit = OnePixelSplitter(true, 0.42f)
        leftSplit.firstComponent = panelWithTitle("Priority", JBScrollPane(priorityTree))
        leftSplit.secondComponent = panelWithTitle("Visible Skills", JBScrollPane(visibleTree))

        val mainSplit = OnePixelSplitter(false, 0.37f)
        mainSplit.firstComponent = leftSplit
        mainSplit.secondComponent = panelWithTitle("Details", JBScrollPane(detailsArea))

        val rootSplit = OnePixelSplitter(true, 0.8f)
        rootSplit.firstComponent = mainSplit
        rootSplit.secondComponent = panelWithTitle("Output", JBScrollPane(outputArea))

        add(header, BorderLayout.NORTH)
        add(rootSplit, BorderLayout.CENTER)

        refreshButton.addActionListener { loadResolution() }
        renderPrioritySelection()
        loadResolution()
    }

    fun refreshNow() {
        loadResolution()
    }

    private fun buildPriorityTree() {
        priorityRoot.removeAllChildren()

        val rules = listOf(
            PriorityRule("1", "Project / Universal", "./.agent/skills/", "Highest priority. Project-local universal skills win first."),
            PriorityRule("2", "Global / Universal", "~/.agent/skills/", "Fallback when the project does not provide the same universal skill."),
            PriorityRule("3", "Project / Claude", "./.claude/skills/", "Checked after universal locations, useful when Claude-specific skills are still installed."),
            PriorityRule("4", "Global / Claude", "~/.claude/skills/", "Lowest visible priority in the official OpenSkills order.")
        )

        rules.forEach { rule ->
            priorityRoot.add(DefaultMutableTreeNode(rule))
        }

        priorityModel.reload()
    }

    private fun loadResolution() {
        refreshButton.isEnabled = false
        statusLabel.text = "Refreshing..."
        outputArea.text = "Running openskills list..."
        clearVisibleTree()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Resolution", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running openskills list"
                val result = cli.list()
                ApplicationManager.getApplication().invokeLater {
                    refreshButton.isEnabled = true
                    statusLabel.text = "Ready"
                    val cleaned = result.combinedOutput().ifBlank { if (result.isSuccess()) "OpenSkills list returned no output." else "Unable to load visible skills." }
                    outputArea.text = cleaned
                    if (result.isSuccess()) {
                        rebuildVisibleTree(result.stdout.stripAnsi())
                    } else {
                        detailsArea.text = cleaned
                    }
                }
            }
        })
    }

    private fun clearVisibleTree() {
        visibleRoot.removeAllChildren()
        visibleModel.reload()
    }

    private fun rebuildVisibleTree(stdout: String) {
        clearVisibleTree()
        val skills = parseVisibleSkills(stdout)

        val projectUniversal = DefaultMutableTreeNode("Project / Universal")
        val globalUniversal = DefaultMutableTreeNode("Global / Universal")
        val projectClaude = DefaultMutableTreeNode("Project / Claude")
        val globalClaude = DefaultMutableTreeNode("Global / Claude")
        val other = DefaultMutableTreeNode("Other")

        skills.forEach { skill ->
            val target = when {
                skill.scope == "Project" && skill.mode == "Universal" -> projectUniversal
                skill.scope == "Global" && skill.mode == "Universal" -> globalUniversal
                skill.scope == "Project" && skill.mode == "Claude" -> projectClaude
                skill.scope == "Global" && skill.mode == "Claude" -> globalClaude
                else -> other
            }
            target.add(DefaultMutableTreeNode(skill))
        }

        listOf(projectUniversal, globalUniversal, projectClaude, globalClaude, other)
            .filter { it.childCount > 0 }
            .forEach(visibleRoot::add)

        visibleModel.reload()
        expandAll(visibleTree)

        if (visibleRoot.childCount > 0) {
            val firstGroup = visibleRoot.getChildAt(0) as DefaultMutableTreeNode
            if (firstGroup.childCount > 0) {
                val firstSkill = firstGroup.getChildAt(0) as DefaultMutableTreeNode
                visibleTree.selectionPath = javax.swing.tree.TreePath(firstSkill.path)
            }
        } else {
            syncingSelection = true
            priorityTree.clearSelection()
            syncingSelection = false
            detailsArea.text = "No visible skills were reported by openskills list."
        }
    }

    private fun parseVisibleSkills(stdout: String): List<VisibleSkill> {
        if (stdout.isBlank()) return emptyList()
        val cleaned = stdout.stripAnsi()
        val matches = Regex("(?m)^\\s*([A-Za-z0-9._-]+)\\s+\\((project|global)\\)").findAll(cleaned)
        return matches.map { match ->
            val id = match.groupValues[1]
            val scope = match.groupValues[2].replaceFirstChar { it.uppercase() }
            VisibleSkill(
                id = id,
                scope = scope,
                mode = inferModeFromText(cleaned, id),
                source = inferSourcePath(scope, inferModeFromText(cleaned, id))
            )
        }.toList()
    }

    private fun inferModeFromText(stdout: String, id: String): String {
        val pattern = Regex("(?is)${Regex.escape(id)}\\s+\\((project|global)\\).*?(\\.agent/skills|\\.claude/skills|\\\\.agent\\\\skills|\\\\.claude\\\\skills)")
        val match = pattern.find(stdout)
        val marker = match?.groupValues?.getOrNull(2) ?: return "Unknown"
        return if (marker.contains(".agent")) "Universal" else "Claude"
    }

    private fun inferSourcePath(scope: String, mode: String): String = when {
        scope == "Project" && mode == "Universal" -> "./.agent/skills/"
        scope == "Global" && mode == "Universal" -> "~/.agent/skills/"
        scope == "Project" && mode == "Claude" -> "./.claude/skills/"
        scope == "Global" && mode == "Claude" -> "~/.claude/skills/"
        else -> "Unknown source"
    }

    private fun renderPrioritySelection() {
        val node = priorityTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: run {
            if (priorityRoot.childCount > 0) {
                val firstRule = priorityRoot.getChildAt(0) as DefaultMutableTreeNode
                priorityTree.selectionPath = javax.swing.tree.TreePath(firstRule.path)
            }
            return
        }
        val rule = node.userObject as? PriorityRule ?: return
        detailsArea.text = buildString {
            appendLine(rule.label)
            appendLine()
            appendLine("Priority")
            appendLine(rule.rank)
            appendLine()
            appendLine("Path")
            appendLine(rule.path)
            appendLine()
            appendLine("Meaning")
            appendLine(rule.description)
            appendLine()
            appendLine("Project root")
            appendLine(project.basePath ?: "n/a")
        }.trim()
        detailsArea.caretPosition = 0
    }

    private fun renderVisibleSelection() {
        val node = visibleTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val skill = node.userObject as? VisibleSkill ?: return
        detailsArea.text = buildString {
            appendLine(skill.id)
            appendLine()
            appendLine("Scope")
            appendLine(skill.scope)
            appendLine()
            appendLine("Mode")
            appendLine(skill.mode)
            appendLine()
            appendLine("Resolved From")
            appendLine(skill.source)
            appendLine()
            appendLine("Priority Order")
            appendLine(priorityFor(skill.source))
            appendLine()
            appendLine("Explanation")
            appendLine(explainResolution(skill))
        }.trim()
        detailsArea.caretPosition = 0
    }

    private fun priorityFor(path: String): String = when (path) {
        "./.agent/skills/" -> "1"
        "~/.agent/skills/" -> "2"
        "./.claude/skills/" -> "3"
        "~/.claude/skills/" -> "4"
        else -> "Unknown"
    }

    private fun explainResolution(skill: VisibleSkill): String = when (skill.source) {
        "./.agent/skills/" -> "This skill is resolved from the current project and will override any matching global or Claude-specific copy."
        "~/.agent/skills/" -> "This skill is coming from the user's global universal skills because the project does not provide a higher-priority universal copy."
        "./.claude/skills/" -> "This skill is project-local, but it is behind both universal locations in the official order."
        "~/.claude/skills/" -> "This skill is visible from the global Claude directory and loses to any higher-priority project or universal copy."
        else -> "OpenSkills reported the skill, but the source path could not be inferred from the current list output."
    }

    private fun panelWithTitle(title: String, content: JBScrollPane): JPanel {
        val panel = JPanel(BorderLayout(0, 6))
        val titleLabel = JLabel(title)
        titleLabel.border = JBUI.Borders.emptyLeft(2)
        content.border = null
        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(content, BorderLayout.CENTER)
        return panel
    }

    private fun expandAll(tree: JTree) {
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row)
            row++
        }
    }

    private fun String.stripAnsi(): String = replace(Regex("\\u001B\\[[;\\d]*[ -/]*[@-~]"), "")

    private data class PriorityRule(
        val rank: String,
        val label: String,
        val path: String,
        val description: String
    ) {
        override fun toString(): String = "$rank. $label"
    }

    private data class VisibleSkill(
        val id: String,
        val scope: String,
        val mode: String,
        val source: String
    ) {
        override fun toString(): String = buildString {
            append(id)
            append("   ")
            append(scope.lowercase())
            if (mode != "Unknown") {
                append(" · ")
                append(mode.lowercase())
            }
        }
    }

    private inner class ResolutionTreeCellRenderer : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
            val node = value as? DefaultMutableTreeNode ?: return this
            when (val userObject = node.userObject) {
                is PriorityRule -> text = userObject.toString()
                is VisibleSkill -> text = userObject.toString()
            }
            return this
        }
    }
}
