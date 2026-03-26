package com.yourorg.openskills.ui.project

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ComponentUtil
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.yourorg.openskills.cli.CliCommandResult
import com.yourorg.openskills.cli.OpenSkillsCliAdapter
import com.yourorg.openskills.manifest.ProjectManifestService
import com.yourorg.openskills.manifest.ResolvedSkill
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.JTextField
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class ProjectPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val manifests = ProjectManifestService(project)
    private val cli = OpenSkillsCliAdapter(project)

    private val refreshAction = ToolbarAction("Refresh", AllIcons.Actions.Refresh) { loadInstalledSkills() }
    private val syncAction = ToolbarAction("Sync", AllIcons.Actions.BuildLoadChanges) { runSync() }
    private val updateAllAction = ToolbarAction("Update All", AllIcons.Actions.CheckForUpdate) { updateAllSkills() }
    private val openProjectConfigAction = ToolbarAction("Project Config", AllIcons.General.Settings) { openManifest() }
    private val readAction = SelectionAwareToolbarAction("Read", AllIcons.Actions.ShowReadAccess) { readSelectedSkill() }
    private val updateAction = SelectionAwareToolbarAction("Update", AllIcons.Actions.MenuSaveall) { updateSelectedSkill() }
    private val removeAction = SelectionAwareToolbarAction("Remove", AllIcons.General.Remove) { removeSelectedSkill() }

    private val primaryToolbar = ActionManager.getInstance()
        .createActionToolbar(
            "OpenSkills.Installed.Primary",
            DefaultActionGroup(refreshAction, syncAction, updateAllAction, openProjectConfigAction),
            true
        )
    private val detailToolbar = ActionManager.getInstance()
        .createActionToolbar(
            "OpenSkills.Installed.Detail",
            DefaultActionGroup(readAction, updateAction, removeAction),
            true
        )

    private val rootNode = DefaultMutableTreeNode("Installed Skills")
    private val treeModel = DefaultTreeModel(rootNode)
    private val skillTree = JTree(treeModel)

    private val nameField = createReadOnlyField()
    private val versionField = createReadOnlyField()
    private val scopeField = createReadOnlyField()
    private val modeField = createReadOnlyField()
    private val sourceField = createReadOnlyField()
    private val pathField = createReadOnlyField()
    private val packageUrlField = createReadOnlyField()
    private val publishedAtField = createReadOnlyField()
    private val previewArea = JBTextArea()
    private val outputArea = JBTextArea()
    private val statusLabel = JLabel("Ready")

    init {
        border = JBUI.Borders.empty(8)
        primaryToolbar.setTargetComponent(this)
        detailToolbar.setTargetComponent(this)

        val toolbar = JPanel(BorderLayout(0, 6))
        toolbar.add(primaryToolbar.component, BorderLayout.NORTH)

        val header = JPanel(BorderLayout())
        header.add(toolbar, BorderLayout.WEST)
        header.add(statusLabel, BorderLayout.EAST)

        skillTree.isRootVisible = false
        skillTree.showsRootHandles = true
        skillTree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        skillTree.cellRenderer = SkillTreeCellRenderer()
        skillTree.addTreeSelectionListener {
            if (!it.isAddedPath) return@addTreeSelectionListener
            renderSelectedNode()
            loadSkillPreviewForSelection()
        }

        previewArea.isEditable = false
        previewArea.lineWrap = true
        previewArea.wrapStyleWord = true
        previewArea.margin = JBUI.insets(8)
        previewArea.text = "Select a skill to load a preview."

        outputArea.isEditable = false
        outputArea.lineWrap = true
        outputArea.wrapStyleWord = true
        outputArea.margin = JBUI.insets(8)
        outputArea.text = "Command logs and errors will appear here."

        val treePane = panelWithTitle("Installed", JBScrollPane(skillTree))
        val detailPane = buildDetailPane()
        val mainSplit = OnePixelSplitter(false, 0.32f)
        mainSplit.firstComponent = treePane
        mainSplit.secondComponent = detailPane

        val outputPane = panelWithTitle("Output", JBScrollPane(outputArea))
        val rootSplit = OnePixelSplitter(true, 0.84f)
        rootSplit.firstComponent = mainSplit
        rootSplit.secondComponent = outputPane

        add(header, BorderLayout.NORTH)
        add(rootSplit, BorderLayout.CENTER)

        resetDetailPanel("Select a skill to view details.")
        updateSelectionActions()
        loadInstalledSkills()
    }

    fun refreshNow() {
        loadInstalledSkills()
    }

    private fun createReadOnlyField(): JTextField = JTextField().apply {
        isEditable = false
        border = JBUI.Borders.empty(4, 6)
        columns = 1
    }

    private fun buildDetailPane(): JPanel {
        val header = JPanel(BorderLayout())
        val titleLabel = JLabel("Details")
        titleLabel.border = JBUI.Borders.emptyLeft(2)
        header.add(titleLabel, BorderLayout.WEST)
        header.add(detailToolbar.component, BorderLayout.EAST)

        val panel = JPanel(BorderLayout(0, 6))
        panel.add(header, BorderLayout.NORTH)
        panel.add(buildDetailScrollPane(), BorderLayout.CENTER)
        return panel
    }

    private fun buildDetailScrollPane(): JBScrollPane {
        val detailContent = JPanel(BorderLayout(0, 12))
        detailContent.border = JBUI.Borders.empty(4)

        val propertiesPanel = JPanel(GridBagLayout())
        val fields = listOf(
            "Name" to nameField,
            "Version" to versionField,
            "Scope" to scopeField,
            "Mode" to modeField,
            "Source" to sourceField,
            "Resolved Path" to pathField,
            "Package URL" to packageUrlField,
            "Published At" to publishedAtField
        )

        fields.forEachIndexed { index, (label, field) ->
            val labelConstraints = GridBagConstraints().apply {
                gridx = 0
                gridy = index
                anchor = GridBagConstraints.NORTHWEST
                insets = JBUI.insets(0, 0, 8, 12)
            }
            propertiesPanel.add(JLabel(label), labelConstraints)

            val fieldConstraints = GridBagConstraints().apply {
                gridx = 1
                gridy = index
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.NORTHWEST
                insets = JBUI.insets(0, 0, 8, 0)
            }
            propertiesPanel.add(field, fieldConstraints)
        }

        val previewScroll = JBScrollPane(previewArea).apply {
            border = BorderFactory.createEmptyBorder()
        }

        val previewPanel = JPanel(BorderLayout(0, 6))
        previewPanel.add(JLabel("Preview"), BorderLayout.NORTH)
        previewPanel.add(previewScroll, BorderLayout.CENTER)

        detailContent.add(propertiesPanel, BorderLayout.NORTH)
        detailContent.add(previewPanel, BorderLayout.CENTER)

        return JBScrollPane(detailContent).apply {
            border = BorderFactory.createEmptyBorder()
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }
    }

    private fun panelWithTitle(title: String, content: JBScrollPane): JPanel {
        val panel = JPanel(BorderLayout(0, 6))
        val titleLabel = JLabel(title)
        titleLabel.border = JBUI.Borders.emptyLeft(2)
        content.border = BorderFactory.createEmptyBorder()
        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(content, BorderLayout.CENTER)
        return panel
    }

    private fun loadInstalledSkills() {
        setBusyState(true)
        statusLabel.text = "Refreshing..."
        outputArea.text = "Running openskills list..."

        val resolvedSkills = manifests.readLockOrNull()?.resolved.orEmpty()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Refresh", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running openskills list"
                val listResult = cli.list()
                ApplicationManager.getApplication().invokeLater {
                    applyLoadedSkills(resolvedSkills, listResult)
                    setBusyState(false)
                    statusLabel.text = "Ready"
                }
            }
        })
    }

    private fun applyLoadedSkills(resolvedSkills: List<ResolvedSkill>, listResult: CliCommandResult) {
        val cleanedStdout = listResult.stdout.stripAnsi()
        val visibleSkills = mergeVisibleSkills(resolvedSkills, parseCliVisibleSkills(cleanedStdout))
        rebuildTree(visibleSkills)

        outputArea.text = if (listResult.isSuccess()) {
            cleanedStdout.ifBlank { "OpenSkills list returned no output." }
        } else {
            buildString {
                appendLine("OpenSkills CLI is currently unavailable.")
                appendLine()
                appendLine(listResult.combinedOutput().ifBlank { "Run `npx openskills list` from a terminal to verify your local setup." })
            }.trim()
        }

        if (visibleSkills.isEmpty()) {
            resetDetailPanel("No visible skills found yet. Use Install later, or verify what `openskills list` reports below.")
        }

        updateSelectionActions()
    }

    private fun mergeVisibleSkills(resolvedSkills: List<ResolvedSkill>, parsedSkills: List<DisplayedSkill>): List<DisplayedSkill> {
        val byId = linkedMapOf<String, DisplayedSkill>()
        resolvedSkills.forEach { skill ->
            val bucket = classifyBucket(skill.installedPath)
            byId[skill.id] = DisplayedSkill(
                id = skill.id,
                version = skill.version,
                scope = bucket.scopeLabel,
                mode = bucket.modeLabel,
                installedPath = skill.installedPath,
                packageUrl = skill.packageUrl,
                checksumSha256 = skill.checksumSha256,
                sourceIndexUrl = skill.sourceIndexUrl,
                publishedAt = skill.publishedAt,
                fromLock = true
            )
        }
        parsedSkills.forEach { skill ->
            byId.putIfAbsent(skill.id, skill)
        }
        return byId.values.toList().sortedWith(compareBy<DisplayedSkill> { it.scope }.thenBy { it.mode }.thenBy { it.id })
    }

    private fun parseCliVisibleSkills(stdout: String): List<DisplayedSkill> {
        if (stdout.isBlank()) return emptyList()

        val matches = Regex("(?m)^\\s*([A-Za-z0-9._-]+)\\s+\\((project|global)\\)").findAll(stdout)
        return matches.map { match ->
            val id = match.groupValues[1]
            val listedScope = match.groupValues[2].replaceFirstChar { it.uppercase() }
            val resolvedLocation = resolveSkillLocation(id)
            val resolvedBucket = resolvedLocation?.let { classifyBucket(it.toString()) }
            DisplayedSkill(
                id = id,
                version = "listed",
                scope = resolvedBucket?.scopeLabel?.takeUnless { it == "Unknown" } ?: listedScope,
                mode = resolvedBucket?.modeLabel?.takeUnless { it == "Unknown" } ?: inferModeFromText(stdout, id),
                installedPath = resolvedLocation?.toString() ?: "Listed by openskills",
                packageUrl = null,
                checksumSha256 = null,
                sourceIndexUrl = null,
                publishedAt = null,
                fromLock = false
            )
        }.toList()
    }

    private fun resolveSkillLocation(id: String): Path? {
        val projectBase = project.basePath?.let { Path.of(it) }
        val userHome = System.getProperty("user.home")?.let { Path.of(it) }

        val candidates = listOfNotNull(
            projectBase?.resolve(".agent")?.resolve("skills")?.resolve(id),
            userHome?.resolve(".agent")?.resolve("skills")?.resolve(id),
            projectBase?.resolve(".claude")?.resolve("skills")?.resolve(id),
            userHome?.resolve(".claude")?.resolve("skills")?.resolve(id)
        )

        return candidates.firstOrNull { Files.exists(it) }
    }

    private fun inferModeFromText(stdout: String, id: String): String {
        val skillPathPattern = Regex("(?is)${Regex.escape(id)}\\s+\\((project|global)\\).*?(\\.agent/skills|\\.claude/skills|\\\\.agent\\\\skills|\\\\.claude\\\\skills)")
        val match = skillPathPattern.find(stdout)
        val marker = match?.groupValues?.getOrNull(2) ?: return "Unknown"
        return if (marker.contains(".agent")) "Universal" else "Claude"
    }

    private fun rebuildTree(skills: List<DisplayedSkill>) {
        rootNode.removeAllChildren()

        val projectUniversal = DefaultMutableTreeNode("Project / Universal")
        val globalUniversal = DefaultMutableTreeNode("Global / Universal")
        val projectClaude = DefaultMutableTreeNode("Project / Claude")
        val globalClaude = DefaultMutableTreeNode("Global / Claude")
        val unknown = DefaultMutableTreeNode("Other")

        skills.forEach { skill ->
            val target = when {
                skill.scope == "Project" && skill.mode == "Universal" -> projectUniversal
                skill.scope == "Global" && skill.mode == "Universal" -> globalUniversal
                skill.scope == "Project" && skill.mode == "Claude" -> projectClaude
                skill.scope == "Global" && skill.mode == "Claude" -> globalClaude
                else -> unknown
            }
            target.add(DefaultMutableTreeNode(skill))
        }

        listOf(projectUniversal, globalUniversal, projectClaude, globalClaude, unknown)
            .filter { it.childCount > 0 }
            .forEach(rootNode::add)

        treeModel.reload()
        expandAll()

        if (rootNode.childCount > 0) {
            val firstGroup = rootNode.getChildAt(0) as DefaultMutableTreeNode
            if (firstGroup.childCount > 0) {
                val firstSkill = firstGroup.getChildAt(0) as DefaultMutableTreeNode
                skillTree.selectionPath = javax.swing.tree.TreePath(firstSkill.path)
            }
        }
    }

    private fun expandAll() {
        var row = 0
        while (row < skillTree.rowCount) {
            skillTree.expandRow(row)
            row++
        }
    }

    private fun renderSelectedNode() {
        val node = skillTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: run {
            resetDetailPanel("Select a skill to view details.")
            updateSelectionActions()
            return
        }
        val skill = node.userObject as? DisplayedSkill ?: run {
            resetDetailPanel(node.userObject.toString())
            updateSelectionActions()
            return
        }

        renderSkillDetails(skill, "Loading preview...")
        updateSelectionActions()
    }

    private fun resetDetailPanel(message: String) {
        nameField.text = ""
        versionField.text = ""
        scopeField.text = ""
        modeField.text = ""
        sourceField.text = ""
        pathField.text = ""
        packageUrlField.text = ""
        publishedAtField.text = ""
        previewArea.text = message
    }

    private fun renderSkillDetails(skill: DisplayedSkill, preview: String?) {
        nameField.text = skill.id
        versionField.text = skill.version
        scopeField.text = skill.scope
        modeField.text = skill.mode
        sourceField.text = if (skill.fromLock) "openskills.lock.json" else "openskills list"
        pathField.text = skill.installedPath
        packageUrlField.text = skill.packageUrl ?: "n/a"
        publishedAtField.text = skill.publishedAt ?: "n/a"
        previewArea.text = preview ?: "No preview loaded."
        previewArea.caretPosition = 0
    }

    private fun loadSkillPreviewForSelection() {
        val skill = selectedSkill() ?: return
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Preview", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running openskills read ${skill.id}"
                val result = cli.read(skill.id)
                val preview = if (result.isSuccess()) {
                    val cleaned = result.stdout.stripAnsi().trim()
                    cleaned.lines().take(12).joinToString(System.lineSeparator()).ifBlank { "No preview returned by openskills read ${skill.id}." }
                } else {
                    "Preview unavailable. Use Read to inspect the full content."
                }
                ApplicationManager.getApplication().invokeLater {
                    val current = selectedSkill()
                    if (current?.id == skill.id) {
                        renderSkillDetails(skill, preview)
                    }
                }
            }
        })
    }

    private fun selectedSkill(): DisplayedSkill? {
        val node = skillTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return null
        return node.userObject as? DisplayedSkill
    }

    private fun classifyBucket(path: String): SkillBucket = when {
        path.contains("/.agent/skills/") || path.contains("\\.agent\\skills\\") -> if (project.basePath?.let { path.contains(it, ignoreCase = true) } == true) SkillBucket.PROJECT_UNIVERSAL else SkillBucket.GLOBAL_UNIVERSAL
        path.contains("/.claude/skills/") || path.contains("\\.claude\\skills\\") -> if (project.basePath?.let { path.contains(it, ignoreCase = true) } == true) SkillBucket.PROJECT_CLAUDE else SkillBucket.GLOBAL_CLAUDE
        else -> SkillBucket.OTHER
    }

    private fun updateSelectionActions() {
        refreshAction.isBusy = false
        syncAction.isBusy = false
        updateAllAction.isBusy = false
        openProjectConfigAction.isBusy = false
        val hasSelection = selectedSkill() != null
        readAction.hasSelection = hasSelection
        updateAction.hasSelection = hasSelection
        removeAction.hasSelection = hasSelection
        refreshToolbars()
    }

    private fun setBusyState(isBusy: Boolean) {
        refreshAction.isBusy = isBusy
        syncAction.isBusy = isBusy
        updateAllAction.isBusy = isBusy
        openProjectConfigAction.isBusy = isBusy
        readAction.isBusy = isBusy
        updateAction.isBusy = isBusy
        removeAction.isBusy = isBusy
        if (!isBusy) updateSelectionActions() else refreshToolbars()
    }

    private fun readSelectedSkill() {
        val skill = selectedSkill() ?: return
        setBusyState(true)
        statusLabel.text = "Reading ${skill.id}..."
        outputArea.text = "Running openskills read ${skill.id}..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Read", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running openskills read ${skill.id}"
                val result = cli.read(skill.id)
                ApplicationManager.getApplication().invokeLater {
                    setBusyState(false)
                    statusLabel.text = "Ready"
                    if (result.isSuccess()) {
                        val cleaned = result.stdout.stripAnsi().ifBlank { "No content returned by openskills read ${skill.id}." }
                        renderSkillDetails(skill, cleaned)
                        outputArea.text = "Read succeeded for ${skill.id}."
                    } else {
                        outputArea.text = result.combinedOutput().ifBlank { "Read failed." }
                        Messages.showErrorDialog(project, outputArea.text, "OpenSkills Read Error")
                    }
                }
            }
        })
    }

    private fun updateSelectedSkill() {
        val skill = selectedSkill() ?: return
        setBusyState(true)
        statusLabel.text = "Updating ${skill.id}..."
        outputArea.text = "Running openskills update ${skill.id}..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Update", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running openskills update ${skill.id}"
                val result = cli.update(skill.id)
                ApplicationManager.getApplication().invokeLater {
                    setBusyState(false)
                    statusLabel.text = "Ready"
                    outputArea.text = result.combinedOutput().ifBlank { if (result.isSuccess()) "Update completed." else "Update failed." }
                    if (result.isSuccess()) {
                        loadInstalledSkills()
                    } else {
                        Messages.showErrorDialog(project, outputArea.text, "OpenSkills Update Error")
                    }
                }
            }
        })
    }

    private fun updateAllSkills() {
        setBusyState(true)
        statusLabel.text = "Updating all skills..."
        outputArea.text = "Running openskills update..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Update All", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running openskills update"
                val result = cli.update()
                ApplicationManager.getApplication().invokeLater {
                    setBusyState(false)
                    statusLabel.text = "Ready"
                    outputArea.text = result.combinedOutput().ifBlank { if (result.isSuccess()) "Update all completed." else "Update all failed." }
                    if (result.isSuccess()) {
                        loadInstalledSkills()
                    } else {
                        Messages.showErrorDialog(project, outputArea.text, "OpenSkills Update Error")
                    }
                }
            }
        })
    }

    private fun removeSelectedSkill() {
        val skill = selectedSkill() ?: return
        val confirmed = Messages.showYesNoDialog(
            project,
            "Remove ${skill.id} from the current OpenSkills installation?",
            "OpenSkills Remove",
            "Remove",
            "Cancel",
            null
        )
        if (confirmed != Messages.YES) return

        setBusyState(true)
        statusLabel.text = "Removing ${skill.id}..."
        outputArea.text = "Running openskills remove ${skill.id}..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Remove", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running openskills remove ${skill.id}"
                val result = cli.remove(skill.id)
                ApplicationManager.getApplication().invokeLater {
                    setBusyState(false)
                    statusLabel.text = "Ready"
                    outputArea.text = result.combinedOutput().ifBlank { if (result.isSuccess()) "Remove completed." else "Remove failed." }
                    if (result.isSuccess()) {
                        loadInstalledSkills()
                    } else {
                        Messages.showErrorDialog(project, outputArea.text, "OpenSkills Remove Error")
                    }
                }
            }
        })
    }

    private fun runSync() {
        setBusyState(true)
        statusLabel.text = "Syncing..."
        outputArea.text = "Running openskills sync..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "OpenSkills Sync", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running openskills sync"
                val result = cli.sync()
                ApplicationManager.getApplication().invokeLater {
                    setBusyState(false)
                    statusLabel.text = "Ready"
                    outputArea.text = result.combinedOutput().ifBlank { if (result.isSuccess()) "Sync completed." else "Sync failed." }
                    if (result.isSuccess()) {
                        loadInstalledSkills()
                    } else {
                        Messages.showErrorDialog(project, outputArea.text, "OpenSkills Sync Error")
                    }
                }
            }
        })
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

    private fun String.stripAnsi(): String = replace(Regex("\\u001B\\[[;\\d]*[ -/]*[@-~]"), "")

    private data class DisplayedSkill(
        val id: String,
        val version: String,
        val scope: String,
        val mode: String,
        val installedPath: String,
        val packageUrl: String?,
        val checksumSha256: String?,
        val sourceIndexUrl: String?,
        val publishedAt: String?,
        val fromLock: Boolean
    ) {
        override fun toString(): String = if (version == "listed") id else "$id ($version)"
    }

    private inner class SkillTreeCellRenderer : DefaultTreeCellRenderer() {
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
            val skill = node.userObject as? DisplayedSkill
            if (skill != null) {
                text = buildString {
                    append(skill.id)
                    append("   ")
                    append(skill.scope.lowercase())
                    if (skill.mode != "Unknown") {
                        append(" · ")
                        append(skill.mode.lowercase())
                    }
                    if (skill.version != "listed") {
                        append("   ")
                        append(skill.version)
                    }
                }
            }
            return this
        }
    }

    private enum class SkillBucket(val scopeLabel: String, val modeLabel: String) {
        PROJECT_UNIVERSAL("Project", "Universal"),
        GLOBAL_UNIVERSAL("Global", "Universal"),
        PROJECT_CLAUDE("Project", "Claude"),
        GLOBAL_CLAUDE("Global", "Claude"),
        OTHER("Unknown", "Unknown")
    }

    private fun refreshToolbars() {
        primaryToolbar.updateActionsImmediately()
        detailToolbar.updateActionsImmediately()
    }

    private open inner class ToolbarAction(
        text: String,
        icon: javax.swing.Icon,
        private val handler: () -> Unit
    ) : DumbAwareAction(text, null, icon) {
        var isBusy: Boolean = false

        override fun actionPerformed(e: AnActionEvent) {
            if (!isBusy) handler()
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = !isBusy
            e.presentation.text = templateText
        }
    }

    private inner class SelectionAwareToolbarAction(
        text: String,
        icon: javax.swing.Icon,
        handler: () -> Unit
    ) : ToolbarAction(text, icon, handler) {
        var hasSelection: Boolean = false

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = hasSelection && !isBusy
            e.presentation.text = templateText
        }
    }
}
