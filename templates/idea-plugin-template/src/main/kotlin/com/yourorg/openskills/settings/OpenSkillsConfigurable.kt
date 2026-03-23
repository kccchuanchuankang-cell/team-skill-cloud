package com.yourorg.openskills.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class OpenSkillsConfigurable : Configurable {
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "OpenSkills"

    override fun createComponent(): JComponent {
        if (panel == null) {
            panel = JPanel()
            panel!!.add(JLabel("TODO: OpenSkills settings UI"))
        }
        return panel!!
    }
}
